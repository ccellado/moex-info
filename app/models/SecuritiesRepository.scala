package models

import java.sql.Date
import javax.inject.{Inject, Singleton}
import play.api.db.slick.{HasDatabaseConfigProvider, DatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.lifted.ColumnOrdered
import scala.xml.{XML, Node, NodeSeq}
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Await}

import slick.jdbc.PostgresProfile.api._
class SecuritiesTable(tag: Tag) extends Table[Securities](tag, "SECURITIES") {
  def id = column[Int]("ID")
  def secid = column[String]("SECID", O.PrimaryKey)
  def shortname = column[Option[String]]("SHORTNAME")
  def regnumber = column[Option[String]]("REGNUMBER")
  def name = column[String]("NAME")
  def isin = column[Option[String]]("ISIN")
  def is_traded = column[Option[Int]]("IS_TRADED")
  def emitent_id = column[Option[Int]]("EMITENT_ID")
  def emitent_title = column[Option[String]]("EMITENT_TITLE")
  def emitent_inn = column[Option[String]]("EMITENT_INN")
  def emitent_okpo = column[Option[String]]("EMITENT_OKPO")
  def gosreg = column[Option[String]]("GOSREG")
  def _type = column[Option[String]]("TYPE")
  def group = column[Option[String]]("GROUP")
  def primary_boardid = column[Option[String]]("PRIMARY_BOARDID")
  def marketprice_boardid = column[Option[String]]("MARKETPRICE_BOARDID")
  def * =
    (
      id,
      secid,
      shortname,
      regnumber,
      name,
      isin,
      is_traded,
      emitent_id,
      emitent_title,
      emitent_inn,
      emitent_okpo,
      gosreg,
      _type,
      group,
      primary_boardid,
      marketprice_boardid
    ).<>((Securities.apply _).tupled, Securities.unapply)
}

@Singleton
class SecuritiesRepository @Inject() (
    protected val dbConfigProvider: DatabaseConfigProvider
)(implicit ec: ExecutionContext)
    extends HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  private lazy val securitiesTable = TableQuery[SecuritiesTable]

  private def decoder = implicitly[utils.Decoder[Securities]].decode(_)

  object Api {
    lazy val table = securitiesTable

    def exec[T](action: DBIO[T]): T =
      Await.result(db.run(action), 2.seconds)

    def list(): Future[Seq[Securities]] =
      db.run {
        table.result
      }

    def deleteAll =
      db.run {
        table.delete
      }

    def create =
      db.run {
        table.schema.createIfNotExists
      }

    def update(command: Securities) = {
      lazy val action = table += command
      val charSet = (('а' to 'я') ++ ('А' to 'Я') ++ ('0' to '9') ++ " ").toSet
      if (command.name.forall(charSet.contains(_))) exec(action)
    }

    def updateBatch(command: Seq[Securities]) = {
      lazy val action: DBIO[Option[Int]] = table ++= command
      exec(action)
    }

    def importXml(xml: NodeSeq) = {
      val attrs =
        (xml \\ "column" \\ "@name")
      updateBatch(
        for (
          row <- (xml \\ "row")
          if ((row \\ "@secid").text != "")
          if !exec(
            (for (
              u <- securitiesTable if u.secid === (row \\ "@secid").text.bind
            ) yield u).exists.result
          )
        )
          yield decoder(utils.XmlUtils.parseRows(row, attrs))
      )
    }
  }
}
