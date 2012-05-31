package com.stackmob.tests.airbrake

import org.specs2.specification._
import org.specs2.execute.Result
import scalaz._
import Scalaz._

trait AirbrakeContext extends Around {

  def before() { }

  def after() { }

  override def around[T <% Result](t: => T): Result = {
    try { before(); t } finally { after() }
  }

  protected def validating[A](a: => A): Validation[Throwable, A] = a.pure[Function0].throws

}
