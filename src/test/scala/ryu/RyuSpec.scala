package ryu

import org.specs._

/** note: expects a running riak server running
 *  on localhost@8098
 */
object RyuSpec extends Specification {
  "Ryu" should {
    
    // timeout
    import Mapred.QueryDefaults._
    
    val db = Ryu("localhost",8098)
    
    "store a document" in {
      val key = ^('fighters, "ryu")
      val value = "test"
      val (doc, headers) = db(key, value)
      db - key
      doc must be_==(value)
    }
    "store multiple documents" in {
      val key1 = ^('fighters, "ryu")
      val value1 = "ryu"
      val key2 = ^('fighters, "dan")
      val value2 = "dan"
      val res = db ++ ((key1, value1) :: (key2, value2) :: Nil)
      db - key1
      db - key2
      res.size must be_==(2)
      res(0)._1 must be_==(value1)
      res(1)._1 must be_==(value2)
      
    }
    "get a document" in {
      val key = ^('fighters, "ryu")
      val value = "test"
      db(key, value)
      val (doc, headers) = db(key, value)
      db - key
      doc must be_==(value)
    }
    "delete a document" in {
      val key = ^('fighters, "ryu")
      val value = "test"
      db(key, value)
      db - key
      db(key) must throwA[dispatch.StatusCode] 
    }
    "walk between documents " in {
      val l2r = Link('fighters, Some("ryu"), "white")
      val ken = ^('fighters, "ken", None, Some(Seq(l2r)))
      val l2k = ken asLink("red")
      val kenValue = "this is ken"
      
      val ryu = ^('fighters, "ryu", None, Some(Seq(l2k)))
      val ryuValue = "this is ryu"
      
      db(ken, kenValue)
      db(ryu, ryuValue)
      
      // walk from ken to ryu
      val k2r = db > (ken, l2r.queryVal(true))
      k2r.size must_==(1)
      val (rdoc, rheaders) = k2r(0)
      rdoc must be_==(ryuValue)
      rheaders must haveKey("Location") //-> /raw/fighters/ryu
      rheaders must haveKey("Last-Modified")
      rheaders must haveKey("Etag")
      rheaders must haveKey("Link")
      
      // walk from ryu to ken
      val r2k = db > (ryu, l2k.queryVal(true))
      r2k.size must_== 1
      val (kdoc, kheaders) = r2k(0)
      kdoc must be_==(kenValue)
      kheaders must haveKey("Location") //-> /raw/fighters/ken
      kheaders must haveKey("Last-Modified")
      kheaders must haveKey("Etag")
      kheaders must haveKey("Link")
      
      val rall = db > (ryu, ('fighters, None, Some(true)))
      rall.size must_== 1
      val (alldoc, allheaders) = rall(0)
      alldoc must be_==(kenValue)
      allheaders must haveKey("Location") //-> /raw/fighters/ken
      allheaders must haveKey("Last-Modified")
      allheaders must haveKey("Etag")
      allheaders must haveKey("Link")
       
      db - ken
      db - ryu
    }
    "support map reduce for simple types" in {
      val key = ^('fighters, "ryu")
      val value = "test"
      db(key, value)
      val q = Query(Seq(("fighters", None, None)), Seq(Mapper named("Riak.mapValues")))
      val (r, resHeaders) = db mapred(q)
      r must be_==("[\"test\"]")
    }
    "support map reduce for compile types" in {
      val key = ^('fighters, "ryu", None, None)
      val value = "{\"foo\":\"bar\"}"
      db(key, value)
      val q = Query(Seq(("fighters", None, None)), Seq(Mapper named("Riak.mapValuesJson")))
      val (r, resHeaders) = db mapred(q)
      r must be_==("[{\"foo\":\"bar\"}]")
      db - key
    }
    // TODO "should support map reduce for annonymous mappers fns"
    // TODO "should support map reduce for annonymous reducer fns"
    // TODO "should support map reduce for `chained` queries"
  }
}