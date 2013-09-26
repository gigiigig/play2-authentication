package org.gg.play.authentication.auth

import org.specs2.mutable._
import org.gg.play.authentication.misc.Loggable

import auth.BaseSpec
import play.api.mvc._
import play.api.mvc.Results._
import play.api.test.Helpers._
import play.api.test.{WithApplication, FakeRequest}
import play.api.test.FakeApplication

/**
 * User: luigi
 * Date: 11/09/13
 * Time: 14:35
 */
class OAuthSpec extends Specification with Loggable with BaseSpec {

  "OAuth Spec".title

  "clientIdParam" should {

    val fakeAppClientId = "fake_oauth_client_id"

    "return a tuple with client_id key" in fakeApp {
      OAuthController.clientIdParam(NotProvider)._1 mustEqual "client_id"
    }

    """return as value for client_id the property
       'oauth2.provider.${PROVIDER_NAME}.clientId' taken from application.config""" in
      fakeApp(Map("oauth2.provider.not_provider.clientId" -> fakeAppClientId)) {

      (OAuthController.clientIdParam(NotProvider)._2)(0) mustEqual fakeAppClientId

    }

  }

  "clientSecretParam" should {

    val fakeAppClientSecret = "fake_oauth_client_secret"

    "return a tuple with client_secret key" in fakeApp {
      OAuthController.clientSecretParam(NotProvider)._1 mustEqual "client_secret"
    }

    """return as value for client_id the property
       'oauth2.provider.${PROVIDER_NAME}.clientSecret' taken from application.config""" in
      fakeApp(Map("oauth2.provider.not_provider.clientSecret" -> fakeAppClientSecret)) {

      (OAuthController.clientSecretParam(NotProvider)._2)(0) mustEqual fakeAppClientSecret

    }

  }

  "oauth" should {

    "return BadRequest if provider is not recognized" in fakeApp {
      val response = OAuthController.oauth("wrong_provider")(FakeRequest())
      status(response) mustEqual BAD_REQUEST
      contentAsString(response) mustEqual "provider not exist"
    }

    "redirect to OAUTH2_CODE_URL if provider exist and code is not in query string" in fakeApp {
      val provider = GoogleProvider
      val response = OAuthController.oauth(provider.NAME)(FakeRequest())
      status(response) mustEqual SEE_OTHER
      redirectLocation(response).get must startWith(provider.OAUTH2_CODE_URL)
    }

    "call useEmail method with email retrived from provider getEmail method" in fakeApp {
      pending("how to test ?")
      val testEmail = "test@test.com"
      val provider = GoogleProvider
      (OAuthController.oauth(provider.NAME)(FakeRequest().withFormUrlEncodedBody("code" -> "ciao"))) mustEqual NOT_IMPLEMENTED
    } 

  }

}

object OAuthController extends OAuth {
  /**
   * This method define how to use email
   * after oauth login is concluded
   *
   * @param email
   * @return
   */
  def useEmail(email: String, provider: String): Result = Ok(s"${email}:${provider}")

  /**
   * Return the redirect url for oauth
   *
   * @param provider
   * @return
   */
  def redirectUrl(provider: String): Call = Call("", "")
}


