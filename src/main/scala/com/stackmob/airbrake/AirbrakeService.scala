package com.stackmob.airbrake

import java.io.OutputStreamWriter
import java.net.{URL, HttpURLConnection}
import xml.NodeSeq
import util.Random
import scalaz._
import scalaz.concurrent._
import scalaz.effects._
import Scalaz._

abstract class AirbrakeService (actorPoolSize: Int = Runtime.getRuntime.availableProcessors) {

  private val actorList = Vector.fill(actorPoolSize)(airbrakeActor)
  private val random = new Random

  def getApiKey: String
  def getEnvironment: String
  def isSecure: Boolean = true

  def notifyAsync(notice: AirbrakeNotice) {
    submitAsync(() => notify(notice).unsafePerformIO)
  }

  def notifySync(notice: AirbrakeNotice): Validation[Throwable, Int] = {
    notify(notice).unsafePerformIO
  }

  def notify(notice: AirbrakeNotice): IO[Validation[Throwable, Int]] = {
    notify(prepareRequest(notice))
  }

  def notify(xml: NodeSeq): IO[Validation[Throwable, Int]] = {
    sendNotification(xml).map(_.success[Throwable]).except(_.fail[Int].pure[IO])
  }

  def prepareRequest(notice: AirbrakeNotice): NodeSeq = {
    <notice version="2.2">
      <api-key>{getApiKey}</api-key>
      <notifier>
        <name>StackMob Scala Notifier</name>
        <version>0.1.0</version>
        <url>http://stackmob.com</url>
      </notifier>
      <error>
        <class>{notice.throwable.getClass.getName}</class>
        <message>{notice.throwable.getMessage}</message>
        <backtrace>
          {formatStacktrace(notice.throwable.getStackTrace)}
        </backtrace>
      </error>
      {(for (u <- notice.url) yield
      <request>
        <url>{u.toString}</url>
        {formatParams(notice.params)}
        <component>{~notice.component}</component>
      </request>
      ).orNull}
      <server-environment>
        <environment-name>{getEnvironment}</environment-name>
      </server-environment>
    </notice>
  }

  protected def formatStacktrace(traceElements: Array[StackTraceElement]): Array[NodeSeq] = {
    traceElements map { t =>
      <line method={t.getMethodName} file={t.getFileName} number={t.getLineNumber.toString}/>
    }
  }

  protected def formatParams(params: Option[Map[String, List[String]]]): NodeSeq = {
    (params map { parameters =>
      <params>
        {for((key, value) <- parameters) yield <var key={key}>{value.mkString(" ")}</var>}
      </params>
    }).orNull
  }

  protected def sendNotification(xml: NodeSeq): IO[Int] = io {
    val connection = new URL(getAirbrakeEndpoint).openConnection.asInstanceOf[HttpURLConnection]
    connection.setDoOutput(true)
    connection.setRequestProperty("Content-type", "text/xml")
    connection.setRequestProperty("Accept", "text/xml, application/xml")
    connection.setRequestMethod("POST")

    val writer = new OutputStreamWriter(connection.getOutputStream)
    writer.write(xml.toString())
    writer.close()

    connection.getResponseCode
  }

  protected def getAirbrakeEndpoint: String = {
    "%s://api.airbrake.io/notifier_api/v2/notices".format(if (isSecure) "https" else "http")
  }

  private def submitAsync(f: () => Unit) {
    getRandomActor(f)
  }

  private def getRandomActor: Actor[() => Unit] = {
    actorList(random.nextInt(actorList.length))
  }

  private def airbrakeActor: Actor[() => Unit] = {
    actor(doNotify)
  }

  private def doNotify(f: () => Unit) {
    f()
  }

}

case class AirbrakeNotice(throwable: Throwable,
                          url: Option[URL] = none[URL],
                          params: Option[Map[String, List[String]]] = none[Map[String, List[String]]],
                          component: Option[String] = none[String])
