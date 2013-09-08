package org.gg.play.authentication.auth

import org.specs2.mutable._
import org.gg.play.authentication.misc.Loggable
import play.api.mvc._
import play.api.mvc.Results._
import play.api.test.Helpers._
import play.api.test.{WithApplication, FakeRequest}
import play.api.test.FakeApplication

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.NotImplementedError

/**
 * Created with IntelliJ IDEA.
 * User: luigi
 * Date: 06/09/13
 * Time: 14:18
 */
class SecuredSpec extends Specification with Loggable with DeactivatedTimeConversions {

  import FakeUsersRetriever._

  "Secured Spec".title

  "#username" should {

    "return None if username is nor in session neither in remember cookie" in fakeApp {
      SecuredController.username(FakeRequest()) must beNone
    }

    "return Some(username) if username is in session" in fakeApp {
      SecuredController.username(FakeRequest()
        .withSession(Security.username -> username)) mustEqual Some(username)
    }

    "return Some(username) if username is in remember cookie" in fakeApp {
      SecuredController.username(FakeRequest()
        .withCookies(Cookie(
        SecuredController.COOKIE_REMEMBER_ME, "cookievalue", Some(10000)))) mustEqual Some(username)
    }

  }

  "#onUnauthorizedRest" should {

    """return OK status with "not authorized" message""" in fakeApp {
      val result: Result = SecuredController.onUnauthorizedRest(FakeRequest())
      status(result) mustEqual OK
      contentAsString(result) mustEqual "not authorized"
    }

  }

  "#withAuthBase" should {

    def sendRequest(request: RequestHeader) = {
      Await.result(SecuredController.withAuthBase({
        username => request => Ok(username)
      })(request).run, 3 seconds)
    }

    "call onUnauthorized if username return None" in fakeApp {
      sendRequest(FakeRequest()) must throwA[NotImplementedError]
    }

    "return the action returned from the function f() if username exist" in fakeApp {

      val request = sendRequest(FakeRequest().withSession(Security.username -> username))
      status(request) mustEqual OK
      contentAsString(request) mustEqual username

    }

  }

  "#withUserBase" should {

    def sendRequest(request: RequestHeader) = {
      Await.result(SecuredController.withUserBase[SecureUser]()({
        user => request => Ok(user.email)
      })(request).run, 3 seconds)
    }

    "call onUnauthorized if username return None" in fakeApp {
      sendRequest(FakeRequest()) must throwA[NotImplementedError]
    }

    "call f() with the user loaded from database and return the f() action" in fakeApp {
      pending
    }

  }

  def fakeApp = new WithApplication(FakeApplication()) {}


}


object SecuredController extends Controller with Secured {
  def secureUsersRetriever: SecureUsersRetriever = FakeUsersRetriever

  /**
   * This function is called when user is not authorized,
   * because probably we want a redirect, we leave the controller to imlpement that
   * function
   *
   * @param request
   * @return
   */
  def onUnauthorized(request: RequestHeader): Result = ???
}

object FakeUsersRetriever extends SecureUsersRetriever {

  val username: String = "test@gmail.it"

  def findByEmail(email: String): Option[SecureUser] = ???

  def findByRemember(cookie: String): Option[SecureUser] = Some(new SecureUser {
    def isAdmin: Boolean = false

    def email: String = username

    def id: Option[Int] = Some(1)
  })

}

trait DeactivatedTimeConversions extends org.specs2.time.TimeConversions {
  override def intToRichLong(v: Int) = super.intToRichLong(v)
}