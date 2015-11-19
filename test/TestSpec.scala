import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._

/**
 * TestSpec, unit testing at a hackathon l0lz.
 */
@RunWith(classOf[JUnitRunner])
class TestSpec extends Specification {

  "Application" should {

    "send 404 on a bad request" in new WithApplication{
      route(FakeRequest(GET, "/bad_route")) must beSome.which (status(_) == NOT_FOUND)
    }

  }
}
