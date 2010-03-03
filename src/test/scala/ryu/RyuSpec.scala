package ryu

import org.specs._

/** note: expects a running riak server running
 *  on localhost@8098
 */
object RyuSpec extends Specification {
  "Ryu" should {
    
    val db = Ryu("localhost",8098)
    
    "store a document" in {
      val key = ^('fighters, "ryu", None, None)
      val value = "test"
      val (doc, headers) = db(key, value)
      print("put response body -> " + doc)
      print("put response h -> " + headers)
      doc must be_==(value)
    }
    "get a document" in {
      val key = ^('fighters, "ryu", None, None)
      val value = "test"
      db(key, value)
      val (doc, headers) = db(key, value)
      print("get response body -> " + doc)
      print("get response h -> " + headers)
      doc must be_==(value)
    }
    "delete a document" in {
      val key = ^('fighters, "ryu", None, None)
      val value = "test"
      db(key, value)
      db - key
      db(key) must throwA[dispatch.StatusCode] 
    }
    "walk about" in {
      val key = ^('fighters, "ryu", None, None)
      val value = "test"
      db(key, value)
      val (r, headers) = db > (key, ('fighters, None, None))
      print("walk response body -> " + r)
      print("walk response h -> " + headers)
      headers must haveKey("Expires")
    }
    "support map reduce for simple types" in {
      val key = ^('fighters, "ryu", None, None)
      val value = "test"
      db(key, value)
      val q = Query(Seq(("fighters", None)), Seq(Mapper named("Riak.mapValues")))
      val (r, resHeaders) = db mapred(q)
      print("m/r 1 response h -> " + resHeaders)
      print("m/r 1  r -> " + r)
      r must be_==("[\"test\"]")
    }
    "support map reduce for compile types" in {
      val key = ^('fighters, "ryu", None, None)
      val value = "{\"foo\":\"bar\"}"
      db(key, value)
      val q = Query(Seq(("fighters", None)), Seq(Mapper named("Riak.mapValuesJson")))
      val (r, resHeaders) = db mapred(q)
      print("m/r 2 -> " + r)
      r must be_==("[{\"foo\":\"bar\"}]")
    }
    // TODO "should support map reduce for annonymous mappers fns"
    // TODO "should support map reduce for annonymous reducer fns"
    // TODO "should support map reduce for `chained` queries"
  }
}