package org.gg.play.authentication.auth

import scala.concurrent.{Future, Await}
import scala.concurrent.duration._

import org.specs2.mutable._
import org.gg.play.authentication.misc.Loggable
import play.api.mvc._
import play.api.mvc.Results._
import play.api.test.Helpers._
import play.api.test.{PlaySpecification, FakeRequest}
import auth.BaseSpec

/**
 * User: luigi
 * Date: 06/09/13
 * Time: 14:18
 */
class SecuredSpec extends PlaySpecification with Loggable with DeactivatedTimeConversions with BaseSpec {

  import FakeUsersRetriever._

  def testSession = Security.username -> username

  "Secured Spec".title

  "#username" should {

    "return None if username is nor in session neither in remember cookie" in fakeApp {
      SecuredController.username(FakeRequest()) must beNone
    }

    "return Some(username) if username is in session" in fakeApp {
      SecuredController.username(FakeRequest()
        .withSession(testSession)) mustEqual Some(username)
    }

    "return Some(username) if username is in remember cookie" in fakeApp {
      SecuredController.username(FakeRequest()
        .withCookies(Cookie(
        SecuredController.CookieRememberMe, "cookievalue", Some(10000)))) mustEqual Some(username)
    }
  }

  "#onUnauthorizedRest" should {

    """return UNAUTHORIZED status with UNAUTHORIZED_REST message""" in fakeApp {
      val result = Future.successful(SecuredController.onUnauthorizedRest(FakeRequest()))
      status(result) mustEqual UNAUTHORIZED
      contentAsString(result) mustEqual SecuredController.UnauthorizedRest
    }
  }

  "#withAuthBase" should {

    def sendRequest(request: RequestHeader) = {
      SecuredController.withAuthBase[AnyContent](){
        username => request => Future.successful(Ok(username))
      }(request).run
    }

    "call onUnauthorized if username return None" in fakeApp {
      status(sendRequest(FakeRequest())) mustEqual NOT_IMPLEMENTED
    }

    "return the action returned from the function f() if username exist" in fakeApp {
      val response = sendRequest(FakeRequest().withSession(testSession))
      status(response) mustEqual OK
      contentAsString(response) mustEqual username
    }
  }

  "#withUserBase" should {

    val f: (SecureUser) => (Request[_ >: AnyContent]) => Future[Result] = {
      user => request => Future.successful(Ok(user.email))
    }

    val action: EssentialAction = SecuredController.withUserBase()(f)

    def sendRequest(request: RequestHeader, action: EssentialAction = action) = {
      action(request).run
    }

    "call onUnauthorized if username return None" in fakeApp {
      status(sendRequest(FakeRequest())) mustEqual NOT_IMPLEMENTED
    }

    "call f(user) with the user loaded from database and return the f() => Action" in fakeApp {
      val response = sendRequest(FakeRequest().withSession(testSession))
      status(response) mustEqual OK
      contentAsString(response) mustEqual FakeUsersRetriever.fakeUser.get.email
    }

    "call onUnauthorized if userFilter() function NOT validate the user" in fakeApp {
      status(sendRequest(FakeRequest().withSession(testSession),
        SecuredController.withUserBase[AnyContent](userFilter = _ => false)(f))) mustEqual NOT_IMPLEMENTED
    }

    "call f(user) if userFilter() function validate the user" in fakeApp {
      val response = sendRequest(FakeRequest().withSession(testSession),
        SecuredController.withUserBase[AnyContent](userFilter = _ => true)(f))

      status(response) mustEqual OK
      contentAsString(response) mustEqual FakeUsersRetriever.fakeUser.get.email
    }

    "call unauthF() if it is passed as parameter when user is NOT authorized" in fakeApp {
      val response = sendRequest(FakeRequest(),
        SecuredController.withUserBase[AnyContent](unauthF = request => BadRequest)(f))
      status(response) mustEqual BAD_REQUEST
    }
  }
}


object SecuredController extends Controller with Secured[SecureUser] {
  def secureUsersRetriever: SecureUsersRetriever[SecureUser] = FakeUsersRetriever

  /**
   * This function is called when user is not authorized,
   * because probably we want a redirect, we leave the controller to imlpement that
   * function
   *
   * @param request
   * @return
   */
  def onUnauthorized(request: RequestHeader): Result = NotImplemented
}

object FakeUsersRetriever extends SecureUsersRetriever[SecureUser] {

  val username: String = "test@gmail.it"

  def findByEmail(email: String): Option[SecureUser] = fakeUser

  def findByRemember(cookie: String): Option[SecureUser] = fakeUser

  def fakeUser = Some(new SecureUser {
    def isAdmin: Boolean = false

    def email: String = username

    def id: Int = 1
  })

}

trait DeactivatedTimeConversions extends org.specs2.time.TimeConversions {
  override def intToRichLong(v: Int) = super.intToRichLong(v)
}