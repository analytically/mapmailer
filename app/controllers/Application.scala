package controllers

import play.api.mvc._
import play.modules.reactivemongo.MongoController

import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson._
import models.{Location, Party}
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future
import play.api.libs.json._
import scala.Some
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.core.commands._
import play.modules.reactivemongo.json.BSONFormats._
import play.extras.geojson._
import models.JsonFormats._
import models.Location
import scala.Some
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.core.commands.RawCommand
import models.Location
import reactivemongo.bson.BSONString
import scala.Some
import reactivemongo.api.collections.default.BSONCollection
import com.google.common.base.CharMatcher

/**
 * The Distinct command.
 *
 * Returns a document containing the distinct number of documents matching the query.
 * @param collectionName the name of the target collection
 * @param field for which field to return distinct values
 * @param query the document selector
 */
case class Distinct(collectionName: String,
                    field: String,
                    query: Option[BSONDocument] = None) extends Command[List[String]] {
  override def makeDocuments =
    BSONDocument(
      "distinct" -> BSONString(collectionName),
      "key" -> field,
      "query" -> query)

  val ResultMaker = Distinct
}

/**
 * Deserializer for the Distinct command. Basically returns a List[String].
 */
object Distinct extends BSONCommandResultMaker[List[String]] {
  def apply(document: BSONDocument) =
    CommandError.checkOk(document, Some("distinct")).toLeft(document.getAs[List[String]]("values").get)
}

object Application extends Controller with MongoController {
  def partyCollection: BSONCollection = db.collection[BSONCollection]("parties")
  def postcodeUnitCollection: BSONCollection = db.collection[BSONCollection]("pcu")

  implicit val locationWites = Json.writes[Location]
  implicit val partyWrites = Json.writes[Party]

  def index = Action.async {
    db.command(Distinct("parties", "grps")).map {
      groups =>
        Ok(views.html.index(groups.map(group => CharMatcher.JAVA_LETTER.retainFrom(group)).take(10), Location(-2, 53), false))
    }
  }

  def indexWithMarker(partyId: String) = Action.async {
    partyCollection.find(BSONDocument("cid" -> partyId)).one[Party].map {
      case Some(party) => Ok(views.html.index(Nil, party.location, true, Some(party.name + " (" + party.postcode + ")")))
      case None => NotFound("not found")
    }
  }

  def search = Action.async(parse.json) {
    request =>
      val coordinates = (request.body \ "geometry" \ "coordinates").as[List[List[List[Double]]]].flatten.map(coordinate => BSONArray(coordinate.head, coordinate.tail.head))

      val parties = partyCollection.find(BSONDocument("loc" ->
        BSONDocument("$geoWithin" -> BSONDocument("$geometry" -> BSONDocument("type" -> "Polygon", "coordinates" -> BSONArray(BSONArray(coordinates)))))))
        .cursor[Party]

      parties.collect[List](upTo = 1000).map {
        parties =>
          Ok(Json.toJson(parties))
      }
  }
}