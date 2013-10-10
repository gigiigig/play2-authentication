package org.gg.play.authentication.auth

import scala.concurrent.Future

import play.api.mvc._
import play.api.libs.iteratee.{Enumerator, Iteratee}
import play.api.libs.json.{Json, JsValue}
import play.api.libs.concurrent.Execution.Implicits._

import org.gg.play.authentication.misc.Loggable


/**
 * User: luigi
 * Date: 05/09/13
 * Time: 11:50
 */
trait Secured extends BodyParsers with Loggable {

  val COOKIE_REMEMBER_ME: String = "PLAY_REMEMBER_ME"
  val UNAUTHORIZED_REST = "not authorized"

  def secureUsersRetriever: SecureUsersRetriever

  /**
   * Try to retrieve the username of the user,
   * before from the standard play session,
   * and after from a the remember me cookie
   *
   * @param request
   * @return
   */
  def username(request: RequestHeader): Option[String] = {

    val remoteAddress = s"  ip : ${request.remoteAddress}"

    request.session.get(Security.username) match {
      case username: Some[String] =>
        log debug "username from session : " + username.get + remoteAddress
        username
      case None =>
        request.cookies.get(COOKIE_REMEMBER_ME) match {
          case None => None
          case Some(cookie) =>
            val email = secureUsersRetriever.findByRemember(cookie.value).map(_.email)
            log debug "username from cookie : " + email.getOrElse("email not found") + "  cookie : " + cookie.value + remoteAddress
            email
        }
    }
  }

  /**
   * This function is called when user is not authorized,
   * because probably we want a redirect, we leave the controller to implement that
   * function
   *
   * @param request
   * @return
   */
  def onUnauthorized(request: RequestHeader): Result

  /**
   * This function is called when a rest call is not authorized,
   * a default message can be provided in that case
   *
   * @param request
   * @return
   */
  def onUnauthorizedRest(request: RequestHeader): Result = {
    log.debug(s"on onUnauthorized ip : ${request.remoteAddress}")
    Results.Unauthorized(UNAUTHORIZED_REST)
  }

  /**
   * Create a WebSocket loading the username,
   * if user not exist create a WebSocket which send the not
   * authorized message and close the connection
   *
   * @param f
   * @return
   */
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

  /**
   * The scope of this function is to wrap the creation of
   * an acton, providing the username of the user,
   * retrieved from the session or from the remember cookie
   *
   * @param f
   * @param unauthF
   * @param parser
   * @return
   */
  def withAuthBase(f: => String => Request[_ >: AnyContent] => Result,
                   unauthF: RequestHeader => Result = onUnauthorized,
                   parser: BodyParser[_ >: AnyContent] = parse.anyContent): EssentialAction = {

    Security.Authenticated(username, unauthF) {
      user =>
        Action(parser)(request => f(user)(request))
    }

  }

  /**
   * This function, relying on  withAuthBase,
   * try to pass the user loaded from SecureUsersRetriever to the
   * wrapped action
   *
   * Don't use this function directly,
   * use on of the withUser implementation
   *
   * @param unauthF
   * @param parser
   * @param userFilter
   * @param f
   * @tparam T
   * @return
   */
  def withUserBase[T <: SecureUser](unauthF: RequestHeader => Result = onUnauthorized,
                                    parser: BodyParser[_ >: AnyContent] = parse.anyContent,
                                    userFilter: SecureUser => Boolean = _ => true)
                                   (f: T => Request[_ >: AnyContent] => Result): EssentialAction = {

    withAuthBase({
      username => implicit request =>
        secureUsersRetriever.findByEmail(username).filter(userFilter).map {
          user =>
            f(user.asInstanceOf[T])(request)
        }.getOrElse(unauthF(request))
    }, unauthF, parser)

  }

  /**
   * Default implementation, should be used to wrap
   * html actions, call onUnauthorized implementation if
   * user is not present
   *
   * @tparam T
   * @return
   */
  def withUser[T <: SecureUser]: ((T) => (Request[_ >: AnyContent]) => Result) => EssentialAction = withUserBase() _

  /**
   * Default implementation for rest calls,
   * call onUnauthorizedRest if user not exists,
   * to use for standard rest GET
   *
   * @tparam T
   * @return
   */
  def withRestUser[T <: SecureUser]: ((T) => (Request[_ >: AnyContent]) => Result) => EssentialAction = withUserBase(onUnauthorizedRest) _

  /**
   * This implementation must be used for rest calls,
   * when the request have a JSON body
   *
   * @tparam T
   * @return
   */
  def withJsonUser[T <: SecureUser]: ((T) => (Request[_ >: AnyContent]) => Result) => EssentialAction = withUserBase(onUnauthorizedRest, parse.json) _

  /**
   * Same as withUser call,
   * verify that user is admin
   *
   * @tparam T
   * @return
   */
  def withAdmin[T <: SecureUser]: ((T) => (Request[_ >: AnyContent]) => Result) => EssentialAction = withUserBase(userFilter = _.isAdmin) _


  /**
   * Same as withUser call,
   * verify that user is admin
   *
   * @tparam T
   * @return
   */
  def withRestAdmin[T <: SecureUser]: ((T) => (Request[_ >: AnyContent]) => Result) => EssentialAction = withUserBase(onUnauthorizedRest, userFilter = _.isAdmin) _


  /**
   * Same as withUser call,
   * verify that user is admin
   *
   * @tparam T
   * @return
   */
  def withJsonAdmin[T <: SecureUser]: ((T) => (Request[_ >: AnyContent]) => Result) => EssentialAction = withUserBase(onUnauthorizedRest, parse.json, userFilter = _.isAdmin) _

}

/**
 * The DAO class, must mix that trait for allow
 * secured trait to load user from database
 *
 */
trait SecureUsersRetriever {
  def findByEmail(email: String): Option[SecureUser]

  def findByRemember(cookie: String): Option[SecureUser]
}

/**
 * Mi that trait in User implementation
 */
trait SecureUser {
  def id: Option[Int]

  def email: String

  def isAdmin: Boolean
}