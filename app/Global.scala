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
import scala.collection.JavaConversions._
import scala.concurrent._
import scala.util.control.NonFatal
import scala.util.{Success, Failure, Try}
import uk.co.coen.capsulecrm.client._
import play.api.Play.current
import ExecutionContext.Implicits.global
import scala.util.control.Exception._
import play.modules.reactivemongo.json.BSONFormats._
import scala.concurrent.duration._

object Global extends WithFilters(new GzipFilter()) with GlobalSettings {
  val camelContext = new DefaultCamelContext()

  implicit def javaFutureToScalaFuture[T](javaFuture: java.util.concurrent.Future[T]): Future[T] = {
    val p = promise[T]()

    Futures.addCallback(JdkFutureAdapters.listenInPoolThread(javaFuture), new FutureCallback[T]() {
      override def onSuccess(result: T) = p.success(result)
      override def onFailure(t: Throwable) = p.failure(t)
    })

    p.future
  }

  override def onStart(app: Application) {
    val processActor = Akka.system.actorOf(Props[ProcessCPOCsvEntry], name = "processCPOCsvEntry")

    // import postcodes
    camelContext.addRoutes(new RouteBuilder {
      override def configure() {
        from(app.configuration.getString("cpo.from").get).unmarshal.bindy(BindyType.Csv, "models.csv").split(body).process(new Processor {
          override def process(exchange: Exchange) {
            try {
              val csvEntryMap = mapAsScalaMap[String, CodePointOpenCsvEntry](exchange.getIn.getBody.asInstanceOf[java.util.Map[String, CodePointOpenCsvEntry]])

              for (entry <- csvEntryMap.values) {
                processActor ! entry
              }
            }
            catch {
              case NonFatal(e) => Logger.error(s"Error processing CSV: ${e.getMessage}", e)
            }
          }
        })
      }
    })

    val pcuCollection: BSONCollection = ReactiveMongoPlugin.db.collection[BSONCollection]("pcu")
    val partyCollection: BSONCollection = ReactiveMongoPlugin.db.collection[BSONCollection]("parties")

    pcuCollection.indexesManager.ensure(Index(Seq("outward" -> Ascending, "inward" -> Ascending), unique = true))

    partyCollection.indexesManager.ensure(Index(Seq("pid" -> Ascending)))
    partyCollection.indexesManager.ensure(Index(Seq("loc" -> Geo2D)))
    partyCollection.indexesManager.ensure(Index(Seq("grps" -> Ascending)))

    try {
      camelContext.start()
    } catch {
      case e: FailedToStartRouteException => Logger.warn(e.getMessage)
    }

    app.configuration.getString("capsulecrm.url") match {
      case Some(url) if !Play.isTest =>
        importParties(pcuCollection, partyCollection, CParty.listAll())

        Akka.system().scheduler.schedule(10 minutes, 5 minutes) {
          importParties(pcuCollection, partyCollection, CParty.listModifiedSince(new DateTime().minusHours(1)))
        }
      case _ =>
    }
  }

  def importParties(pcuCollection: BSONCollection, partyCollection: BSONCollection, partiesFuture: Future[CParties]) {
    val skipImport = Play.current.configuration.getStringList("groups.skipImport").get

    partiesFuture.onComplete {
      case Success(parties) =>
        for (party <- parties if party.firstEmail() != null && party.firstAddress() != null && party.firstAddress().zip != null) {
          party.listTags().onComplete {
            case Success(tags) if tags.size > 0 && !tags.tags.map(_.name).exists(skipImport.toSet) =>
              importParty(pcuCollection, partyCollection, party, tags) onComplete {
                case Success(result) => result match {
                  case Right(insertResult) => insertResult.onComplete {
                    case Failure(e) => Logger.error(e.getMessage, e)
                    case Success(_) =>
                  }
                  case Left(message) => Logger.warn(message)
                }
                case Failure(t) => Logger.error(t.getMessage, t)
              }
            case Success(tags) => Logger.debug(s"Skipping import of $party")
            case Failure(t) => Logger.error(t.getMessage, t)
          }
        }
      case Failure(t) => Logger.error(t.getMessage, t)
      case _ =>
    }
  }

  def importParty(pcuCollection: BSONCollection, partyCollection: BSONCollection, party: CParty, tags: CTags): Future[Either[String, Future[LastError]]] = {
    val groupsToIgnore = Play.current.configuration.getStringList("groups.ignore").get
    val groupsToCollapseIfContains = Play.current.configuration.getStringList("groups.collapseIfContains").get

    val groups = (tags.tags.map(_.name) ++
      (if (party.isInstanceOf[COrganisation]) Nil else Try(Splitter.on(CharMatcher.anyOf(",&")).trimResults().omitEmptyStrings().split(party.asInstanceOf[CPerson].jobTitle).toList).getOrElse(Nil)))
      .map(group => allCatch.opt(groupsToCollapseIfContains.filter(group.toLowerCase.contains(_)).maxBy(_.length)).getOrElse(group).trim)
      .filter(_.length > 1)
      .distinct
      .diff(groupsToIgnore)
      .map(_.capitalize)
      .padTo(1, "No groups")
      .toList

    party.firstAddress().zip.toUpperCase.split(' ') match {
      case Array(outward, inward) => {
        pcuCollection.find(BSONDocument("outward" -> outward.trim, "inward" -> inward.trim)).one[PostcodeUnit].map {
          case Some(postcodeUnit) =>
            Right(partyCollection.find(BSONDocument("pid" -> party.id.toString)).one[Party].flatMap {
              case Some(existingParty) =>
                partyCollection.remove(existingParty).flatMap { _ =>
                  partyCollection.insert(existingParty.copy(
                    party.id.toString,
                    party.getName,
                    party.firstEmail().emailAddress,
                    if (party.firstWebsite(WebService.URL) != null) Some(party.firstWebsite(WebService.URL).webAddress) else None,
                    outward,
                    inward,
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
                  outward,
                  inward,
                  party.isInstanceOf[COrganisation],
                  postcodeUnit.location,
                  groups
                ))
            })

          case None => Left(s"Unable to find location for party ${party.getName} with postcode $outward / $inward (${party.firstAddress().zip})")
        }
      }
      case _ => {
        Logger.error(s"No space in ZIP code for $party")
        Future.failed(new Exception)
      }
    }
  }

  override def onStop(app: Application) = {
    camelContext.stop()
  }
}