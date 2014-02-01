package controllers

import play.api.mvc._
import play.modules.reactivemongo.MongoController

import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.BSONDocument
import models.{Location, Contact}
import play.api.libs.concurrent.Execution.Implicits._

object Application extends Controller with MongoController {
  def contactCollection: BSONCollection = db.collection[BSONCollection]("contacts")
  def postcodeUnitCollection: BSONCollection = db.collection[BSONCollection]("pcu")

  def index = Action {
    Ok(views.html.index(Location(-2, 53), false))
  }

  def indexWithMarker(contactId: String) = Action.async {
    val contactOption = contactCollection.find(BSONDocument("cid" -> contactId)).one[Contact]
    contactOption.map {
      case Some(contact) =>
        Ok(views.html.index(contact.location, true, Some(contact.name)))
      case None => NotFound("not found")
    }
  }
}