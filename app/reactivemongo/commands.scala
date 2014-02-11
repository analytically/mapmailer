package reactivemongo

import reactivemongo.bson.{BSONString, BSONDocument}
import reactivemongo.core.commands.{CommandError, BSONCommandResultMaker, Command}

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