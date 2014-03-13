package models

import reactivemongo.bson.Macros.Annotations.Key
import reactivemongo.bson.{BSONObjectID, Macros}
import play.modules.reactivemongo.json.BSONFormats._

case class Location(@Key("lng") longitude: Double,
                    @Key("lat") latitude: Double)

case class Party(@Key("cid") partyId: String,
                 @Key("n") name: String,
                 @Key("em") emailAddress: String,
                 @Key("ws") website: Option[String],
                 @Key("pc") postcode: String,
                 @Key("org") organisation: Boolean,
                 @Key("loc") location: Location,
                 @Key("grps") groups: List[String],
                 _id: BSONObjectID = BSONObjectID.generate)

object Party {
  implicit val locationHandler = Macros.handler[Location]
  implicit val partyHandler = Macros.handler[Party]
}

case class PostcodeUnit(@Key("pc") postcode: String,
                        @Key("loc") location: Location,
                        _id: BSONObjectID = BSONObjectID.generate)

object PostcodeUnit {
  implicit val locationHandler = Macros.handler[Location]
  implicit val postcodeUnitHandler = Macros.handler[PostcodeUnit]
}

object JsonFormats {
  import play.api.libs.json.Json

  implicit val locationFormat = Json.format[Location]
  implicit val partyFormat = Json.format[Party]

  implicit val locationWites = Json.writes[Location]
  implicit val partyWrites = Json.writes[Party]
}