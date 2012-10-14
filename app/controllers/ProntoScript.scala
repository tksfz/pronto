package controllers

import play.api.libs.iteratee._
import play.api.libs.concurrent._
import akka.dispatch.Future

trait ProntoScript {
  self: ConsoleLike =>

  // TODO: trickery to make the body of the class the script itself
  def run()
  
}

trait ConsoleLike {
  type OutputTarget
  
  val StdoutTarget: OutputTarget
  
  def println(target: OutputTarget, str: String): Unit
  
  def println(str: String): Any = { println(StdoutTarget, str); 5 }
  
  def read[A]: Future[A]
}

trait WebConsole extends ConsoleLike {
  type OutputTarget = String
  
  val StdoutTarget = "stdout"
    
  def out: PushEnumerator[String]
  
  def in: FutureQueue[String]
  
  implicit def ctx: akka.dispatch.ExecutionContext
    
  override def println(target: OutputTarget, str: String) {
    out.push(str)
  }
  
  def read[A] = {
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

class TestScript extends ProntoScript {
  self: ConsoleLike =>
    
  def run {
    println("hello world from the script")
  }
}