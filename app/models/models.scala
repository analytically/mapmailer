package models

import reactivemongo.bson.Macros.Annotations.Key
import reactivemongo.bson.{BSONObjectID, Macros}
import play.modules.reactivemongo.json.BSONFormats._

case class Contact(@Key("n") capsuleId: Long,
                   @Key("n") name: String,
                   @Key("em") emailAddress: String,
                   @Key("pc") postcode: String,
                   id: Option[BSONObjectID] = None)

object Contact {
  implicit val contactHandler = Macros.handler[Contact]
}

case class Location(@Key("lng") longitude: Double,
                    @Key("lat") latitude: Double,
                    id: Option[BSONObjectID] = None)

object Location {
  implicit val locationHandler = Macros.handler[Location]
}

case class PostcodeUnit(@Key("pc") postcode: String,
                        @Key("q") pqi: String,
                        @Key("loc") location: Location,
                        id: Option[BSONObjectID] = None)

object PostcodeUnit {
  implicit val postcodeUnitHandler = Macros.handler[PostcodeUnit]
}

object JsonFormats {
  import play.api.libs.json.Json

  implicit val contactFormat = Json.format[Contact]
}