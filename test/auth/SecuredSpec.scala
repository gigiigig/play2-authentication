package org.gg.play.authentication.auth

import org.specs2.mutable._
import org.gg.play.authentication.misc.Loggable
import play.api.mvc.{Security, Result, RequestHeader, Controller}
import play.api.test.{WithApplication, FakeApplication, FakeRequest}

/**
 * Created with IntelliJ IDEA.
 * User: luigi
 * Date: 06/09/13
 * Time: 14:18
 */
class SecuredSpec extends Specification with Loggable {

  "Secured Spec".title

  "#username" should {

    "return None if username is nor in session neither in remember cookie" in {
      SecuredController.username(FakeRequest()) must beNone
    }

    "return Some(username) if username is in session" in fa {
      val username: String = "test@gmail.it"
      SecuredController.username(FakeRequest()
        .withSession(Security.username -> username)) mustEqual Some(username)
    }

  }

  object SecuredController extends Controller with Secured {
    def secureUsersRetriever: SecureUsersRetriever = ???

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

  def fa = new WithApplication(FakeApplication()) {}

}
