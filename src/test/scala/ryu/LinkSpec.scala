package ryu

import org.specs._

class LinkSpec extends Specification {
  "A Link" should {
    "render a key'd header value" in {
      val h = Link('fighters, Some("sagat"), "boss").headerVal
      h must be_==("</raw/fighters/sagat>; riaktag=\"boss\"")
    }
    "render a non-key'd header value" in {
      val h = Link('fighters, None, "all").headerVal
      h must be_==("</raw/fighters>; riaktag=\"all\"")
    }
  }
}