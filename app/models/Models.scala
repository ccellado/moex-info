package models

import play.api.libs.json._
import scala.annotation.StaticAnnotation
import java.sql.Date

/** special annotation for the "type" field, which is present in the data but is a reserved keyword in scala */
class renamed(val _type: String) extends StaticAnnotation

case class Securities(
    id: Int,
    secid: String,
    shortname: Option[String],
    regnumber: Option[String],
    name: String,
    isin: Option[String],
    is_traded: Option[Int],
    emitent_id: Option[Int],
    emitent_title: Option[String],
    emitent_inn: Option[String],
    emitent_okpo: Option[String],
    gosreg: Option[String],
    @renamed(_type = "type") _type: Option[String],
    group: Option[String],
    primary_boardid: Option[String],
    marketprice_boardid: Option[String]
)

case class SecuritiesHistory(
    boardid: String,
    tradedate: Date,
    secid: String,
    shortname: Option[String],
    numtrades: Double,
    value: Double,
    open: Double,
    low: Option[Double],
    high: Option[Double],
    legalcloseprice: Option[Double],
    waprice: Option[Double],
    close: Double,
    volume: Option[Double],
    marketprice2: Option[Double],
    marketprice3: Option[Double],
    admittedquote: Option[Double],
    mp2valtrd: Option[Double],
    marketprice3tradesvalue: Option[Double],
    admittedvalue: Option[Double],
    waval: Option[Double]
)

case class SpecialTable(
    secid: String,
    regnumber: Option[String],
    name: String,
    emitent_title: Option[String],
    tradedate: Option[Date],
    numtrades: Option[Double],
    open: Option[Double],
    close: Option[Double]
)

case class SortBy(by: String, emitent_filter: Option[String], tradedate_from: Option[Date], tradedate_to: Option[Date])

object SpecialTable {
  implicit val specialTableFormat = Json.format[SpecialTable]
}

object Securities {
  implicit val secutiriesFormat = Json.format[Securities]
}

object SecuritiesHistory {
  implicit val secutiriesHistoryFormat = Json.format[SecuritiesHistory]
}
