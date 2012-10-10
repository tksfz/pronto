package controllers

import play.api._
import play.api.mvc._
import play.api.libs.iteratee._

object Run extends Controller {
  
  def index = Action {
    Ok(views.html.run())
  }

  def ws = WebSocket.using[String] { request =>
    Logger.info("hey we got a connection")
    val in = Iteratee.consume[String]()
    val out = Enumerator("hello")
    (in, out)
  }
}