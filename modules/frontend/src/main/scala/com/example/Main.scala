package com.example.frontend

import scala.scalajs.js
import scala.scalajs.js.annotation._

import org.scalajs.dom

object Main {
// import javascriptLogo from "/javascript.svg"
  @js.native @JSImport("/javascript.svg", JSImport.Default)
  val javascriptLogo: String = js.native

  def main(args: Array[String]): Unit = {
    dom.document.querySelector("#app").innerHTML = s"""
    <div>
      <a href="https://vitejs.dev" target="_blank">
        <img src="/images/vite.svg" class="logo" alt="Vite logo" />
      </a>
      <a href="https://developer.mozilla.org/en-US/docs/Web/JavaScript" target="_blank">
        <img src="$javascriptLogo" class="logo vanilla" alt="JavaScript logo" />
      </a>
      <h1>Hello Scala.js and Vite!</h1>
      <div class="card">
        <button id="counter" type="button"></button>
      </div>
      <p class="read-the-docs">
        Click on the Vite logo to learn more
      </p>
    </div>
  """

    setupCounter(dom.document.getElementById("counter"))
  }

  def setupCounter(element: dom.Element): Unit = {
    var counter = 0

    def setCounter(count: Int): Unit = {
      counter = count
      element.innerHTML = s"count is $counter"
    }

    element.addEventListener(
      "click",
      (e: dom.Event) => setCounter(counter + 1)
    )
    setCounter(0)
  }
}
