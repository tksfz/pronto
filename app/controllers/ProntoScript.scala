package controllers

import play.api.libs.iteratee._
import akka.dispatch.Future
import akka.dispatch.Future
import play.api.data._
import play.api.data.Forms._
import play.core.parsers.FormUrlEncodedParser
import play.api.Logger
import scala.util.continuations.cps
import play.api.templates.Html
import play.api.mvc.Call
import play.api.libs.json.Json
import akka.dispatch.Promise
import play.api.templates.HtmlFormat

trait ProntoScript {
  self: ConsoleLike =>
    
  // TODO: trickery to make the body of the class the script itself
  def run() 
}

trait AkkaProntoScript extends ProntoScript {
  self: ConsoleLike =>
    
  implicit def executionContext: akka.dispatch.ExecutionContext
    
  def run() = {
    Future.flow {
      script()
    } onFailure {
      case x: Exception =>
        Logger.error("outer error", x)
        println(x.getStackTraceString)
    }
  }
    
  def script(): Any @cps[Future[Any]] 
} 

trait ConsoleLike {
  type OutputTarget
  
  val Stdout: OutputTarget
  
  def println(target: OutputTarget, str: String): Unit
  
  def println(str: String): Any = { println(Stdout, str); 5 }
  
  def read[A]: Future[A]
}

trait WebConsole extends ConsoleLike {
    
  type OutputTarget = String
  
  override val Stdout = "stdout"
    
  def out: PushEnumerator[String]
  
  def in: FutureQueue[String]
  
  implicit def executionContext: akka.dispatch.ExecutionContext
  
  def printTo(target: String, str: String) {
    val json = Json.toJson(Map(("target" )-> target, ("html" )-> str))
      out.push(Json.stringify(json))
  }

  def print(str: String) {
    printTo(Stdout, str)
  }
  
  def print(html: Html) {
    print(Stdout, html)
  }
  
  override def println(str: String) {
    println(Stdout, str)
  }

  override def println(target: String, str: String) {
    println2(target, str)
  }
  
  def println2(target: String, str: String) {
    val html = HtmlFormat.escape(str)
    println(target, html)
  }
  
  def println(target: String, html: Html) {
    print(target, html + Html("<br/>"))
  }
  
  def print(target: String, html: Html) {
    printraw(target, html.toString)
  }
  
  def printraw(target: String, str: String) {
    val json = Json.toJson(Map("target" -> target, "html" -> str))
    out.push(Json.stringify(json))
  }
  
  override def read[A] = {
    val future = in.getNextFuture
    // convert string or whatever to A
    future map {
      x =>
        x.asInstanceOf[A]
    }
  }
  
  // TODO: just as we have multiple output channels identified by div id
  // we should have multiple input channels identified by form id
  // could we also use iteratees there?
  def readForm[A](form: Form[A]): Future[A] = {
    // TOOD: errors especially parse errors should just become form errors that the user can re-do
    // rather than failing out the whole script
    read[String] map { socketMessage =>
      val x = getFormData(socketMessage)
      form.bind(FormUrlEncodedParser.parse(x, "utf-8").mapValues(_.headOption.getOrElse(""))).get
    }
  }
  
  /**
   * This is the equivalent to something like a "readLine()" call - just wait for the user
   * to hit Enter or in this case click a button.
   */
  def readClick(/*elementId: String*/): Future[Unit] = {
    // TODO: make this read[JsValue] using the implicit formatter nice
    read[String] flatMap { socketMessage =>
      val socketMsgJson = Json.parse(socketMessage)
      if ((socketMsgJson \ "event").as[String] == "click") {
        Promise.successful(())
      } else {
        readClick()
      }
    }
  }
  
  /**
   * Prompt shows a form, waits for input, validates the result, re-shows with errors if necessary
   * until the input is valid
   * optional target
   */
  def promptTo[A](target: OutputTarget, form: Form[A])(html: Form[A] => Html): Future[A] = {
    printTo(target, html(form).toString)
    read[String] flatMap { socketMessage =>
      Logger.info(socketMessage)
      val socketMessageJson = Json.parse(socketMessage)
      val formData = (socketMessageJson \ "data").as[String]
      Logger.info(formData)
      val formResult = form.bind(FormUrlEncodedParser.parse(formData, "utf-8").mapValues(_.headOption.getOrElse("")))
      if (formResult.hasErrors || formResult.hasGlobalErrors) {
        // TODO: clear - replace div or just replace form content?
        promptTo(target, formResult)(html)
      } else {
        Future(formResult.get)
      }
    }
  }
  
  private[this] def getFormData(socketMessage: String) = {
    val socketMessageJson = Json.parse(socketMessage)
    val formData = (socketMessageJson \ "data").as[String]
    formData
  }
}

/**
 * Helper methods to generate HTML from code.  Scripts won't generally use views.  Instead
 * they'll use an imperative style where they simply print HTML.
 * 
 * These methods all return Play's Html type.  We also re-use some existing html helpers provided
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
  
  def form(args: (Symbol, String)*)(body: Html) = helper.form(Call("GET", "#"), args: _*) { body + Html(<input type="submit"/>.toString)}
  
  def inputText(field: play.api.data.Field, args: (Symbol, Any)*) = helper.inputText(field, args: _* )
  
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
  
}

trait TestScript extends AkkaProntoScript with WebConsole with HtmlHelper {
  
  override def script = {
    while(true) {
      println("hello world from the script")
      var x = null
      
      val form2 = Form(tuple("name" -> text, "age" -> number))
      val prontoForm = form('id -> "myform") {
        inputText(form2("name")) + inputText(form2("age"), '_showConstraints -> false)
      }
      println(prontoForm.toString)
      println("trying to read form")
      val (name, age) = readForm(form2)()
      println("<b>we</b> got name = " + name + " and age = " + age)
    }
  }
}

trait TestScript2 extends AkkaProntoScript with WebConsole with BootstrapHtmlHelper {
  // we want auto-scrolling to bottom
  override def script = {
    print(div('class -> "container") { row {
      span('id -> "left", 'class -> "span4 box", 'style -> "height: 200px")() +
      span('id -> "right", 'class -> "span6 box", 'style -> "height: 200px; overflow: auto")()
    } })

    val form2 = Form(tuple("name" -> text, "age" -> number))
    
    println("right", "here are some instructions")
    val (name, age) = promptTo("left", form2) { form3 =>
        prontoform() {
          inputText(form3("name")) + inputText(form3("age"), '_showConstraints -> false)
        }
    }()
    
    println("right", Html("<b>we</b> got name = ") + htmlescape(name) + Html(" and age = " + age))
    println("right", "to continue click the button:")
    println("right", prontobutton() { Html("Hit Me!") })
   
    // We need to distinguish buttons using both readClick("thisbutton") or whichButton = readClick
    readClick()()
    
    println("right", "alright now we're rolling")
  }
}

trait SimplePoll extends AkkaProntoScript with WebConsole with BootstrapHtmlHelper {
  override def script = {
    
  }
}
