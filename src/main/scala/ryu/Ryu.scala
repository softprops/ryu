package ryu

object Ryu {
  /** function for building riak requests (riak default is localhost, 8098) */
  def apply(host: String, port: Int) = new Ryu(host, port)
  
  /** Link extractor */
  object Link_? {
    val Header = """<\/raw\/(\w.+)>;\s*(rel|riaktag)=\"(\w.+)\" """.trim.r
    def unapply(h: String): Option[Link] = h match {
      case Header(path, t, tag) => t match {
        case "riaktag" | "rel" => {
          val parts = path.split("/")
          if(parts.size > 1) Some(Link(Symbol(parts(0)),Some(parts(1)), tag))
          else Some(Link(Symbol(path), None, tag))
        }
        case _ => None
      }
    }
  }
  
  object Js {
    implicit def any2Json(obj: Any) = new Jsonifier(obj)
    private [ryu] class Jsonifier(obj: Any) {
      /** huge TODO convert objects jsón strings */
      def asJson: String = obj match {
        case _ => obj.toString
      }
    }
  }
  import Js._
  
  
  /** riak request builder impl */
  private [ryu] class Ryu(host: String, port: Int) extends Mapred {
    import dispatch._
    import dispatch.mime.Mime._
    import scala.io.Source
    
    private [ryu] val http = new Http
    private [ryu] val headers =  Map("Content-Type" -> "application/json", "X-Riak-ClientId" -> "ryu")
    private [ryu] val withBody = Map("returnbody" -> true) 
    private [ryu] val docHeaders = Seq("Link", "Date", "ETag", "Expires", "X-Riak-Vclock", "Content-Type")
    private [ryu] val bucHeaders = Seq("Link", "Date", "Expires", "Content-Type")
    
    val riak = :/(host, port)
    val raw = riak / "riak"
    
    /** Get a server's stats */
    def stats = http(
      (riak / "stats") >+ { r =>
        (r as_str, r >:> { h => h })
      }
    )
    
    /** Get bucket info */
    def apply(bucket: Symbol) = http(
      (raw / bucket.name) >+ { *(_, bucHeaders) }
    )
    
    /** Save or update a document @return (doc,headers) */
    def apply(meta: ^, doc: String) = http(
      (raw / meta.bucket.name / meta.key <:< headers ++ meta.headers <<? withBody <<< doc.asJson) >+ { *(_, docHeaders) }
    )
    
    /** Save or update multiple documents @return List[(doc,headers)] */
    def ++ (kvs: Iterable[(^, String)]) = ((List[(String, scala.collection.Map.Projection[String, Set[String]])]() /: kvs) (
      (l, e) => apply(e._1, e._2) :: l
    )).reverse
    
    /** Get a document */
    def apply(meta: ^) = http(
      (raw / meta.bucket.name / meta.key <:< headers) >+ { *(_, docHeaders) }
    )
    
    /** Delete a document */
    def - (meta: ^)  = http(
      (raw / meta.bucket.name / meta.key DELETE) >|
    )
    
    /** Walk document links 
     * @return list of (doc, headers)
     */
    def > (meta: ^, links: (Symbol, Option[String], Option[Boolean])*) = {
      def segments = ((List[String]() /: links) { (a,l) => 
         ("%s,%s,%s" format(l._1.name, l._2.getOrElse("_"), l._3.getOrElse("_"))) :: a
      }).reverse.mkString("/")
      
      http(
        raw / meta.bucket.name / meta.key / segments >--> { (headers, stm) =>
          (Source.fromInputStream(stm).mkString, headers)
        }
      )
    }
    
    /** Submit a map/reduce query */
    def mapred(q: Query) = http(
      (riak / mapredPath <:< headers << q.asJson) >+ { r =>
        (r as_str, r >:> { h => h })
      }
    )
    
    /** `Splat` req handler to split response into (doc, headers) */
    private [ryu] def *(r: Handlers, keys: Seq[String]) =
      (r as_str, r >:> { h => h.filterKeys { keys.contains } })
  }
}

/** Represents a link from on doc to another
 *  @param bucket
 *  @param key optional key to other doc
 *  @tag rel="up" if link to parent, riaktag="contained" if link to child,
 *       else riaktag="user defined tag"
 */
case class Link(bucket: Symbol, key:Option[String], tag: String) {
  
  /** @return Header representation of self */
  def headerVal = key match {
    case None => "</riak/%s>; riaktag=\"%s\"" format(bucket.name, tag)
    case _ => "</riak/%s/%s>; riaktag=\"%s\"" format(bucket.name, key.get, tag)
  }
  
  /** @return walk query representation of self */
  def queryVal(keep:Boolean) = (bucket, Some(tag), if(keep) Some(keep) else None)
}

/** Able to project self as a Link */
trait LinkLike { 
  def asLink(tag: String): Link
}

/**  document meta info */
case class ^ (bucket: Symbol, key: String, vclock: Option[String], links: Option[Seq[Link]]) extends LinkLike {
  def headers = Map(
    "Link" -> links.getOrElse(Seq[Link]()).map(l => 
      l.headerVal
    ).mkString(", ")
  )
  
  def asLink(tag: String) = Link(bucket, Some(key), tag)
  
  /** @return new ^ with given Link l */
  def + (l: Link) = ^(
    bucket, key, vclock, Some(links getOrElse(Seq[Link]()) ++ Seq(l))
  )
}

/** bucket meta into */
case class |^| (bucket: Symbol, allowMulti: Boolean, bigVclock: Int, chashKeyfun: (String, String), linkfun: (String, String), nVal: Int, oldVclock: Int, smallVclock: Int, youngVclock: Int)