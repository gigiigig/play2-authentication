package org.gg.play.authentication.auth

import scala.concurrent.Future

import play.api.mvc._
import play.api.libs.iteratee.{Enumerator, Iteratee}
import play.api.libs.json.{Json, JsValue}
import play.api.libs.concurrent.Execution.Implicits._

import org.gg.play.authentication.misc.Loggable


/**
 * Created with IntelliJ IDEA.
 * User: luigi
 * Date: 05/09/13
 * Time: 11:50
 * To change this template use File | Settings | File Templates.
 */
trait Secured extends BodyParsers with Loggable {

  val COOKIE_REMEMBER_ME: String = "PLAY_REMEMBER_ME"

  def secureUsersRetriever: SecureUsersRetriever

  /**
   * Try to retrive the username of the user,
   * before from the standard play session,
   * and after from a the remember me cookie
   *
   * @param request
   * @return
   */
  def username(request: RequestHeader): Option[String] = {

    val remoteAddress = s"    ip : ${request.remoteAddress}"

    request.session.get(Security.username) match {
      case username: Some[String] =>
        log debug "username from session : " + username.get + remoteAddress
        username
      case None =>
        request.cookies.get(COOKIE_REMEMBER_ME) match {
          case None => None
          case Some(cookie) =>
            log debug "username from cookie : " + cookie.value + remoteAddress
            secureUsersRetriever.findByRemember(cookie.value).map(_.email)
        }
    }
  }

  /**
   * This function is called when user is not authorized,
   * because probably we want a redirect, we leave the controller to imlpement that
   * function
   *
   * @param request
   * @return
   */
  def onUnauthorized(request: RequestHeader): Result

  def onUnauthorizedRest(request: RequestHeader): Result = {
    log.debug(s"on onUnauthorized ip : ${request.remoteAddress}")
    Results.Ok("not authorized")
  }

  def withAuthWS(f: => Int => Future[(Iteratee[JsValue, Unit], Enumerator[JsValue])]): WebSocket[JsValue] = {

    def errorFuture = {
      // Just consume and ignore the input
      val in = Iteratee.ignore[JsValue]

      // Send a single 'Hello!' message and close
      val out = Enumerator(Json.toJson("not authorized")).andThen(Enumerator.eof)

      Future {
        (in, out)
      }
    }

    WebSocket.async[JsValue] {
      request =>
        username(request) match {
          case None =>
            errorFuture

          case Some(username) =>
            secureUsersRetriever.findByEmail(username).map {
              user =>
                f(user.id.get)
            }.getOrElse(errorFuture)

        }
    }


  }

  def withAuthBase(f: => String => Request[_ >: AnyContent] => Result,
                   authF: RequestHeader => Result = onUnauthorized,
                   parser: BodyParser[_ >: AnyContent] = parse.anyContent): EssentialAction = {

    Security.Authenticated(username, authF) {
      user =>
        Action(parser)(request => f(user)(request))
    }

  }

  def withUserBase[T <: SecureUser](authF: RequestHeader => Result = onUnauthorized,
                                    parser: BodyParser[_ >: AnyContent] = parse.anyContent,
                                    userFilter: SecureUser => Boolean = _ => true)
                                   (f: T => Request[_ >: AnyContent] => Result): EssentialAction = {

    withAuthBase({
      username => implicit request =>
        secureUsersRetriever.findByEmail(username).filter(userFilter).map {
          user =>
            f(user.asInstanceOf[T])(request)
        }.getOrElse(authF(request))
    }, authF, parser)

  }


  def withUser[T <: SecureUser]: ((T) => (Request[_ >: AnyContent]) => Result) => EssentialAction = withUserBase() _

  def withRestUser[T <: SecureUser]: ((T) => (Request[_ >: AnyContent]) => Result) => EssentialAction = withUserBase(onUnauthorizedRest) _

  def withJsonUser[T <: SecureUser]: ((T) => (Request[_ >: AnyContent]) => Result) => EssentialAction = withUserBase(onUnauthorizedRest, parse.json) _

  def withAdmin[T <: SecureUser]: ((T) => (Request[_ >: AnyContent]) => Result) => EssentialAction = withUserBase(userFilter = _.isAdmin) _

  def withRestAdmin[T <: SecureUser]: ((T) => (Request[_ >: AnyContent]) => Result) => EssentialAction = withUserBase(onUnauthorizedRest, userFilter = _.isAdmin) _

  def withJsonAdmin[T <: SecureUser]: ((T) => (Request[_ >: AnyContent]) => Result) => EssentialAction = withUserBase(onUnauthorizedRest, parse.json, userFilter = _.isAdmin) _

}


trait SecureUsersRetriever {
  def findByEmail(email: String): Option[SecureUser]

  def findByRemember(cookie: String): Option[SecureUser]
}

trait SecureUser {
  def id: Option[Int]

  def email: String

  def isAdmin: Boolean
}