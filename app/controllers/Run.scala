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
      (actor ? Start()).asPromise map {
        case Connected(out) =>
          val iteratee = Iteratee.foreach[String] {
            event =>
              actor ! Message(event)
          }
          (iteratee, out)
      }
  }
}

import akka.actor._
import akka.pattern.ask

case class Start()
case class Message(msg: String)
case class Connected(out: PushEnumerator[String]) 

object RunActor {
  lazy val default = {
    val runActor = Akka.system.actorOf(Props[RunActor])
    runActor
  }
  
  def join(out: PushEnumerator[String]) = {
    default ! Start()
    //default ? Start(out)
    val iteratee = Iteratee.foreach[String] {
      event =>
        default ! Message(event)
    }
    (iteratee, out)
  } 
}

class RunActor extends Actor {
  private[this] var out: PushEnumerator[String] = _
  
  private[this] var script: ProntoScript = _
  
  private[this] val futureQueue = new FutureQueue[String]
  
  private var first = true
  
  override def receive = {
    case Start() => {
      val enumerator = Enumerator.imperative[String](
          onStart = { this.script.run() })
      this.out = enumerator
      this.script = new TestScript with WebConsole {
        override val out = enumerator
        override val in = futureQueue
        override val executionContext = context.dispatcher
      }
      sender ! Connected(enumerator)
    }
    case Message(msg) => {
      this.out.push(msg)
      this.futureQueue.put(msg)(context.dispatcher)
    }
  }
}