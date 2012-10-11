package controllers

import play.api._
import play.api.mvc._
import play.api.libs.iteratee._
import play.api.libs.concurrent.Akka
import play.api.Play.current

object Run extends Controller {
  
  def index = Action {
    Ok(views.html.run())
  }

  def ws = WebSocket.async[String] { request => Akka.future { 
    Logger.info("hey we got a connection")
    val enumerator = Enumerator.imperative[String]()
    Akka.future {
      Thread.sleep(3000)
      enumerator.push("hello")
    }
    val iteratee = Iteratee.foreach[String] {
      event =>
        Logger.info("we got " + event)
        enumerator.push(event)
    }
    (iteratee, enumerator)
    }
  }
}

