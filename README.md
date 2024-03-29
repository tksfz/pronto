# Pronto - dataflow scripting for Play

Pronto is a module for Play Framework 2.0.  It adds the ability to write interactive "scripts" inside your controllers.  The scripts can render HTML UI to the page and handle forms.  They are written in a compact, imperative style.  Pronto uses WebSockets + Iteratees + Akka dataflow concurrency.  By combining these, we can write _non-blocking_ scripts in an imperative style.  For example:

In your controller:

    object MyController extends Controller with ProntoConsoleHelper with HtmlHelper {
      def wsNameAndAge = ProntoWebSocket { implicit context =>
        println("Enter your name and age:")
        val form = Form(tuple("name" -> text, "age" -> number))
        val (name, age) = prompt(form) { form => prontoform() { inputText(form("name")) + inputText(form("age")) + inputSubmit('value -> "Hit me!") } }(context)()
        println("name = " + name + ", age = " + age)
      }
    }

You can see a [full example](https://github.com/tksfz/pronto/tree/master/sample-app) in the repo and more documentation on the [wiki](https://github.com/tksfz/pronto/wiki).

Pronto is geared towards quickly hacking small scripts when you don't want to fuss with building a complex UI or MVC.  Potential use cases include:  internal tools, devops tasks, bots (part of the inspiration comes from GitHub's Hubot).  It also is meant to demonstrate the flexibility of the Play framework.

## Installation & Usage

Pronto is a module for Play Framework 2.0.  That means you can add it as a library to use within your own Play application.  To see detailed installation instructions and usage see the [wiki](https://github.com/tksfz/pronto/wiki).

## Discussion

For now, please just use the [play mailing list](https://groups.google.com/forum/#!forum/play-framework).

## Status

This is an early release to demonstrate the general idea.  There are still a lot of rough edges.  The API _will_ change.

## Contact

Please email pronto@tksfz.org if you have any questions or feedback.

