package org.tksfz.pronto

import play.api.templates.{Html, HtmlFormat}
import scala.util.Random
import play.api.data.Form
import play.api.Logger
import akka.dispatch.{Future, Promise}
import play.api.libs.json.Json
import play.core.parsers.FormUrlEncodedParser
import play.api.libs.json.JsValue

trait ProntoConsoleHelper {    
  def print(str: String)(implicit context: ProntoContext) {
    print("stdout", HtmlFormat.escape(str))
  }
  
  def print(html: Html)(implicit context: ProntoContext) {
    print("stdout", html)
  }
  
  def println(str: String)(implicit context: ProntoContext) {
    println("stdout", str)
  }

  def println(target: String, str: String)(implicit context: ProntoContext) {
    println(target, HtmlFormat.escape(str))
  }
  
  def println(target: String, html: Html)(implicit context: ProntoContext) {
    print(target, html + Html("<br/>"))
  }
  
  def print(target: String, html: Html)(implicit context: ProntoContext) {
    printraw(target, html.toString)
  }
  
  def printReplace(target: String, html: Html)(implicit context: ProntoContext) {
    printrawreplace(target, html.toString)
  }
  
  def clear(target: String = "stdout")(implicit context: ProntoContext) {
    printReplace(target, Html(""))
  }
  
  private[this] def printraw(target: String, str: String)(implicit context: ProntoContext) {
    // TODO: if target is default let client-side choose stdout or something
    val json = Json.toJson(Map("target" -> target, "html" -> str))
    context.out.push(json)
  }
  
  private[this] def printrawreplace(target: String, str: String)(implicit context: ProntoContext) {
    val json = Json.toJson(Map("target" -> target, "method" -> "replace", "html" -> str))
    context.out.push(json)
  }
  
  private[this] def read(implicit context: ProntoContext) = {
    context.in.getNextFuture(context.dispatcher)
  }
  
  // TODO: just as we have multiple output channels identified by div id
  // we should have multiple input channels identified by form id
  // could we also use iteratees there?
  def readForm[A](form: Form[A])(implicit context: ProntoContext): Future[A] = {
    // TOOD: errors especially parse errors should just become form errors that the user can re-do
    // rather than failing out the whole script
    read map { socketMessage =>
      val x = getFormData(socketMessage)
      form.bind(FormUrlEncodedParser.parse(x, "utf-8").mapValues(_.headOption.getOrElse(""))).get
    }
  }
  
  /**
   * This is the equivalent to something like a "readLine()" call - just wait for the user
   * to hit Enter or in this case click a button.
   */
  // TODO: provide pre-apply'ed versions readClick()()
  def readClick(/*elementId: String*/)(implicit context: ProntoContext): Future[Unit] = {
    // TODO: make this read[JsValue] using the implicit formatter nice
    read flatMap { socketMessage =>
      if ((socketMessage \ "event").as[String] == "click") {
        Promise.successful(())(context.dispatcher)
      } else {
        readClick()
      }
    }
  }
  
  /**
   * Prompt shows a form, waits for input, validates the result, re-shows with errors if necessary
   * until the input is valid
   * optional target
   * 
   * we need a simpler version of this for non-validating cases
   */
  def promptTo[A](target: String, form: Form[A])(html: Form[A] => Html)(implicit context: ProntoContext): Future[A] = {
    def promptToHelper(target: String, form: Form[A])(html: Form[A] => Html): Future[A] = {
      printReplace(target, html(form))
      read flatMap { socketMessage =>
        val formData = (socketMessage \ "data").as[String]
        Logger.info(formData)
        val formResult = form.bind(FormUrlEncodedParser.parse(formData, "utf-8").mapValues(_.headOption.getOrElse("")))
        if (formResult.hasErrors || formResult.hasGlobalErrors) {
          promptToHelper(target, formResult)(html)
        } else {
          // if the old form had errors and the new one doesn't then we want to clear the errors on screen
          // alternatively, we could compare the new and old html
          if (form.hasErrors || form.hasGlobalErrors) {
            printReplace(target, html(formResult))
          }
          Promise.successful(formResult.get)(context.dispatcher)
        }
      }
    }
    
    // This allows us to re-prompt without the client-side having to compute
    // innerHTML of the form html we give it
    val containerId = "formcontainer_" + Random.nextInt(Int.MaxValue)
    print(target, Html("<div id='" + containerId + "'></div>"))
    promptToHelper(containerId, form)(html)
  }
  
  def prompt[A](form: Form[A])(html: Form[A] => Html)(implicit context: ProntoContext): Future[A] = {
    promptTo("stdout", form)(html)
  }
  
  private[this] def getFormData(socketMessage: JsValue) = {
    val formData = (socketMessage \ "data").as[String]
    formData
  }

}
