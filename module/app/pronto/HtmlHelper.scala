package org.tksfz.pronto

import play.api.data._
import play.api.data.Forms._
import play.api.templates.Html
import play.api.mvc.Call
import play.api.templates.HtmlFormat

/**
 * Helper methods to generate HTML from code.  Scripts won't typically use views.  Instead
 * they'll use an imperative style where they simply print HTML.
 * 
 * These methods all return Play's Html type.  We also incorporate some existing html helpers provided
 * by Play.
 * 
 * prontoform
 * prontobutton
 * 
 * By default, forms and buttons automatically have markup added that tells Pronto to propagate
 * submit and click events, respectively, back to the server.  To suppress this behavior use the
 * alternate forms "plainform" and "plainbutton"
 * 
 * prontoanchor
 */
trait HtmlHelper {
    
  import views.html.helper
  
  def form(args: (Symbol, String)*)(body: Html) = {
    helper.form(Call("GET", "#"), args: _*) {
      body
    }
  }
  
  def inputText(field: play.api.data.Field, args: (Symbol, Any)*) = helper.inputText(field, args: _* )
  
  def inputSubmit(args: (Symbol, String)*): Html = {
    tag("input", (args :+ 'type -> "submit"): _*)()
  }
  
  private[this] def tag(tagName: String, args: (Symbol, String)*)(body: Html = Html("")) = {
    Html("<" + tagName + " " + argsToAttributes(args: _*) + ">") + body + Html("</" + tagName+ ">")
  }
  
  def div(args: (Symbol, String)*)(body: Html) = {
    tag("div", args: _*)(body)
  }
  
  def span(args: (Symbol, String)*)(body: Html = Html("")): Html = {
    Html("<span " + argsToAttributes(args: _*) + ">") + body + Html("</span>")
  }
  
  def span_(args: (Symbol, String)*): Html = {
    span(args: _*)()
  }
  
  def argsToAttributes(args: (Symbol, String)*): String = {
    args.foldLeft("") { (str, arg) => str + " " + arg._1.name + "='" + arg._2 + "'"}
  }
  
  def button(args: (Symbol, String)*)(html: Html) = {
    tag("button", args: _*)(html)
  }
  
  val PRONTO_CLASS = "prontoInput"
  
  def prontoform(args: (Symbol, String)*)(body: Html): Html = {
    form(addProntoClass(args): _*)(body)
  }
  
  private[this] def addProntoClass(args: Seq[(Symbol, String)]) = {
    var needsClass = true
    val newargs = args map { elem =>
      if (elem._1 == 'class) {
        needsClass = false
        (elem._1 -> (PRONTO_CLASS + " " + elem._2))
      } else
        elem
    }
    if (needsClass) newargs :+ ('class -> PRONTO_CLASS) else newargs
  }
  
  def prontobutton(args: (Symbol, String)*)(body: Html): Html = {
    button(addProntoClass(args): _*)(body)
  }
  
  def htmlescape(str: String): Html = {
    HtmlFormat.escape(str)
  }
  
}

trait BootstrapHtmlHelper extends HtmlHelper {
  def row(body: Html) = Html("<div class='row'>" + body.toString + "</div>")
  
  import views.html.helper.twitterBootstrap._

  import views.html.helper
  
  // use bootstrap typeclass
  override def inputText(field: play.api.data.Field, args: (Symbol, Any)*) = helper.inputText(field, args: _* )
  
  // override some methods to attach standard bootstrap classes like btn etc.
}