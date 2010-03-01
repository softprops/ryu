package ryu

import org.specs._

object MapredSpec extends Specification {
  "A Query" should {
    "validate presence of mapper or reducer" in {
      val q = Query(Seq(("foo",None)), Seq())
      q.validate must throwAn[IllegalArgumentException]
    }
    "serialize as json with a single input (bucket,None)" in {
      val js = Query(Seq(("foo",None)), Seq(Mapper)).asJson
      js must be_==("{\"inputs\":\"foo\", \"query\":[{\"map\":{\"language\":\"javascript\",\"keep\":true}}]}")
    }
    "serialize as json with multiple inputs [(bucket,key),...]" in {
       val js = Query(Seq(("foo",Some("bar"))), Seq(Mapper)).asJson
       js must be_==("{\"inputs\":[[\"foo\",\"bar\"]], \"query\":[{\"map\":{\"language\":\"javascript\",\"keep\":true}}]}")
    }
    "serialize as json with varied mapper options" in {
      val js = Query(Seq(("foo",Some("bar"))), Seq(Mapper source("..."))).asJson
      js must be_==("{\"inputs\":[[\"foo\",\"bar\"]], \"query\":[{\"map\":{\"language\":\"javascript\",\"keep\":true,\"source\":\"...\"}}]}")
    }
    "serialize as json with varied reducer options" in {
      val js = Query(Seq(("foo",Some("bar"))), Seq(Reducer source("..."))).asJson
      js must be_==("{\"inputs\":[[\"foo\",\"bar\"]], \"query\":[{\"reduce\":{\"language\":\"javascript\",\"keep\":true,\"source\":\"...\"}}]}")
    }
    "serialize as json with list of phases" in {
      val js = Query(Seq(("foo",Some("bar"))), Seq(Linker tag("bar"), Mapper, Reducer)).asJson
      js must be_==("{\"inputs\":[[\"foo\",\"bar\"]], \"query\":[{\"link\":{\"tag\":\"bar\"},\"map\":{\"language\":\"javascript\",\"keep\":true},\"reduce\":{\"language\":\"javascript\",\"keep\":true}}]}")
    }
  }
}