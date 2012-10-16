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
  // Any allows CPS and then caller can handle the CPS etc call this script
  // or do an abstract type member and let run return that for a particular subtrait?
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
    
  override def println(target: OutputTarget, str: String) {
    val json = Json.toJson(Map("html" -> str))
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
  
  //def createWindow// ?
}

trait FormOutputter {
  //def formToHtml(form: Form[_]): Html
}

trait FormReader {
  
}

trait FormPrompter extends FormOutputter with FormReader

trait DefaultBootstrapFormPrompter extends FormPrompter {
  import views.html.helper
  import views.html.helper.twitterBootstrap._
  
  def formToHtml2(form: Form[_]) = {
    views.html.helper.form(action = Call("GET", "#")) {
      null//form.mapping.mappings.map { mapping => mapping. }
    }
  }
  
  def form(body: Html) = helper.form(Call("GET", "#"), 'class -> "prontoForm") { body + Html(<input type="submit"/>.toString)}
  
  def inputText(field: play.api.data.Field, args: (Symbol, Any)*) = helper.inputText(field, args: _* )
  
  def printWindow(id: String) = {
    println("<div id='" + id + "' class='span6'</div>")
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
  override def script = {
    
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

