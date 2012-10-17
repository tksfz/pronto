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
  
  val StdoutTarget: OutputTarget
  
  def println(target: OutputTarget, str: String): Unit
  
  def println(str: String): Any = { println(StdoutTarget, str); 5 }
  
  def read[A]: Future[A]
}

trait WebConsole extends ConsoleLike {
    
  type OutputTarget = String
  
  val StdoutTarget = "stdout"
    
  def out: PushEnumerator[String]
  
  def in: FutureQueue[String]
  
  implicit def executionContext: akka.dispatch.ExecutionContext
  
  def printTo(target: String, str: String) {
    val json = Json.toJson(Map(("target" )-> target, ("html" )-> str))
      out.push(Json.stringify(json))
  }

  def print(str: String) {
    printTo(StdoutTarget, str)
  }

  override def println(target: String, str: String) {
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
  
  def readForm[A](form: Form[A]): Future[A] = {
    // TOOD: errors especially parse errors should just become form errors that the user can re-do
    // rather than failing out the whole script
    read[String] map { x =>
      form.bind(FormUrlEncodedParser.parse(x, "utf-8").mapValues(_.headOption.getOrElse(""))).get
    }
  }
  
  /**
   * Prompt shows a form, waits for input, validates the result, re-shows with errors if necessary
   * until the input is valid
   * optional target
   */
  def promptTo[A](target: OutputTarget, form: Form[A])(html: Form[A] => Html): Future[A] = {
    printTo(target, html(form).toString)
    read[String] flatMap { formData =>
      val formResult = form.bind(FormUrlEncodedParser.parse(formData, "utf-8").mapValues(_.headOption.getOrElse("")))
      if (formResult.hasErrors || formResult.hasGlobalErrors) {
        // TODO: clear - replace div or just replace form content?
        promptTo(target, formResult)(html)
      } else {
        Future(formResult.get)
      }
    }
  }
  
  //def createWindow// ?
}

trait FormOutputter {
  //def formToHtml(form: Form[_]): Html
}

trait FormReader {
  
}

trait FormPrompter extends FormOutputter with FormReader

trait DefaultBootstrapFormPrompter extends FormPrompter {
  self: ConsoleLike =>
    
  import views.html.helper
  import views.html.helper.twitterBootstrap._
  
  def formToHtml2(form: Form[_]) = {
    views.html.helper.form(action = Call("GET", "#")) {
      null//form.mapping.mappings.map { mapping => mapping. }
    }
  }
  
  def form(body: Html) = helper.form(Call("GET", "#"), 'class -> "prontoForm") { body + Html(<input type="submit"/>.toString)}
  
  def inputText(field: play.api.data.Field, args: (Symbol, Any)*) = helper.inputText(field, args: _* )
  
  def tag(tagName: String, args: (Symbol, String)*)(body: Html) = {
    Html("<" + tagName + " " + argsToAttributes(args: _*) + ">") + body + Html("</" + tagName+ ">")
  }
  
  def div(args: (Symbol, String)*)(body: Html) = {
    tag("div", args: _*)(body)
  }
  
  def row(body: Html) = Html("<div class='row'>" + body.toString + "</div>")
  
  def span(args: (Symbol, String)*)(body: Html = Html("")): Html = {
    Html("<span " + argsToAttributes(args: _*) + ">") + body + Html("</span>")
  }
  
  def span_(args: (Symbol, String)*): Html = {
    span(args: _*)()
  }
  
  def argsToAttributes(args: (Symbol, String)*): String = {
    args.foldLeft("") { (str, arg) => str + " " + arg._1.name + "='" + arg._2 + "'"}
  }
  
  def printWindow(id: String) = {
    println("<div id='" + id + "' class='span6'></div>")
  }
}

trait TestScript extends AkkaProntoScript with WebConsole with DefaultBootstrapFormPrompter {
  
  override def script = {
    while(true) {
      println("hello world from the script")
      var x = null
      
      val form2 = Form(tuple("name" -> text, "age" -> number))
      val prontoForm = form {
        inputText(form2("name")) + inputText(form2("age"), '_showConstraints -> false)
      }
      println(prontoForm.toString)
      println("trying to read form")
      val (name, age) = readForm(form2)()
      println("<b>we</b> got name = " + name + " and age = " + age)
    }
  }
}

trait TestScript2 extends AkkaProntoScript with WebConsole with DefaultBootstrapFormPrompter {
  // we want auto-scrolling to bottom
  override def script = {
    print(div('class -> "container") { row {
      span('id -> "left", 'class -> "span4 box", 'style -> "height: 200px")() +
      span('id -> "right", 'class -> "span6 box", 'style -> "height: 200px; overflow: auto")()
    } }.toString)

    val form2 = Form(tuple("name" -> text, "age" -> number))
    val prontoForm = form {
      inputText(form2("name")) + inputText(form2("age"), '_showConstraints -> false)
    }
    //println("left", prontoForm.toString)
    println("right", "here are some instructions")
    promptTo("left", form2) { form3 =>
        val prontoForm = form {
          inputText(form3("name")) + inputText(form3("age"), '_showConstraints -> false)
        }
        Html(prontoForm.toString)
    }
    
    while(true) {
    val(name, age) = readForm(form2)()
    
    println("right", "<b>we</b> got name = " + name + " and age = " + age)
    }

    /*
    Future.flow {
      
    }
    Future.flow {
      
    } */
  }
}

// experimental stuff
sealed abstract class CustomOutputStyle
case class Sidebar(height: Int) extends CustomOutputStyle
case class North(width: Int) extends CustomOutputStyle
case class South(width: Int) extends CustomOutputStyle
// debug window?
// seems like debug msgs should be inline
// instead there should be separate windows that have their own interaction threads
// and certainly there should be asynchrony mechanisms that allow inter-communication among different threads / windows

sealed abstract class OutputTarget2
case object Stdout2 extends OutputTarget2
case object StdinPrompt2 extends OutputTarget2
case object Stderr2 extends OutputTarget2
case class Custom(name: String) extends OutputTarget2

sealed abstract class TextOutputTargets
case object Stdout extends TextOutputTargets
case object Stderr extends TextOutputTargets

class TextConsole {
  type OutputTarget = TextOutputTargets
}

