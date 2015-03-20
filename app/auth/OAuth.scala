package org.gg.play.authentication.auth

import scala.concurrent.Await
import scala.concurrent.Future

import org.apache.commons.codec.binary.Base64
import org.gg.play.authentication.misc.Loggable

import play.api.Play.current
import play.api.mvc._
import play.api.mvc.Results._
import play.api.libs.json.Json
import play.api.libs.ws
import play.api.libs.ws.WS
import play.api.libs.concurrent.Execution.Implicits._

/**
 * User: luigi
 * Date: 05/09/13
 * Time: 11:51
 */
trait OAuth extends Loggable {

  def clientIdParam(provider: OAuthProvider) = "client_id" -> Seq(provider.CLIENT_ID)

  def clientSecretParam(provider: OAuthProvider) = "client_secret" -> Seq(provider.CLIENT_SECRET)

  def redirectUriParam(request: Request[AnyContent], provider: OAuthProvider) = {
    val url = redirectUrl(provider.NAME).absoluteURL(false)(request)
    log debug s"redirect url : $url"
    "redirect_uri" -> Seq(url)
  }

  def codeQuery(request: Request[AnyContent], provider: OAuthProvider) =
    Map(clientIdParam(provider),
      "response_type" -> Seq("code"),
      "scope" -> Seq("email"),
      redirectUriParam(request, provider),
      "state" -> Seq("ciao")
    )

  def tokenQuery(code: String, request: Request[AnyContent], provider: OAuthProvider) =
    Map("code" -> Seq(code),
      clientIdParam(provider),
      clientSecretParam(provider),
      redirectUriParam(request, provider),
      "grant_type" -> Seq("authorization_code")
    )

  def oauth(provider: String) = Action.async {

    request =>
      val code = request.getQueryString("code")

      val providerImpl: OAuthProvider = provider match {
        case "google" => GoogleProvider
        case "facebook" => FacebookProvider
        case _ => NotProvider
      }

      providerImpl match {

        case NotProvider => Future.successful(BadRequest("provider not exist"))
        case _ =>

          code match {

            case Some(c) =>
              WS.url(providerImpl.OAUTH2_TOKEN_URL).post(tokenQuery(c, request, providerImpl)) flatMap { response =>
                val email = providerImpl.getEmail(response)
                useEmail(email, providerImpl.NAME)
              }

            case None => Future.successful(Redirect(providerImpl.OAUTH2_CODE_URL, codeQuery(request, providerImpl)))
          }
      }
  }

  /**
   * This method define how to use email
   * after oauth login is concluded
   *
   * @param email
   * @return
   */
  def useEmail(email: String, provider: String): Future[Result]

  /**
   * Return the redirect url for oauth
   *
   * @param provider
   * @return
   */
  def redirectUrl(provider: String): Call

}

/**
 * OAuth provider
 */
trait OAuthProvider {

  import play.api.libs.ws.WSResponse
  import play.api.Play.current

  def NAME: String

  def OAUTH2_CODE_URL: String

  def OAUTH2_TOKEN_URL: String

  def CLIENT_ID: String = current.configuration.getString(s"oauth2.provider.${NAME}.clientId").getOrElse("")

  def CLIENT_SECRET: String = current.configuration.getString(s"oauth2.provider.${NAME}.clientSecret").getOrElse("")

  /**
   * Specify how to retrive email from the
   * request to the provider
   *
   * @param response
   * @return
   */
  def getEmail(response: WSResponse): String

}


/**
 * Google implementation for OAuthProvider
 */
object GoogleProvider extends OAuthProvider with Loggable {

  import play.api.libs.ws.WSResponse

  val NAME = "google"

  val OAUTH2_CODE_URL = "https://accounts.google.com/o/oauth2/auth"
  val OAUTH2_TOKEN_URL = "https://accounts.google.com/o/oauth2/token"

  def getEmail(response: WSResponse): String = {
    val idToken = (response.json \ "id_token").as[String].split("\\.")(1)
    val json: String = new String(Base64.decodeBase64(idToken))
    val email = (Json.parse(json) \ "email").as[String]
    log.debug("login with google with email : " + email)
    email
  }

}

/**
 * Facebook implementation for OAuthProvider
 */
object FacebookProvider extends OAuthProvider with Loggable {

  import play.api.libs.ws.WSResponse

  val NAME = "facebook"

  val OAUTH2_CODE_URL = "https://www.facebook.com/dialog/oauth"
  val OAUTH2_TOKEN_URL = "https://graph.facebook.com/oauth/access_token"

  def getEmail(response: WSResponse): String = {

    val accessToken = response.body

    import scala.concurrent.duration._

    val json = Await.result(WS.url(s"https://graph.facebook.com/me?${accessToken}").get(), 3 seconds)
    val email = (Json.parse(json.body) \ "email").as[String]
    log debug email

    email

  }

}

/**
 * Empty implementation, when requested provider not exists
 */
object NotProvider extends OAuthProvider {

  def NAME = "not_provider"

  def OAUTH2_CODE_URL: String = ???

  def OAUTH2_TOKEN_URL: String = ???

  def getEmail(response: ws.WSResponse): String = ???
}
