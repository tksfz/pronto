# Pronto - dataflow scripting for Play

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

    <div id="stdout"></div>
    <script src="@routes.Assets.at("javascripts/pronto.js")"></script>
    <scrip>startProntoWebSocket("stdout", "@routes.Application.wsHello");</script>

(of course you also need to define a route)

I love MVC, and Pronto is geared towarsd a particular use case, scripting.

Pronto makes use of Play's existing libraries for WebSockets, Actors, and especially Akka dataflow concurrency.

but meant to help one particular use case: rapid development of
little scripts and other quick hacks, when you don't want to necessarily fuss with a lot of complicated UI.  Other potential use cases include:  bots (part of the inspiration comes from GitHub's Hubot).

## Installation & Usage

Pronto is a module for Play Framework 2.0.  That means you can add it as a library to use within your own Play application.  To see detailed installation instructions and usage see the Wiki.

## Example

There is a more fleshed-out example

## Mailing List

Just use the play mailing list?

## Contact
