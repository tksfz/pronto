package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import org.tksfz.pronto._
import play.api.templates.Html

object Application extends Controller with ProntoConsoleHelper with HtmlHelper with BootstrapHtmlHelper {
  
  def index = Action { implicit request =>
    Ok(views.html.run())
  }
  
  import play.api.Play.current
  
  // TODO: add a variant where the script can be initiated with some request args
  // coming from some initiating form
  // this would be the way many would expect to "kick off" a bit of script:
  //  with something like command-line arguments
  // we should also support these as method args, and allow the script to throw up
  // an error that plays well with the usual REST-style handlers
  def ws = ProntoWebSocket { implicit context =>
    println("Enter your name and age:")
    val form = Form(tuple("name" -> text, "age" -> number))
    val (name, age) = prompt(form) { form => prontoform() { inputText(form("name")) + inputText(form("age")) + inputSubmit('value -> "Hit me!") } }(context)()
    println("name = " + name + ", age = " + age)
  }
  
  def ws2 = ProntoWebSocket { implicit context =>
    // too much html code here no?
    print(div('class -> "container") { row {
      span('id -> "left", 'class -> "span4 box", 'style -> "height: 200px")() +
      span('id -> "right", 'class -> "span6 box", 'style -> "height: 200px; overflow: auto")()
    } })

    val form = Form(tuple("name" -> text, "age" -> number))
    
    println("right", "here are some instructions")
    val (name, age) = promptTo("left", form) { form =>
        prontoform() {
          inputText(form("name")) + inputText(form("age"), '_showConstraints -> false) +
          inputSubmit('value -> "Submit Yo", 'class -> "btn")
        }
    }(context)()
    
    println("right", Html("<b>we</b> got name = ") + htmlescape(name) + Html(" and age = " + age))
    println("right", "to continue click the button:")
    println("right", prontobutton() { Html("Hit Me!") })
   
    // We need to distinguish buttons using both readClick("thisbutton") or whichButton = readClick
    readClick()(context)()
    
    println("right", "alright now we're rolling")
  }
  
  // TODO: simple poll example
  
}