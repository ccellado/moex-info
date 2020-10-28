package utils

import scala.xml.{XML, Node, NodeSeq}

object XmlUtils {
  implicit class XMLQueryWithAttribute(val xml: NodeSeq) extends AnyVal {
    /** needed to extend XML search by a tag
    ** usage: \\@ ("ATTR", _ == "VALUE") */
    def \\@(attr: (String, String => Boolean)): NodeSeq = {
      xml filter {
        _ \ ("@" + attr._1) exists (s => attr._2(s.text))
      }
    }
  }

  /** convert XML data to [[utils.Decoder]] readable form */
  def parseRows(row: NodeSeq, attrs: NodeSeq): String = {
    var query: String = "["
    for (tag <- attrs) {
      val value = (row \\ ("@" + tag)).text
      if (value == "") query += (tag.text.toLowerCase + "=" + "NULL")
      else query += (tag.text.toLowerCase + "=" + value)
      query += '|'
    }
    query = query.dropRight(1)
    query += ']'
    return query
  }

}
