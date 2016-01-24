package com.walbrix.groupsession

import org.apache.http.client.methods.HttpGet

// http://build.mnode.org/projects/ical4j/apidocs/
import net.fortuna.ical4j.model.{Calendar,Property}
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.parameter.Value
import net.fortuna.ical4j.model.DateTime

class ICalServlet extends org.scalatra.ScalatraServlet {
  val apiRoot = Option(System.getenv("GROUPSESSION_API_ROOT")).getOrElse("http://localhost:8080/api")
  val whoami = apiRoot + "/user/whoami.do"
  val scheduleSearch = apiRoot + "/schedule/search.do" // usid=XXX
  val timezoneRegistry = net.fortuna.ical4j.model.TimeZoneRegistryFactory.getInstance.createRegistry;
  val tokyo = timezoneRegistry.getTimeZone("Asia/Tokyo")

  type Closable = { def close():Unit }
  def using[A <: Closable,B]( resource:A )( f:A => B ) = try(f(resource)) finally(resource.close)

  get("/") {
    "/LOGINID.ics?password=PASSWORD"
  }

  def createHttpClient(loginId:String, password:String) = {
    val credsProvider = new org.apache.http.impl.client.BasicCredentialsProvider();
    credsProvider.setCredentials(org.apache.http.auth.AuthScope.ANY, new org.apache.http.auth.UsernamePasswordCredentials(loginId, password))
    org.apache.http.impl.client.HttpClients.custom.setDefaultCredentialsProvider(credsProvider).build
  }

  def getContentAsXML(response:org.apache.http.HttpResponse):scala.xml.Elem = {
    val code = response.getStatusLine.getStatusCode
    if (code != 200) halt(code, "%d (API returned)".format(code))
    val entity = response.getEntity
    val contentType = entity.getContentType.getValue
    if (contentType != "text/xml" && !contentType.startsWith("text/xml;")) halt(500, "500 Internal Server Error: API returned unknown contentType")
    using(entity.getContent) { is =>
      scala.xml.XML.load(is)
    }
  }

  def getSchedule(loginId:String, password:String):Seq[(Int,String,String,String)] = {
    using(createHttpClient(loginId, password)) { httpClient =>
      val usid = using(httpClient.execute(new HttpGet(whoami))) { response =>
        (getContentAsXML(response) \ "Result" \ "Usid").text.toInt
      }
      using(httpClient.execute(new HttpGet(scheduleSearch + "?usid=%d&results=100".format(usid)))) { response =>
        // <ResultSet url="/api/schedule/search.do" Start="1" TotalCount="1"><Result>...
        // 終日イベントは0時0分から23時59分までとなる
        (getContentAsXML(response) \ "Result").map { elem=>
          (
            (elem \ "Schsid").text.toInt,
            (elem \ "Title").text,
            (elem \ "StartDateTime").text,
            (elem \ "EndDateTime").text
          )
        }
      }
    }
  }


  def getICal(loginId:String, password:String):Any = {
    val schedule = getSchedule(loginId, password)
    val calendar = new net.fortuna.ical4j.model.Calendar
    val components = calendar.getComponents

    components.add(tokyo.getVTimeZone);

    schedule.foreach { item =>
      val event = new VEvent(new DateTime(item._3, "yyyy/MM/dd HH:mm:ss", tokyo), new DateTime(item._4, "yyyy/MM/dd HH:mm:ss", tokyo), item._2)
      components.add(event)
    }

    calendar.toString
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
