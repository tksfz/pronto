package controllers

import akka.dispatch.Future
import scala.actors.threadpool.LinkedBlockingQueue
import akka.dispatch.Promise

class FutureQueue[T] {
  
  private[this] val puts = new LinkedBlockingQueue[T]()
  
  private[this] val gets = new LinkedBlockingQueue[Promise[T]]()

  def getNextFuture(implicit context: akka.dispatch.ExecutionContext): Future[T] = {
    var next = puts.poll
    if (next == null) {
      val prom = Promise[T]()
      prom.future
    } else {
      Future(next)
    }
  }
  
  def put(msg: T)(implicit context: akka.dispatch.ExecutionContext) = {
    var waitingGet = gets.poll
    if (waitingGet != null) {
      Future.flow {
          waitingGet << msg
      }
    } else {
      puts.put(msg)
    }
  }
}