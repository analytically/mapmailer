package controllers

import play.api.mvc._
import play.modules.reactivemongo.MongoController

import reactivemongo.bson._
import models._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json._
import com.google.common.base.CharMatcher
import models.Location
import scala.Some
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.Distinct
import play.modules.reactivemongo.json.BSONFormats._
import JsonFormats._

object Application extends Controller with MongoController {
  def partyCollection: BSONCollection = db.collection[BSONCollection]("parties")

  implicit val locationWites = Json.writes[Location]
  implicit val partyWrites = Json.writes[Party]

  def index = Action.async {
    db.command(Distinct("parties", "grps")).map {
      groups =>
        Ok(views.html.index(groups.sorted, Location(-2, 53)))
    }
  }

  def party(partyId: String) = Action.async {
    partyCollection.find(BSONDocument("cid" -> partyId)).one[Party].map {
      case Some(party) => Ok(Json.toJson(party))
      case None => NotFound("not found")
    }
  }

  def search = Action.async(parse.json) {
    request =>
      val parties = (request.body \ "geometry" \ "type").as[String].toLowerCase match {
        case "circle" =>
          val coordinates = (request.body \ "geometry" \ "coordinates").as[Array[Double]]

          partyCollection.find(BSONDocument("loc" ->
            BSONDocument("$geoWithin" -> BSONDocument("$centerSphere" -> BSONArray(coordinates, (request.body \ "geometry" \ "radius").as[Double] / 1609.34 / 3959)))))
            .cursor[Party]

        case "polygon" =>
          val coordinates = (request.body \ "geometry" \ "coordinates").as[Array[Array[Array[Double]]]].flatten.map(coordinate => BSONArray(coordinate.head, coordinate.tail.head))

          partyCollection.find(BSONDocument("loc" ->
            BSONDocument("$geoWithin" -> BSONDocument("$geometry" -> BSONDocument("type" -> "Polygon", "coordinates" -> BSONArray(BSONArray(coordinates)))))))
            .cursor[Party]

        case _ => ???
      }

      parties.collect[List](upTo = 300).map {
        parties =>
          Ok(Json.toJson(parties))
      }
  }
}