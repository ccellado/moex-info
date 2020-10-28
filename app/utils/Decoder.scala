package utils

import magnolia._
import scala.language.experimental.macros
import java.sql.Date
import models.renamed
/** very basic decoder for converting strings to other types */
trait Decoder[T] { def decode(str: String): T }

/** derivation object (and companion object) for [[Decoder]] instances */
object Decoder {

  /** decodes strings */
  implicit val string: Decoder[String] = (s: String) => s
  implicit val stringO: Decoder[Option[String]] = (s: String) =>
    s match {
      case "NULL" => None
      case _      => Some(s)
    }

  /** decodes ints */
  implicit val int: Decoder[Int] = _.toInt
  implicit val intO: Decoder[Option[Int]] = (s: String) =>
    s match {
      case "NULL" => None
      case _      => Some(s.toInt)
    }

  /** decodes doubles */
  implicit val double: Decoder[Double] = _.toDouble
  implicit val doubleO: Decoder[Option[Double]] = (s: String) =>
    s match {
      case "NULL" => None
      case _      => Some(s.toDouble)
    }

  /** decodes java.Date */
  implicit val date: Decoder[Date] = Date.valueOf(_)
  implicit val dateO: Decoder[Option[Date]] = (s: String) =>
    s match {
      case "NULL" => None
      case x      => Some(Date.valueOf(x))
    }

  /** binds the Magnolia macro to this derivation object */
  implicit def gen[T]: Decoder[T] = macro Magnolia.gen[T]

  /** type constructor for new instances of the typeclass */
  type Typeclass[T] = Decoder[T]

  /** defines how new [[Decoder]]s for case classes should be constructed */
  def combine[T](ctx: CaseClass[Decoder, T]): Decoder[T] =
    value => {
      val (_, values) = parse(value)
      ctx.construct { param =>
        /** look for a annotation rename */
        val renameOpt = param.annotations.collectFirst {
          case renamed: renamed => renamed._type
        }
        param.typeclass.decode(values(renameOpt.getOrElse(param.label)))
      }
    }

  /** defines how to choose which subtype of the sealed trait to use for decoding */
  def dispatch[T](ctx: SealedTrait[Decoder, T]): Decoder[T] =
    param => {
      val (name, _) = parse(param)
      val subtype = ctx.subtypes.find(_.typeName.full == name).get
      subtype.typeclass.decode(param)
    }

  /** very simple extractor for grabbing an entire parameter value, assuming matching parentheses */
  private def parse(value: String): (String, Map[String, String]) = {
    val end = value.indexOf('[')
    val name = value.substring(0, end)

    def parts(
        value: String,
        idx: Int = 0,
        depth: Int = 0,
        collected: List[String] = List("")
    ): List[String] = {
      def plus(char: Char): List[String] =
        collected.head + char :: collected.tail

      if (idx == value.length) collected
      else
        value(idx) match {
          case '[' =>
            parts(value, idx + 1, depth + 1, plus('['))
          case ']' =>
            if (depth == 1) plus(']')
            else parts(value, idx + 1, depth - 1, plus(']'))
          case '|' =>
            if (depth == 0) parts(value, idx + 1, depth, "" :: collected)
            else parts(value, idx + 1, depth, plus('|'))
          case char =>
            parts(value, idx + 1, depth, plus(char))
        }
    }

    def keyValue(str: String): (String, String) = {
      val List(label, value) = str.split("=", 2).toList
      (label, value)
    }

    (
      name,
      parts(value.substring(end + 1, value.length - 1)).map(keyValue).toMap
    )
  }
}
