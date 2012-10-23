package org.tksfz

import scala.util.continuations.cps
import play.api.libs.iteratee._
import play.api.libs.concurrent.Akka
import akka.dispatch.{Future, Promise, MessageDispatcher}
import play.api.Application
import play.api.Logger
import play.api.mvc.{WebSocket, RequestHeader}
import play.api.libs.json.JsValue

package object pronto {
  type ProntoInlineScript = ProntoContext => Unit @cps[Future[Any]]
}

package pronto {
  case class ProntoContext(out: PushEnumerator[JsValue], in: FutureQueue[JsValue], dispatcher: MessageDispatcher)
  
  object ProntoWebSocket {    
    def apply(script: ProntoInlineScript)(implicit app: Application): WebSocket[JsValue] = apply(new ProntoInlineScriptWrapper(script))
  
    // TODO: [JsValue]
    def apply(script: ProntoRunnable)(implicit app: Application) = WebSocket.using[JsValue] { request =>
      // TODO: pass request along as well
      val futureQueue = new FutureQueue[JsValue]
      lazy val enumerator = Enumerator.imperative[JsValue](onStart = { script.run(context) })
      lazy val context: ProntoContext = ProntoContext(enumerator, futureQueue, Akka.system.dispatcher)
      val iteratee = Iteratee.foreach[JsValue] { event => futureQueue.put(event)(Akka.system.dispatcher) }
      (iteratee, enumerator)
    }
  }
  
  trait ProntoRunnable {
    def run(context: ProntoContext): Future[_]
  }

  trait ProntoScript extends ProntoRunnable with ProntoConsoleHelper {
    final override def run(context: ProntoContext) = {
      implicit val dispatcher: akka.dispatch.ExecutionContext = context.dispatcher
      Future.flow {
        script(context)
      } onFailure {
        case x: Exception =>
          println("stderr", "Exception while executing script: " + x.getStackTraceString)(context)
      }
    }
    
    protected def script(implicit context: ProntoContext): Unit @cps[Future[Any]]
  }
  
  private class ProntoInlineScriptWrapper(thescript: ProntoInlineScript) extends ProntoScript {
    override protected def script(implicit context: ProntoContext) = thescript(context)
  }
  
}



