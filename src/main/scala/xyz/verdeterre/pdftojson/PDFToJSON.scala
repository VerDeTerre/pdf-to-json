package xyz.verdeterre.pdftojson

import java.io.File

import collection.mutable.ListBuffer

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.util.{PDFTextStripper,TextPosition}

import org.json.{JSONArray,JSONObject}

class PDFToJSON(file: File) {
  case class Element(
    var value: String,
    var top: Float,
    var bottom: Float,
    var left: Float,
    var right: Float
  ) {
    def merge(other: Element): Element = {
      value += other.value
      top = math.min(top, other.top)
      bottom = math.max(bottom, other.bottom)
      left = math.min(left, other.left)
      right = math.max(right, other.right)
      this
    }

    def overlaps(other: Element): Boolean = top == other.top && (math.abs(right - other.left) < 0.8)

    def asJSON: JSONObject = {
      val map = new java.util.HashMap[String, Object]()
      map.put("value", value)
      map.put("left", new java.lang.Float(left))
      map.put("right", new java.lang.Float(right))
      new JSONObject(map)
    }
  }

  class Line(initialElement: Element) {
    var top = initialElement.top
    var bottom = initialElement.bottom
    val elements = ListBuffer(initialElement)

    def append(element: Element): Line = {
      top = math.min(top, element.top)
      bottom = math.max(bottom, element.bottom)
      elements += element
      this
    }

    private def distinctSortedElements = elements.distinct.sortBy(element => element.left)

    def asJSON: JSONObject = {
      val map = new java.util.HashMap[String, Object]()
      map.put("elements", new JSONArray(distinctSortedElements.map(element => element.asJSON).toArray))
      map.put("top", new java.lang.Float(top))
      map.put("bottom", new java.lang.Float(bottom))
      new JSONObject(map)
    }
  }

  val lines = ListBuffer[Line]()

  protected def addToAppropriateLine(element: Element): Line = {
    val line = lines.find(line => (line.top <= element.top && element.top <= line.bottom) ||
                                  (line.top <= element.bottom && element.bottom <= line.bottom) ||
                                  (element.top <= line.top && line.top <= element.bottom))
    line match {
      case None => (lines += new Line(element)).last
      case Some(line) => line.append(element)
    }
  }

  def asJSON: JSONArray = {
    new JSONArray(lines.sortBy(line => line.top).map(line => line.asJSON).toArray) 
  }

  def convert: String = {
    val pdf = PDDocument.load(file)

    var lastElement: Option[Element] = None
    new PDFTextStripper() {
      override protected def processTextPosition(text: TextPosition): Unit = {
        val element = Element(
          text.getCharacter(),
          text.getY(),
          text.getY() + text.getHeight(),
          text.getX(),
          text.getX() + text.getWidth()
        )
        if (!lastElement.isEmpty && lastElement.get.overlaps(element)) {
          lastElement.get.merge(element)
        }
        else {
          addToAppropriateLine(element)
          lastElement = Some(element)
        }
      }
    }.getText(pdf)

    pdf.close()

    asJSON.toString
  }
}

object PDFToJSON {
  def main(args: Array[String]): Unit = {
    args.toList match {
      case filename :: Nil => {
        val file = new File(filename.replaceFirst("^~", System.getProperty("user.home")))
        if (file.exists()) {
          println(new PDFToJSON(file).convert)
        }
        else {
          System.err.println("Specified file does not exist")
          System.exit(1)
        }
      }
      case _ => {
        System.err.println("Takes one parameter, name of PDF file to convert")
        System.exit(1)
      }
    }
  }
}
