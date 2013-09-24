package auth

import play.api.test.{FakeApplication, WithApplication}

/**
 * User: luigi
 * Date: 11/09/13
 * Time: 14:43
 */
trait BaseSpec {

  def fakeApp : WithApplication = fakeApp()

  def fakeApp(config: Map[String, _] = Map()): WithApplication =
    new WithApplication(FakeApplication(additionalConfiguration = config + ("application.secret" -> "test_secret"))) {}

}
