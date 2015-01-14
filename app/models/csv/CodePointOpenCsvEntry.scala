package models.csv

import org.apache.camel.dataformat.bindy.annotation.{DataField, CsvRecord}
import scala.beans.BeanInfo
import scala.annotation.meta.field

/**
 * Represents a Code-Point Open CSV entry.
 *
 * Code-Point Open is a postal geography dataset that features a set of geographically referenced points that
 * represent each of the 1.7 million postcode units in Great Britain. The centre of the postcode unit is derived
 * from the precise coordinates of addresses sharing the same postcode unit in Ordnance Survey’s large-scale address
 * database.
 */
@BeanInfo @CsvRecord(separator = ",") case class CodePointOpenCsvEntry (
  @(DataField@field)(pos = 1) var postcode: String,
  @(DataField@field)(pos = 2) var positionalQualityIndicator: String,
  @(DataField@field)(pos = 3) var eastings: String,
  @(DataField@field)(pos = 4) var northings: String,
  @(DataField@field)(pos = 5) var countryCode: String,
  @(DataField@field)(pos = 6) var nhsRegionalHa: String,
  @(DataField@field)(pos = 7) var nhsHa: String,
  @(DataField@field)(pos = 8) var adminCountryCode: String,
  @(DataField@field)(pos = 9) var adminDistrictCode: String,
  @(DataField@field)(pos = 10) var adminWardCode: String) {

  def this() = this(null, null, null, null, null, null, null, null, null, null)
}