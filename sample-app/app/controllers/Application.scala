package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

object Application extends Controller with ProntoScriptApi with HtmlHelper {
  
  def index = Action {
    Ok(views.html.run())
  }
  
  // TODO: add a variant where the script can be initiated with some request args
  // coming from some initiating form
  // this would be the way many would expect to "kick off" a bit of script:
  //  with something like command-line arguments
  // we should also support these as method args, and allow the script to throw up
  // an error that plays well with the usual REST-style handlers
  def wsActor2 = ProntoWebSocket { implicit context =>
    println("hello")
    val form = Form(tuple("name" -> text, "age" -> number))
    val (name, age) = prompt(form) { form => prontoform() { inputText(form("name")) + inputText(form("age")) + inputSubmit('value -> "Hit me!") } }(context)()
    println("name = " + name + ", age = " + age)
  }
  
}