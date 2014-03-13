import actors.ProcessCPOCsvEntry
import akka.actor.Props
import com.google.common.base.{Splitter, CharMatcher}
import com.google.common.util.concurrent.{FutureCallback, JdkFutureAdapters, Futures}
import models.{PostcodeUnit, Party}
import models.csv.CodePointOpenCsvEntry
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.model.dataformat.BindyType
import org.apache.camel.{FailedToStartRouteException, Exchange, Processor}
import org.joda.time.DateTime
import play.api._
import play.api.mvc.WithFilters
import play.filters.gzip.GzipFilter
import play.libs.Akka
import play.modules.reactivemongo.ReactiveMongoPlugin
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.api.indexes.IndexType.{Geo2D, Ascending}
import reactivemongo.api.indexes.Index
import reactivemongo.bson.BSONDocument
import reactivemongo.core.commands.LastError
import scala.annotation.tailrec
import scala.collection.JavaConversions._
import scala.concurrent.{Future, ExecutionContext}
import scala.Some
import uk.co.coen.capsulecrm.client._
import play.api.Play.current
import ExecutionContext.Implicits.global
import scala.util.control.Exception._
import play.modules.reactivemongo.json.BSONFormats._
import scala.concurrent.duration._

object Global extends WithFilters(new GzipFilter()) with GlobalSettings {
  val camelContext = new DefaultCamelContext()

  override def onStart(app: Application) {
    val processActor = Akka.system.actorOf(Props[ProcessCPOCsvEntry], name = "processCPOCsvEntry")

    // import postcodes
    camelContext.addRoutes(new RouteBuilder {
      override def configure() {
        from(app.configuration.getString("cpo.from").get).unmarshal.bindy(BindyType.Csv, "models.csv").split(body).process(new Processor {
          override def process(exchange: Exchange) {
            val csvEntryMap = mapAsScalaMap[String, CodePointOpenCsvEntry](exchange.getIn.getBody.asInstanceOf[java.util.Map[String, CodePointOpenCsvEntry]])

            for (entry <- csvEntryMap.values) {
              processActor ! entry
            }
          }
        })
      }
    })

    try {
      camelContext.start()
    } catch {
      case e: FailedToStartRouteException => Logger.warn(e.getMessage)
    }

    val pcuCollection: BSONCollection = ReactiveMongoPlugin.db.collection[BSONCollection]("pcu")
    val partyCollection: BSONCollection = ReactiveMongoPlugin.db.collection[BSONCollection]("parties")

    pcuCollection.indexesManager.ensure(Index(List("pc" -> Ascending)))
    partyCollection.indexesManager.ensure(Index(List("loc" -> Geo2D)))

    if (Play.isProd) {
      app.configuration.getString("capsulecrm.url") match {
        case Some(url) =>
          importParties(pcuCollection, partyCollection, CParty.listAll().get())
          Akka.system().scheduler.schedule(5 minutes, 5 minutes) {
            importParties(pcuCollection, partyCollection, CParty.listModifiedSince(new DateTime().minusHours(1)).get())
          }
        case _ =>
      }
    }
  }

  def importParties(pcuCollection: BSONCollection, partyCollection: BSONCollection, parties: CParties) {
    val skipImport = Play.current.configuration.getStringList("groups.skipImport").get

    parties.map {
      party =>
        if (party.firstEmail() != null && party.firstAddress() != null && party.firstAddress().zip != null) {
          Futures.addCallback(JdkFutureAdapters.listenInPoolThread(party.listTags()), new FutureCallback[CTags] {
            override def onSuccess(tags: CTags) = {
              if (!tags.tags.map(_.name).exists(skipImport.toSet))
                importParty(pcuCollection, partyCollection, party, tags) // todo log failure
            }

            override def onFailure(failure: Throwable) = Logger.error(failure.getMessage, failure)
          })
        }
    }
  }

  def importParty(pcuCollection: BSONCollection, partyCollection: BSONCollection, party: CParty, tags: CTags): Future[Either[String, Future[LastError]]] = {
    val groupsToIgnore = Play.current.configuration.getStringList("groups.ignore").get
    val groupsToCollapseIfContains = Play.current.configuration.getStringList("groups.collapseIfContains").get

    val groups = (tags.tags.map(_.name).diff(groupsToIgnore) ++
      (if (party.isInstanceOf[COrganisation]) Nil else Splitter.on(CharMatcher.anyOf(",&")).trimResults().omitEmptyStrings().split(party.asInstanceOf[CPerson].jobTitle).toList))
      .map(_.capitalize)
      .map(group => allCatch.opt(groupsToCollapseIfContains.filter(group.toLowerCase.contains(_)).maxBy(_.length)).getOrElse(group))
      .filter(_.length > 1)
      .distinct
      .toList

    pcuCollection.find(BSONDocument("pc" -> CharMatcher.WHITESPACE.removeFrom(party.firstAddress().zip).toUpperCase)).one[PostcodeUnit].map {
      case Some(postcodeUnit) =>
        Right(partyCollection.find(BSONDocument("cid" -> party.id.toString)).one[Party].flatMap {
          case Some(existingParty) =>
            partyCollection.remove(existingParty).flatMap { _ =>
                partyCollection.insert(existingParty.copy(
                  party.id.toString,
                  party.getName,
                  party.firstEmail().emailAddress,
                  if (party.firstWebsite(WebService.URL) != null) Some(party.firstWebsite(WebService.URL).webAddress) else None,
                  party.firstAddress().zip.toUpperCase,
                  party.isInstanceOf[COrganisation],
                  postcodeUnit.location,
                  groups
                ))
            }

          case None =>
            partyCollection.insert(Party(
              party.id.toString,
              party.getName,
              party.firstEmail().emailAddress,
              if (party.firstWebsite(WebService.URL) != null) Some(party.firstWebsite(WebService.URL).webAddress) else None,
              party.firstAddress().zip.toUpperCase,
              party.isInstanceOf[COrganisation],
              postcodeUnit.location,
              groups
            ))
        })

      case None => {
        Left(s"Unable to find location for party ${party.getName} with postcode ${party.firstAddress().zip}")
      }
    }
  }

  override def onStop(app: Application) = {
    camelContext.stop()
  }
}