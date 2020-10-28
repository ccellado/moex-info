package models

import java.sql.Date
import javax.inject.{Inject, Singleton}
import play.api.db.slick.{HasDatabaseConfigProvider, DatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import scala.concurrent.{Future, ExecutionContext}
import slick.lifted.ColumnOrdered
import scala.xml.{XML, Node, NodeSeq}

@Singleton
class HelpersRepository @Inject() (
    protected val dbConfigProvider: DatabaseConfigProvider
    )(
    implicit ec: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._
  
  private val securitiesHistoryTable = TableQuery[models.SecuritiesHistoryTable]
  private val securitiesTable = TableQuery[models.SecuritiesTable]
  // Alias type
  private type SpecialQuery = Query[
    (models.SecuritiesTable, Rep[Option[models.SecuritiesHistoryTable]]),
    (Securities, Option[SecuritiesHistory]),
    Seq
  ]

  private def chooseSortAsc(sort: String) =
    (query: SpecialQuery) => {
      sort match {
        case "name"          => query.sortBy(_._1.name)
        case "secid"         => query.sortBy(_._1.secid)
        case "regnumber"     => query.sortBy(_._1.regnumber)
        case "emitent_title" => query.sortBy(_._1.emitent_title)
        case "tradedate"     => query.sortBy(_._2.map(_.tradedate))
        case "numtrades"     => query.sortBy(_._2.map(_.numtrades))
        case "open"          => query.sortBy(_._2.map(_.open))
        case "close"         => query.sortBy(_._2.map(_.close))
        case _               => query
      }
    }

  private def chooseSortDesc(sort: String) =
    (query: SpecialQuery) => {
      sort match {
        case "d_name"          => query.sortBy(_._1.name.desc)
        case "d_secid"         => query.sortBy(_._1.secid.desc)
        case "d_regnumber"     => query.sortBy(_._1.regnumber.desc)
        case "d_emitent_title" => query.sortBy(_._1.emitent_title.desc)
        case "d_tradedate"     => query.sortBy(_._2.map(_.tradedate).desc)
        case "d_numtrades"     => query.sortBy(_._2.map(_.numtrades).desc)
        case "d_open"          => query.sortBy(_._2.map(_.open).desc)
        case "d_close"         => query.sortBy(_._2.map(_.close).desc)
        case _                 => query
      }
    }

  private def chooseSortCombine(
      item: List[String],
      query: SpecialQuery
  ): SpecialQuery = {
    item match {
      case Nil => chooseSortDesc("")(query)
      case head :: tail =>
        if (head contains "d_")
          chooseSortDesc(head)(chooseSortCombine(item.tail, query))
        else
          chooseSortAsc(head)(chooseSortCombine(item.tail, query))
    }
  }

  private def filterEmitent(sort: SortBy, query: SpecialQuery): SpecialQuery =
    sort.emitent_filter match {
      case Some(x) => query.filter(_._1.emitent_title like ("%" + x + "%"))
      case None    => query
    }

  private def filterTradedateFrom(
      sort: SortBy,
      query: SpecialQuery
  ): SpecialQuery =
    sort.tradedate_from match {
      case Some(x) => query.filter(_._2.map(_.tradedate) >= x)
      case None    => query
    }

  private def filterTradedateTo(
      sort: SortBy,
      query: SpecialQuery
  ): SpecialQuery =
    sort.tradedate_to match {
      case Some(x) => query.filter(_._2.map(_.tradedate) <= x)
      case None    => query
    }

  def listSpecial(sort: SortBy): Future[Seq[SpecialTable]] =
    db.run {
      val initialQuery =
        securitiesTable.joinLeft(securitiesHistoryTable).on(_.secid === _.secid)
      val items: List[String] = sort.by.split(",").toList
      val sortedQuery = chooseSortCombine(items, initialQuery)
      val queryVal = filterTradedateTo(
        sort,
        filterTradedateFrom(sort, filterEmitent(sort, sortedQuery))
      )
      val finalQuery = queryVal.map {
        case (x, y) =>
          (
            x.secid,
            x.regnumber,
            x.name,
            x.emitent_title,
            y.map(_.tradedate),
            y.map(_.numtrades),
            y.map(_.open),
            y.map(_.close)
          ).<>((SpecialTable.apply _).tupled, SpecialTable.unapply)
      }
      finalQuery.result
    }
}
