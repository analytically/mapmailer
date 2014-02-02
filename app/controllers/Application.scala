package controllers

import play.api.mvc._
import play.modules.reactivemongo.MongoController

import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.BSONDocument
import models.{Location, Party}
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future
import play.api.libs.json._
import scala.Some
import reactivemongo.api.collections.default.BSONCollection
import play.modules.reactivemongo.json.BSONFormats._
import play.extras.geojson._

object Application extends Controller with MongoController {
  def partyCollection: BSONCollection = db.collection[BSONCollection]("parties")
  def postcodeUnitCollection: BSONCollection = db.collection[BSONCollection]("pcu")

  implicit val locationWites = Json.writes[Location]
  implicit val partyWrites = Json.writes[Party]

  def index = Action {
    Ok(views.html.index(Location(-2, 53), false))
  }

  def indexWithMarker(partyId: String) = Action.async {
    val partyOption = partyCollection.find(BSONDocument("cid" -> partyId)).one[Party]
    partyOption.map {
      case Some(party) =>
        Ok(views.html.index(party.location, true, Some(party.name)))
      case None => NotFound("not found")
    }
  }

  def search = Action.async(parse.json) { request =>
    Console.println(request.body)

    /*val parties = partyCollection
      .find(BSONDocument("loc" -> BSONDocument("$geoWithin" -> BSONDocument("$geometry" -> ))))
      .cursor[Party]*/

    Future.successful(Ok(Json.toJson(List(Party("2", "name", "email", "", true, Location(52,-1))))))
  }
}