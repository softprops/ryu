package ryu

object Mapred {
  val path = "mapred"
  val defaults = Map("language"->"javascript", "keep"->true)
  object QueryDefaults {
    implicit val timeout = 60000
  }
}

private [ryu] abstract class Phase(val q: Map[String,Any])
 
private [ryu] class Linker(q:Map[String, Any]) extends Phase(q) {
  protected def arg(k:String)(v:Any) = new Linker(q + (k->v))
  val tag = arg("tag")_
}
/** Link phase builder */
object Linker extends Linker(Map())
 
private [ryu] class Mapper(q:Map[String, Any]) extends Phase(q) {
  protected def arg(k:String)(v:Any) = new Mapper(q + (k->v))
  val lang = arg("language")_
  /** jsanon js anonymous function */
  val source = arg("source")_
  /** jsfn cached js function called by name */
  val named = arg("name")_
  val keep = arg("keep")_
}
/** Map phase builder */
object Mapper extends Mapper(Mapred.defaults)

private [ryu] class Reducer(q:Map[String, Any]) extends Phase(q) {
  protected def arg(k:String)(v:Any) = new Reducer(q + (k->v))
  val lang = arg("language")_
  val source = arg("source")_
  val named = arg("name")_
  val keep = arg("keep")_
}
/** Reduce phase builder */
object Reducer extends Reducer(Mapred.defaults)

 /** A wrapper for a m/r query
  *  @param inputs a Seq of tuples in the format (bucket,Some(key),Some(keyData))
  *                A tuple containing only a bucket triggers the somewhat 
  *                expensive `list keys` operation
  *
  *  @param phases a Seq of map reduce {@link Phase}. Valid types are 
  *                Linkers, Mappers, Reducers. The presence of either a 
  *                Mapper or Reducer is required
  *
  *  @param timeout (implicit) time out for query specified in milliseconds. 
  *                A value of -1 is considered an unlimited timeout
  *                The default `implicit` timeout is 60,000
  *
  *  @examples
  *  1. follow all outbound links
  *  (equiv to http://host:port/riak/bucket/key/_,_,_)
  *  <pre>
  *    mapred(Query(
  *      Seq(("bucket", Some("key"), None)), Seq(
  *        Mapper source("function(v) { return [v]; }")
  *      )
  *    ))
  *  </pre>
  *
  *  2. follow only links that are tagged foo 
  *  (equiv to http://localhost:8098/riak/bucket/key/_,foo,_)
  *  <pre>
  *    mapred(Query(
  *      Seq(("bucket", Some("key"), None)), Seq(
  *        Linker tag("foo"),
  *        Mapper source("function(v) { return [v]; }")
  *      )
  *    ))
  *  </pre>
  * 
  *  3. Link phases may also be chained together (or put after other phases 
  *  if those phases produce bucket/key lists) 
  *  (equiv to http://localhost:8098/riak/bucket/key/_,_,_/_,_,_)
  *  <pre>
  *    mapred(Query(
  *      Seq(("bucket", Some("key"), None)), Seq(
  *        Linker, Linker
  *        Mapper source("function(v) { return [v]; }")
  *      )
  *    ))
  *  </pre>
  */
case class Query(inputs: Seq[(String, Option[String], Option[String])], phases: Seq[Phase])(implicit timeout:Int) {
  val hasMapperOrReducer = !(phases filter { p => 
    p.isInstanceOf[Mapper] || p.isInstanceOf[Reducer]
  } isEmpty)
   
  def validate = require(
    hasMapperOrReducer, "must contain a Mapper or Reducer"
  )
   
  /** FIXME -> `fugly`
   * generates json as
   * {"inputs":[["x","y"]],query:[{["link"|"map"|"reduce"]:{"language":"javascript","source":"function(){...}", "keep":[true|false]}}]}
   */
  def asJson = {
    validate
    
    def jsonifyMap(m:Map[String,Any]) =
      m.map((e) => (e._2 match { 
        case str:String => "\"%s\":\"%s\"" 
        case _ => "\"%s\":%s" 
      }).format(e._1, e._2)).mkString("{",",","}")
    
    // FIXME inputs can actually have 3 values: bucket, key, tag
    val json = new StringBuilder("{\"inputs\":")
    if(inputs.size == 1 && !inputs(0)._2.isDefined)
      json.append("\"%s\"" format(inputs(0)._1))
    else
      json.append("[").append(inputs.map(i => "[\"%s\",\"%s\"]".format(i._1, i._2.get)).mkString(",")).append("]")
    json.append(", \"query\":[")
    
    json.append((phases.map((p) => p match {
      case l:Linker => "{\"link\":%s}" format(jsonifyMap(l.q))
      case m:Mapper => "{\"map\":%s}" format(jsonifyMap(m.q))
      case r:Reducer => "{\"reduce\":%s}" format(jsonifyMap(r.q))
      case _ => ""
    })).mkString(","))
    json append("]}") toString
  }
}
 
/** map/reduce query building mixin */
trait Mapred {
  
  val mapredPath = Mapred.path
  
  /** abstract map reduce method */
  def mapred(q: Query):(String, Map[String,Set[String]])
}