package controllers

import play.api._
import play.api.mvc._
import play.api.libs.iteratee._
import play.api.libs.concurrent.Akka
import play.api.Play.current
import akka.actor._
import akka.pattern.ask
import play.api.data._
import play.api.data.Forms._
import akka.dispatch.Future

object ProntoWebSocket {
  
  import scala.util.continuations._
  // [JsValue] perhaps?
  def apply(script: ProntoContext => Unit @cps[Future[Any]]) = WebSocket.using[String] { request =>
    implicit val ctx = Akka.system.dispatcher
    val futureQueue = new FutureQueue[String]
    lazy val enumerator: PushEnumerator[String] = Enumerator.imperative[String](
        onStart = {
          Future.flow { script(context) }
        })
    lazy val context = ProntoContext(enumerator, futureQueue, Akka.system.dispatcher)
    val iteratee = Iteratee.foreach[String] { event => futureQueue.put(event)(Akka.system.dispatcher) }
    (iteratee, enumerator)
  }
  
}


case class Start(script: ProntoScript)
case class Message(msg: String)
case class Connected(out: PushEnumerator[String])
case class Start2(script: ProntoContext => Unit)

object RunActor {
  import akka.actor._
  import akka.pattern.ask
      
  import akka.util.duration._
  import play.api.libs.concurrent._
  implicit val timeout = akka.util.Timeout(1 second)
  
  def run(script: ProntoScript) = {
    val actor = Akka.system.actorOf(Props[RunActor])
    (actor ? Start(script)).asPromise map {
      case Connected(out) =>
        val iteratee = Iteratee.foreach[String] {
          event =>
            actor ! Message(event)
        }
        (iteratee, out)
    }
  } 
}

class RunActor extends Actor {
  private[this] var out: PushEnumerator[String] = _
  
  private[this] var script: ProntoScript = _
  
  private[this] val futureQueue = new FutureQueue[String]
  
  private var first = true
  
  override def receive = {
    case Start2(thescript) => {
      val enumerator = Enumerator.imperative[String](
          onStart = { /* thescript.run() */ })
      val actualscript = new AkkaProntoScript with WebConsole {
        override val out = enumerator
        override val in = futureQueue
        override val executionContext = context.dispatcher
        
        override def script() = {
          val context = ProntoContext(out, in, executionContext)
          thescript(context)
        }
      }
    }
    case Start(script) => {
      val enumerator = Enumerator.imperative[String](
          onStart = { script.run() })
      this.out = enumerator
      this.script = new TestScript2 with WebConsole {
        override val out = enumerator
        override val in = futureQueue
        override val executionContext = context.dispatcher
      }
      sender ! Connected(enumerator)
    }
    case Message(msg) => {
      this.futureQueue.put(msg)(context.dispatcher)
    }
  }
}