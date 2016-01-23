class ScalatraBootstrap extends org.scalatra.LifeCycle {
  override def init(context: javax.servlet.ServletContext) {
    context.mount(classOf[com.walbrix.groupsession.ICalServlet], "/*")
  }
}
