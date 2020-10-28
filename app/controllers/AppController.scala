package controllers
import models._
import javax.inject._
import play.api.mvc._
import play.Application
import play.api.i18n._
import play.api.libs.json.Json
import java.nio.file.{Paths, Path}
import scala.concurrent.duration._
import scala.xml.{XML, Node, NodeSeq}

import scala.concurrent.{ExecutionContext, Future, Await}

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */

class AppController @Inject() (
    secRepo: SecuritiesRepository,
    hisRepo: HistoryRepository,
    helpRepo: HelpersRepository,
    cc: MessagesControllerComponents,
    application: Application
)(implicit ec: ExecutionContext, assetsFinder: AssetsFinder)
    extends MessagesAbstractController(cc) {
  import play.api.data.Forms._
  import play.api.data.Form
  import play.api.data.format.Formats._

  val rootPath: Path = application.path.toPath

  /**
    * Create an Action to render an HTML page with a welcome message.
    * The configuration in the `routes` file means that this method
    * will be called when the application receives a `GET` request with
    * a path of `/`.
    */

  val tableNames = List(
    "secid",
    "regnumber",
    "name",
    "emitent_title",
    "tradedate",
    "numtrades",
    "open",
    "close"
  )

  val defaultSort = SortBy("secid", None, None, None)

  val sortForm = Form(
    mapping(
      "by" -> text,
      "emitent_filter" -> optional(text),
      "tradedate_from" -> optional(sqlDate),
      "tradedate_to" -> optional(sqlDate)
    )(SortBy.apply)(SortBy.unapply)
  )

  def index =
    Action.async { implicit request =>
      helpRepo.listSpecial(defaultSort).map { info =>
        Ok(views.html.index(info, tableNames, sortForm, defaultSort))
      }
    }

  def parseUpload(file: NodeSeq) =
    (file \\ "data" \\ "@id").head.toString match {
      case "securities" => { secRepo.Api.importXml(file); Some(1) }
      case "history"    => { hisRepo.Api.importXml(file); Some(1) }
      case _            => None
    }

  def upload =
    Action(parse.multipartFormData) { implicit request =>
      request.body
        .file("xml")
        .map { xml =>
          val file: NodeSeq = scala.xml.XML.loadFile(xml.ref.toFile())
          parseUpload(file) match {
            case None =>
              BadRequest(
                views.html.index(
                  Await.result(helpRepo.listSpecial(defaultSort), 2.seconds),
                  tableNames,
                  sortForm,
                  defaultSort
                )
              )
            case Some(_) =>
              Ok(
                views.html.index(
                  Await.result(helpRepo.listSpecial(defaultSort), 2.seconds),
                  tableNames,
                  sortForm,
                  defaultSort
                )
              )
          }

        }
        .getOrElse {
          BadRequest(
            views.html.index(
              Await.result(helpRepo.listSpecial(defaultSort), 2.seconds),
              tableNames,
              sortForm,
              defaultSort
            )
          )
        }
    }

  def getSort =
    Action { implicit request =>
      sortForm
        .bindFromRequest()(request)
        .fold(
          (formContainingErrors: Form[SortBy]) => {
            println("error")
            println(formContainingErrors)
            // Show the user a completed form with error messages:
            BadRequest(
              views.html.index(
                Await.result(helpRepo.listSpecial(defaultSort), 2.seconds),
                tableNames,
                sortForm,
                defaultSort
              )
            )
          },
          (sort: SortBy) => {
            Ok(
              views.html.index(
                Await.result(helpRepo.listSpecial(sort), 2.seconds),
                tableNames,
                sortForm,
                sort
              )
            )
          }
        )
    }

  /**
    * A REST endpoints that gets all avaliable data as JSON.
    */
  def getSecurities =
    Action.async { implicit request =>
      helpRepo.listSpecial(defaultSort).map { securities =>
        Ok(Json.toJson(securities))
      }
    }
  def getSecuritiesHistory =
    Action.async { implicit request =>
      secRepo.Api.list().map { securities =>
        Ok(Json.toJson(securities))
      }
    }
  def getSpecialTable =
    Action.async { implicit request =>
      hisRepo.Api.list().map { securities =>
        Ok(Json.toJson(securities))
      }
    }
}
