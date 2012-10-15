package controllers

import akka.dispatch.Future
import scala.actors.threadpool.LinkedBlockingQueue
import akka.dispatch.Promise

class FutureQueue[T] {
  
  private[this] val puts = new LinkedBlockingQueue[T]()
  
  private[this] val gets = new LinkedBlockingQueue[Promise[T]]()
  
  private[this] val lock = new Object

  def getNextFuture(implicit context: akka.dispatch.ExecutionContext): Future[T] = {
    lock.synchronized {
      var next = puts.poll
      if (next == null) {
        val prom = Promise[T]()
        this.gets.put(prom)
        prom
      } else {
        Promise.successful(next)
      }
    }
  }
  
  def put(msg: T)(implicit context: akka.dispatch.ExecutionContext) = {
    lock.synchronized {
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
}