package models

import java.sql.Date
import javax.inject.{Inject, Singleton}
import scala.xml.{XML, Node, NodeSeq}
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Await}
import play.api.db.slick.{HasDatabaseConfigProvider, DatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.lifted.ColumnOrdered
import utils.XmlUtils._
import utils.RequestData

import slick.jdbc.PostgresProfile.api._
// Definition of the SecuritiesHistory table
class SecuritiesHistoryTable(tag: Tag)
    extends Table[SecuritiesHistory](tag, "SECURITIES_HISTORY") {
  def boardid = column[String]("BOARDID")
  def tradedate = column[Date]("TRADEDATE")
  def secid = column[String]("SECID")
  def shortname = column[Option[String]]("SHORTNAME")
  def numtrades = column[Double]("NUMTRADES")
  def value = column[Double]("VALUE")
  def open = column[Double]("OPEN")
  def low = column[Option[Double]]("LOW")
  def high = column[Option[Double]]("HIGH")
  def legalcloseprice = column[Option[Double]]("LEGALCLOSEPRICE")
  def waprice = column[Option[Double]]("WAPRICE")
  def close = column[Double]("CLOSE")
  def volume = column[Option[Double]]("VOLUME")
  def marketprice2 = column[Option[Double]]("MARKETPRICE2")
  def marketprice3 = column[Option[Double]]("MARKETPRICE3")
  def admittedquote = column[Option[Double]]("ADMITTEDQUOTE")
  def mp2valtrd = column[Option[Double]]("MP2VALTRD")
  def marketprice3tradesvalue =
    column[Option[Double]]("MARKETPRICE3TRADESVALUE")
  def admittedvalue = column[Option[Double]]("ADMITTEDVALUE")
  def waval = column[Option[Double]]("WAVAL")
  def * =
    (
      boardid,
      tradedate,
      secid,
      shortname,
      numtrades,
      value,
      open,
      low,
      high,
      legalcloseprice,
      waprice,
      close,
      volume,
      marketprice2,
      marketprice3,
      admittedquote,
      mp2valtrd,
      marketprice3tradesvalue,
      admittedvalue,
      waval
    ).<>((SecuritiesHistory.apply _).tupled, SecuritiesHistory.unapply)
}

@Singleton
class HistoryRepository @Inject() (
    protected val secRepo: SecuritiesRepository,
    protected val reqUtil: RequestData,
    protected val dbConfigProvider: DatabaseConfigProvider
)(implicit
    ec: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  private def decoder = implicitly[utils.Decoder[SecuritiesHistory]].decode(_)

  private lazy val securitiesHistoryTable = TableQuery[SecuritiesHistoryTable]
  private lazy val securitiesTable = TableQuery[models.SecuritiesTable]

  object Api {
    lazy val table = securitiesHistoryTable

    def exec[T](action: DBIO[T]): T =
      Await.result(db.run(action), 2.seconds)

    def list(): Future[Seq[SecuritiesHistory]] =
      db.run {
        securitiesHistoryTable.result
      }

    def create =
      db.run { table.schema.createIfNotExists }

    def deleteAll =
      db.run { table.delete }

    def update(command: SecuritiesHistory) = {
      lazy val action = table += command
      val existingSecid: Seq[String] = exec(
        securitiesTable.map(_.secid).result
      )
      exec(action)
    }

    def updateBatch(command: Seq[SecuritiesHistory]) = {
      val action: DBIO[Option[Int]] = table ++= command
      exec(action)
    }

    private def requestSecurity(query: List[String]): Boolean = {
      val search = query.mkString(",")
      if (
        !(secRepo.Api.exec(
          (for (u <- securitiesTable if u.secid === query.head.bind)
            yield u).exists.result
        ))
      ) {
        val res =
          Await.result(reqUtil.showSomeSiteContent(search), 1.second).xml
        if ((res \\ "rows" \\ "row") == Seq()) false
        else { secRepo.Api.importXml(res); true }
      } else false
    }

    def importXml(xml: NodeSeq) = {
      val existingSecid: Seq[String] = exec(
        securitiesTable.map(_.secid).result
      )
      val attrs =
        (xml \\ "data" \\@ ("id", _ == "history") \\ "column" \\ "@name")
      val batch: Seq[Option[SecuritiesHistory]] =
        (
          for (
            row <- (xml \\ "row")
            if ((row \\ "@OPEN").text != "")
            if ((row \\ "@CLOSE").text != "")
            if ((row \\ "@NUMTRADES").text != "")
            if ((row \\ "@SECID").text != "")
            if ((row \\ "@TRADEDATE").text != "")
            if !(secRepo.Api.exec(
              (for (
                h <- securitiesHistoryTable
                if h.open === (row \\ "@OPEN").text.toDouble.bind
                if h.close === (row \\ "@CLOSE").text.toDouble.bind
                if h.numtrades === (row \\ "@NUMTRADES").text.toDouble.bind
                if h.secid === (row \\ "@SECID").text.bind
                if h.tradedate === Date.valueOf((row \\ "@TRADEDATE").text).bind
              )
                yield h).exists.result
            ))
          )
            yield {
              if (!existingSecid.contains((row \\ "@SECID").text)) {
                if (
                  requestSecurity(
                    List((row \\ "@SECID").text, (row \\ "@SHORTNAME").text)
                  )
                )
                  Some(decoder(parseRows(row, attrs)))
                else None
              } else Some(decoder(parseRows(row, attrs)))
            }
        )
      updateBatch(batch.flatten)
    }
  }
}
