import com.google.common.util.concurrent.Futures
import java.util.concurrent
import models.Location
import models.{Location, Party}
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import org.specs2.time.NoTimeConversions
import play.api.test._
import play.api.test.FakeApplication
import play.api.test.Helpers._
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoPlugin
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.BSONDocument
import scala.concurrent._
import scala.concurrent.duration._
import scala.Some
import uk.co.coen.capsulecrm.client._
import scala.collection.JavaConversions._

@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends Specification with NoTimeConversions {

  def testConfig: Map[String, _] = {
    Map(
      "mongodb.uri" -> "mongodb://localhost:27017/mapmailer-test",
      "ratelimit" -> 100,
      "groups.ignore" -> List("ignore")
    )
  }

  "MapMailer application" should {
    "send 404 on a bad request" in new WithApplication(FakeApplication(additionalConfiguration = testConfig)) {
      route(FakeRequest(GET, "/boum")) must beNone
    }

    "render the index page" in new WithApplication(FakeApplication(additionalConfiguration = testConfig)) {
      val request = route(FakeRequest(GET, "/")).get

      status(request) must equalTo(OK)
      contentType(request) must beSome.which(_ == "text/html")
      contentAsString(request) must contain("MapMailer")
    }

    "get party with invalid num id" in new WithApplication(FakeApplication(additionalConfiguration = testConfig)) {
      val request = route(FakeRequest(GET, "/party/123456789")).get
      status(request) must equalTo(NOT_FOUND)
    }

    "get party with invalid alphanum id" in new WithApplication(FakeApplication(additionalConfiguration = testConfig)) {
      val request = route(FakeRequest(GET, "/party/abc123")).get
      status(request) must equalTo(NOT_FOUND)
    }

    "get party with valid id" in new WithApplication(FakeApplication(additionalConfiguration = testConfig)) {
      import ExecutionContext.Implicits.global

      val partyCollection = ReactiveMongoPlugin.db.collection[BSONCollection]("parties")
      Await.result(partyCollection.insert(Party("12345678", "Some School", "mathias.bogaert@gmail.com", Some("http://www.coen.co.uk/"), "DY10 4PW",
        true, Location(-2.18494136, 52.3621734), List("Institution", "Independent"))), 10 seconds)

      val request = route(FakeRequest(GET, "/party/12345678")).get
      status(request) must equalTo(OK)
      contentType(request) must beSome.which(_ == "application/json")
      contentAsString(request) must contain("Some School")

      partyCollection.remove(BSONDocument("pid" -> "12345678"))
    }

    "search with invalid json" in new WithApplication(FakeApplication(additionalConfiguration = testConfig)) {
      val request = route(FakeRequest.apply(POST, "/party/search").withJsonBody(Json.obj(
        "blah" -> 98
      ))).get

      status(request) must equalTo(BAD_REQUEST)
    }

    "search with valid json but invalid type" in new WithApplication(FakeApplication(additionalConfiguration = testConfig)) {
      val request = route(FakeRequest.apply(POST, "/party/search").withJsonBody(Json.obj(
        "geometry" -> Json.obj(
          "type" -> "blah"
        )
      ))).get

      status(request) must equalTo(BAD_REQUEST)
    }

    "search with valid json but invalid content" in new WithApplication(FakeApplication(additionalConfiguration = testConfig)) {
      val request = route(FakeRequest.apply(POST, "/party/search").withJsonBody(Json.obj(
        "geometry" -> Json.obj(
          "type" -> "circle",
          "coordinates" -> "blah"
        )
      ))).get

      status(request) must equalTo(BAD_REQUEST)
    }

    "search with valid json" in new WithApplication(FakeApplication(additionalConfiguration = testConfig)) {
      import ExecutionContext.Implicits.global

      val partyCollection = ReactiveMongoPlugin.db.collection[BSONCollection]("parties")
      Await.result(partyCollection.insert(Party("12345678", "Some School", "mathias.bogaert@gmail.com", Some("http://www.coen.co.uk/"), "DY10 4PW",
        true, Location(-2.18494136, 52.3621734), List("Institution", "Independent"))), 10 seconds)

      val inclusiveRequest = route(FakeRequest.apply(POST, "/party/search").withJsonBody(Json.parse(
        """{"type":"Feature","properties":{},"geometry":{"type":"Circle","coordinates":[-2.1533203125,52.382305628707854],"radius":19643.358128558553}}"""
      ))).get

      status(inclusiveRequest) must equalTo(OK)
      contentType(inclusiveRequest) must beSome.which(_ == "application/json")
      contentAsString(inclusiveRequest) must contain("Some School")
      contentAsString(inclusiveRequest) must contain("DY10 4PW")

      val exclusiveRequest = route(FakeRequest.apply(POST, "/party/search").withJsonBody(Json.parse(
        """{"type":"Feature","properties":{},"geometry":{"type":"Circle","coordinates":[-2.2533203125,52.882305628707854],"radius":19643.358128558553}}"""
      ))).get

      status(exclusiveRequest) must equalTo(OK)
      contentType(exclusiveRequest) must beSome.which(_ == "application/json")
      contentAsString(exclusiveRequest) must contain("[]")

      partyCollection.remove(BSONDocument("pid" -> "12345678"))
    }

    /*"import and search with valid json" in new WithApplication(FakeApplication(additionalConfiguration = testConfig)) {
      import ExecutionContext.Implicits.global

      val pcuCollection: BSONCollection = ReactiveMongoPlugin.db.collection[BSONCollection]("pcu")
      val partyCollection: BSONCollection = ReactiveMongoPlugin.db.collection[BSONCollection]("parties")

      pcuCollection.insert(BSONDocument("pc" -> "SOMEWHEREELSE", "loc" -> BSONDocument("lng" -> -2.48494136, "lat" -> 52.9621734)))
      pcuCollection.insert(BSONDocument("pc" -> "DY104PW", "loc" -> BSONDocument("lng" -> -2.18494136, "lat" -> 52.3621734)))

      val tags = new CTags()
      tags.size = 1
      tags.tags = List(new CTag("group1"), new CTag("group2"), new CTag("ignore"))

      val organisation = new COrganisation("Some School")
      organisation.id = 12345678

      organisation.addContact(new CEmail(null, "mathias.bogaert@gmail.com"))
      val address = new CAddress(null, null, null, "DY10 4PW", null, null)
      organisation.addContact(address)

      Await.result(Global.importParty(pcuCollection, partyCollection, organisation, tags), 10 seconds)

      val inclusiveRequest = route(FakeRequest.apply(POST, "/party/search").withJsonBody(Json.parse(
        """{"type":"Feature","properties":{},"geometry":{"type":"Circle","coordinates":[-2.1533203125,52.382305628707854],"radius":19643.358128558553}}"""
      ))).get

      status(inclusiveRequest) must equalTo(OK)
      contentType(inclusiveRequest) must beSome.which(_ == "application/json")
      contentAsString(inclusiveRequest) must contain("Some School")
      contentAsString(inclusiveRequest) must contain("DY10 4PW")
      contentAsString(inclusiveRequest) must contain("Group1")
      contentAsString(inclusiveRequest) must contain("Group2")
      contentAsString(inclusiveRequest) must not contain("ignore")

      address.zip = "SOMEWHEREELSE"
      Await.result(Global.importParty(pcuCollection, partyCollection, organisation, tags), 10 seconds)

      val inclusiveRequestNotFound = route(FakeRequest.apply(POST, "/party/search").withJsonBody(Json.parse(
        """{"type":"Feature","properties":{},"geometry":{"type":"Circle","coordinates":[-2.1533203125,52.382305628707854],"radius":19643.358128558553}}"""
      ))).get

      status(inclusiveRequestNotFound) must equalTo(OK)
      contentAsString(inclusiveRequestNotFound) must contain("[]")

      partyCollection.remove(BSONDocument("pid" -> "12345678"))
      pcuCollection.remove(BSONDocument("pc" -> "DY104PW"))
    }*/
  }
}
