package com.walbrix.groupsession

// http://build.mnode.org/projects/ical4j/apidocs/
import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.component.VEvent

class ICalServlet extends org.scalatra.ScalatraServlet {
  val apiRoot = Option(System.getenv("GROUPSESSION_API_ROOT")).getOrElse("http://localhost:8080/api")

  get("/") {
    "/LOGINID.ics?password=PASSWORD"
  }

  def getICal(loginId:String, password:String):Any = {
    println(loginId)
    println(password)
    "Hello, World!"
  }

  get("/:loginId.txt") {
    contentType = "text/plain"
    getICal(params("loginId"), params.get("password").getOrElse(halt(403, "403 Forbidden: password not provided")))
  }

  get("/:loginId.ics") {
    contentType = "text/calendar"
    getICal(params("loginId"), params.get("password").getOrElse(halt(403, "403 Forbidden: password not provided")))
  }
}
