# Pronto - dataflow scripting for Play

Pronto is a module for Play Framework 2.0.  It adds the ability to write interactive "scripts" inside your controllers.  The scripts can render HTML UI to the page and handle forms.  They are written in a compact, imperative style.  For example:

In your controller:

    object MyController extends Controller with ProntoConsoleHelper {
      def playAGame = ProntoAction {
        val form = Form("name" -> text, "age" -> age)
        val (name, age) = prompt(form) { htmlform { inputText(form("name")) + inputText(form("age")) } }
        println
      }
    }

and in your view:

    <div id="stdout"></div>
    <script src="@routes.Assets.at("javascripts/pronto.js")"></script>
    <scrip>startProntoWebSocket("stdout", "@routes.Application.wsHello");</script>

You can see a [full example](https://github.com/tksfz/pronto/tree/master/sample-app) in the repo.  Pronto leverages Play's existing libraries for WebSockets, iteratees, and especially Akka dataflow concurrency.

Pronto is geared towards quickly hacking small scripts when you don't want to fuss with multiple files and complicated UI.  Potential use cases include:  internal tools, devops tasks, bots (part of the inspiration comes from GitHub's Hubot).  It also is meant to demonstrate the flexibility and elegance of the Play framework.

## Installation & Usage

Pronto is a module for Play Framework 2.0.  That means you can add it as a library to use within your own Play application.  To see detailed installation instructions and usage see the [wiki](https://github.com/tksfz/pronto/wiki).

## Discussion

For now, please just use the [play mailing list](https://groups.google.com/forum/#!forum/play-framework).

## Status

This is an early release.  The API _will_ change.

## Contact

Please email pronto@tksfz.org if you have any questions.

