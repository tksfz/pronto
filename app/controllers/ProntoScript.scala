package controllers

import play.api.libs.iteratee._
import akka.dispatch.Future

trait ProntoScript {
  self: ConsoleLike =>

  // TODO: trickery to make the body of the class the script itself
  def run(): Any // Any allows CPS and then caller can handle the CPS etc call this script
  // or do an abstract type member and let run return that for a particular subtrait?
  
}

trait ConsoleLike {
  type OutputTarget
  
  val StdoutTarget: OutputTarget
  
  def println(target: OutputTarget, str: String): Unit
  
  def println(str: String): Any = { println(StdoutTarget, str); 5 }
  
  def read[A]: Future[A]
}

trait WebConsole extends ConsoleLike {
  self: ProntoScript =>
    
  type OutputTarget = String
  
  val StdoutTarget = "stdout"
    
  def out: PushEnumerator[String]
  
  def in: FutureQueue[String]
  
  implicit def ctx: akka.dispatch.ExecutionContext
    
  override def println(target: OutputTarget, str: String) {
    out.push(str)
  }
  
  override def read[A] = {
    val future = in.getNextFuture
    // convert string or whatever to A
    future map {
      x =>
        x.asInstanceOf[A]
    }
  }
  
  //def createWindow// ?
}

sealed abstract class TextOutputTargets
case object Stdout extends TextOutputTargets
case object Stderr extends TextOutputTargets

class TextConsole {
  type OutputTarget = TextOutputTargets
}

import akka.dispatch.Future

abstract class TestScript extends ProntoScript {
  self: WebConsole =>
    
  override def run = {
    Future.flow {
      while(true) {
        println("hello world from the script")
        val x = read[String]()
        println("script got " + x)
      }
    }
  }
}