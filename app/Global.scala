import actors.ProcessCPOCsvEntry
import akka.actor.Props
import com.google.common.base.CharMatcher
import models.Contact
import models.csv.CodePointOpenCsvEntry
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.model.dataformat.BindyType
import org.apache.camel.{Exchange, Processor}
import play.api._
import play.libs.Akka
import play.modules.reactivemongo.ReactiveMongoPlugin
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.api.indexes.IndexType.{Geo2D, Ascending}
import reactivemongo.api.indexes.Index
import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext
import uk.co.coen.capsulecrm.client.{SimpleCapsuleEntity, COrganisation, CParty}
import play.api.Play.current
import ExecutionContext.Implicits.global

object Global extends GlobalSettings {
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
    camelContext.start()

    def postcodeUnitCollection: BSONCollection = ReactiveMongoPlugin.db.collection[BSONCollection]("pcu")
    def contactCollection: BSONCollection = ReactiveMongoPlugin.db.collection[BSONCollection]("contacts")

    app.configuration.getString("capsulecrm.url") match {
      case Some(url) =>
        // import contacts from capsule
        CParty.listAll().get().foreach {
          party =>
            if (party.firstEmail() != null && party.firstAddress() != null && party.firstAddress().zip != null) {
              contactCollection.insert(Contact(
                party.id,
                party.getName,
                party.firstEmail().emailAddress,
                CharMatcher.WHITESPACE.removeFrom(party.firstAddress().zip)
              ))
            }
        }
      case _ =>
    }

    postcodeUnitCollection.indexesManager.ensure(Index(List("loc" -> Geo2D)))
    contactCollection.indexesManager.ensure(Index(List("postcode" -> Ascending)))
  }

  override def onStop(app: Application) = {
    camelContext.stop()
  }
}