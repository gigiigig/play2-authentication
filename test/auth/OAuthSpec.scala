package org.gg.play.authentication.auth

import org.specs2.mutable._
import org.gg.play.authentication.misc.Loggable
import play.api.mvc.{Result, Call}

import play.api.mvc.Results._
import auth.BaseSpec
import oauth.signpost.OAuthProvider
import play.api.libs.ws.Response

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


