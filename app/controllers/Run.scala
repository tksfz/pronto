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

  import akka.actor._
  import akka.pattern.ask
      
  import akka.util.duration._
  import play.api.libs.concurrent._
  implicit val timeout = akka.util.Timeout(1 second)
  
  def wsActor = WebSocket.async[String] {
    request =>
      val actor = Akka.system.actorOf(Props[RunActor])
      val enumerator = Enumerator.imperative[String]()
      (actor ? Start(enumerator)).asPromise map {
        case Connected(out) =>
          val iteratee = Iteratee.foreach[String] {
            event =>
              actor ! Message(event)
          }
          (iteratee, enumerator)
      }
  }
}

import akka.actor._
import akka.pattern.ask

case class Start(out: PushEnumerator[String])
case class Message(msg: String)
case class Connected(out: PushEnumerator[String]) 

object RunActor {
  lazy val default = {
    val runActor = Akka.system.actorOf(Props[RunActor])
    runActor
  }
  
  def join(out: PushEnumerator[String]) = {
    default ! Start(out)
    //default ? Start(out)
    val iteratee = Iteratee.foreach[String] {
      event =>
        default ! Message(event)
    }
    (iteratee, out)
  } 
}

class RunActor extends Actor {
  var out: PushEnumerator[String] = _
  
  override def receive = {
    case Start(out) => this.out = out
    case Message(msg) => {
      this.out.push(msg)
    }
  }
}