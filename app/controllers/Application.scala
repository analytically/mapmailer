package controllers

import play.api.mvc._
import play.modules.reactivemongo.MongoController

import reactivemongo.api.collections.default.BSONCollection

object Application extends Controller with MongoController {
  def postcodeUnit: BSONCollection = db.collection[BSONCollection]("pcu")

  def index = Action {
    Ok(views.html.index())
  }
}