Pronto - dataflow scripting for Play
=====================================

Pronto is a module for Play Framework 2.0.  That means you can add it as a library to use within your own Play application.
It adds the ability to write small, interactive "scripts" inside your controllers.  The scripts can
include UI, interactive prompts, and multi-step/page wizards all written in an imperative style, MVC be damned :)
E.g.:

code:

object MyController extends Controller with ProntoScript {
  def playAGame = ProntoAction {
    val form = Form("name" -> text, "age" -> age)
    val (name, age) = prompt(form) { htmlform { inputText(form("name")) + inputText(form("age")) } }
    println
  }
}

and in a view:

@prontotoplayagame

(of course you also need to define a route)

(Actually I love MVC, this is for an entirely special use case.)

Pronto makes use of Play's existing libraries for WebSockets, Actors, and especially Akka dataflow concurrency.

This is similar to the approach of continuations-based frameworks like Seaside and indeed we could grow it
into a framework within a framework.  for blog:  I like the idea of having a single framework (i.e. Play) that
spans multiple paradigms.  Play already supports both MVC as well as a reactive style of programming (a la node.js),
which shows the power and completeness of the framework as well as the Scala language - it is truly a "scalable" framework.

This is not meant to be a full-fledged "framework within a framework", but meant to help one particular use case: rapid development of
little scripts and other quick hacks, when you don't want to necessarily fuss with a lot of complicated UI.

This _is_ meant to be used like a "bot" and part of the inspiration is GitHub's Hubot.

Installation

Use it with Bootstrap

Of course, everything will generally look better if you use Bootstrap, without you having to do much at all.  See

I ended up writing something somewhat more general.
