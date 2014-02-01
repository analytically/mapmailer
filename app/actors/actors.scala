package actors

import akka.actor.{ActorLogging, Actor}
import org.opengis.referencing.crs.CoordinateReferenceSystem
import org.geotools.referencing.CRS
import org.geotools.referencing.crs.DefaultGeographicCRS
import com.google.common.base.{CharMatcher, Throwables}
import org.opengis.referencing.FactoryException
import models.csv.CodePointOpenCsvEntry
import models.{Location, PostcodeUnit}
import org.opengis.geometry.DirectPosition
import org.geotools.geometry.GeneralDirectPosition
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.api.Play.current
import reactivemongo.api.collections.default.BSONCollection

trait MongoActor extends Actor {
  def driver = ReactiveMongoPlugin.driver
  def connection = ReactiveMongoPlugin.connection
  def db = ReactiveMongoPlugin.db
}

class ProcessCPOCsvEntry extends MongoActor with ActorLogging {
  def pcuCollection: BSONCollection = db.collection[BSONCollection]("pcu")

  import context.dispatcher

  val osgbToWgs84Transform = {
    try {
      val osgbCrs: CoordinateReferenceSystem = CRS.decode("EPSG:27700")
      val wgs84crs: CoordinateReferenceSystem = DefaultGeographicCRS.WGS84
      CRS.findMathTransform(osgbCrs, wgs84crs)
    }
    catch {
      case e: FactoryException => {
        throw Throwables.propagate(e)
      }
    }
  }

  def receive = {
    case entry: CodePointOpenCsvEntry =>
      val eastings: Int = Integer.parseInt(entry.eastings)
      val northings: Int = Integer.parseInt(entry.northings)
      val eastNorth: DirectPosition = new GeneralDirectPosition(eastings, northings)
      val latLng: DirectPosition = osgbToWgs84Transform.transform(eastNorth, eastNorth)

      val postcodeUnit = PostcodeUnit(CharMatcher.WHITESPACE.removeFrom(entry.postcode).toUpperCase,
        entry.positionalQualityIndicator,
        new Location(round(latLng.getOrdinate(0), 8), round(latLng.getOrdinate(1), 8))
      )

      pcuCollection.insert(postcodeUnit)
  }

  def round(valueToRound: Double, numberOfDecimalPlaces: Int): Double = {
    val multipicationFactor: Double = Math.pow(10, numberOfDecimalPlaces)
    val interestedInZeroDPs: Double = valueToRound * multipicationFactor
    Math.round(interestedInZeroDPs) / multipicationFactor
  }
}
