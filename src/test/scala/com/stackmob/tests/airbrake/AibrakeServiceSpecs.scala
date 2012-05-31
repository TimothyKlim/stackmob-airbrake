package com.stackmob.tests.airbrake

import org.specs2._
import com.stackmob.airbrake.{AirbrakeNotice, AirbrakeService}
import javax.xml.validation.SchemaFactory
import javax.xml.transform.stream.StreamSource
import java.net.{HttpURLConnection, URL}
import java.io.StringReader
import scala.io.Source
import scala.xml.NodeSeq
import scalaz._
import scalaz.effects._
import Scalaz._

class AibrakeServiceSpecs extends Specification { def is =

  "AibrakeServiceSpecs".title                                                    ^
  """
  The Airbrake service provides access to the Airbrake Notification API.
  """                                                                            ^
  "Prepare a notification with only an error elem if no url is provided"         ! airbrake().prepareErrorOnly ^
  "Prepare a notification with a request elem if a url is provided"              ! airbrake().prepareWithUrl ^
  "Prepare a notification with including a component if a component is provided" ! airbrake().prepareWithComponent ^
  "Send a notification synchronously"                                            ! airbrake().sendNoticeSync ^
  "Send a notification asynchronously"                                           ! airbrake().sendNoticeAsync ^
                                                                                 end

  case class airbrake() extends context {

    def prepareErrorOnly = {
      apply {
        val service = new AirbrakeService {
          override def getApiKey = key
          override def getEnvironment = environment
          override def isSecure = true
        }
        val ex = new Exception("foobar")
        val notice = AirbrakeNotice(ex)
        val request = service.prepareRequest(notice)
        (request must \\("api-key") \> key) and
          (request must \\("environment-name") \> environment) and
          (request must \\("class") \> ex.getClass.getName) and
          (request must \\("message") \> ex.getMessage) and
          (request must \\("line")) and
          (request must not \\ ("request")) and
          (validate(request) must beTrue)
      }
    }

    def prepareWithUrl = {
      apply {
        val service = new AirbrakeService {
          override def getApiKey = key
          override def getEnvironment = environment
          override def isSecure = true
        }
        val ex = new Exception("foobar")
        val varKey = "foo"
        val uri = "http://stackmob.com"
        val params = Map(varKey -> List("bar1", "bar2"))
        val notice = AirbrakeNotice(ex, new URL(uri).some, params.some)
        val request = service.prepareRequest(notice)
        (request must \\("api-key") \> key) and
          (request must \\("environment-name") \> environment) and
          (request must \\("class") \> ex.getClass.getName) and
          (request must \\("message") \> ex.getMessage) and
          (request must \\("line")) and
          (request must \\("request")) and
          (request must \\("params")) and
          (request must \\("url") \> uri) and
          (request must \\("var", "key" -> varKey) \> (params.get(varKey) | List()).mkString(" ")) and
          (request must \\("component") \> "") and
          (validate(request) must beTrue)
      }
    }

    def prepareWithComponent = {
      apply {
        val service = new AirbrakeService {
          override def getApiKey = key
          override def getEnvironment = environment
          override def isSecure = true
        }
        val ex = new Exception("foobar")
        val varKey = "foo"
        val uri = "http://stackmob.com"
        val params = Map(varKey -> List("bar1", "bar2"))
        val component = "AirbrakeSpecs".some
        val notice = AirbrakeNotice(ex, new URL(uri).some, params.some, component)
        val request = service.prepareRequest(notice)
        (request must \\("api-key") \> key) and
          (request must \\("environment-name") \> environment) and
          (request must \\("class") \> ex.getClass.getName) and
          (request must \\("message") \> ex.getMessage) and
          (request must \\("line")) and
          (request must \\("request")) and
          (request must \\("params")) and
          (request must \\("url") \> uri) and
          (request must \\("var", "key" -> varKey) \> (params.get(varKey) | List()).mkString(" ")) and
          (request must \\("component") \> (component | "")) and
          (validate(request) must beTrue)
      }
    }

    def sendNoticeSync = {
      apply {
        val service = new AirbrakeService {
          override def getApiKey = key
          override def getEnvironment = environment
          override def isSecure = true
          override protected def sendNotification(xml: NodeSeq): IO[Int] = {
            HttpURLConnection.HTTP_OK.pure[IO]
          }
        }
        val ex = new Exception("foobar")
        val varKey = "foo"
        val uri = "http://stackmob.com"
        val params = Map(varKey -> List("bar1", "bar2"))
        val component = "AirbrakeSpecs".some
        val notice = AirbrakeNotice(ex, new URL(uri).some, params.some, component)
        service.notifySync(notice).either must beRight.like { case code: Int => code must beEqualTo(HttpURLConnection.HTTP_OK) }
      }
    }

    def sendNoticeAsync = {
      apply {
        val service = new AirbrakeService {
          override def getApiKey = key
          override def getEnvironment = environment
          override def isSecure = true
          override protected def sendNotification(xml: NodeSeq): IO[Int] = {
            HttpURLConnection.HTTP_OK.pure[IO]
          }
        }
        val ex = new Exception("foobar")
        val varKey = "foo"
        val uri = "http://stackmob.com"
        val params = Map(varKey -> List("bar1", "bar2"))
        val component = "AirbrakeSpecs".some
        val notice = AirbrakeNotice(ex, new URL(uri).some, params.some, component)
        service.notifyAsync(notice).either must beRight
      }
    }

  }

  trait context extends AirbrakeContext {

    protected val key = "some-key"
    protected val environment = "specs"

    def validate(xml: NodeSeq): Boolean = {
      (validating {
        val xmlReader = new StringReader(xml.toString())
        val factory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema")
        val xsdSource = Source.fromURL(getClass.getResource("/airbrake_2_2.xsd"))
        val schema = factory.newSchema(new StreamSource(xsdSource.bufferedReader()))
        val validator = schema.newValidator()
        validator.validate(new StreamSource(xmlReader))
      }).fold(failure = _ => false, success = _ => true)
    }

  }

}
