package controllers

import akka.dispatch.Future
import scala.actors.threadpool.LinkedBlockingQueue
import akka.dispatch.Promise
import play.api.Logger

class FutureQueue[T] {
  
  private[this] val puts = new LinkedBlockingQueue[T]()
  
  private[this] val gets = new LinkedBlockingQueue[Promise[T]]()
  
  private[this] val lock = new Object

  def getNextFuture(implicit context: akka.dispatch.ExecutionContext): Future[T] = {
    lock.synchronized {
      var next = puts.poll
      if (next == null) {
        Logger.debug("saving get")
        val prom = Promise[T]()
        this.gets.put(prom)
        prom
      } else {
        Logger.debug("popping saved msg " + next)
        Promise.successful(next)
      }
    }
  }
  
  def put(msg: T)(implicit context: akka.dispatch.ExecutionContext) = {
    lock.synchronized {
      var waitingGet = gets.poll
      if (waitingGet != null) {
        Logger.debug("filling msg")
        Future.flow {
            waitingGet << msg
        }
      } else {
        Logger.debug("saving msg")
        puts.put(msg)
      }
    }
  }
}