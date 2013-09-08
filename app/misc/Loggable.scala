package org.gg.play.authentication.misc

import play.api.Logger

/**
 * User: luigi
 * Date: 20/04/13
 * Time: 16:51
 */
trait Loggable {
  lazy val log = Logger("application." + this.getClass.getName)
}
