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
    
    private [ryu] val http = new Http
    private [ryu] val headers =  Map("Content-Type" -> "application/json")
    private [ryu] val withBody = Map("returnbody" -> true) 
    private [ryu] val docHeaders = Seq("Link", "Date", "ETag", "Expires", "X-Riak-Vclock", "Content-Type")
    private [ryu] val bucHeaders = Seq("Link", "Date", "Expires", "Content-Type")
    
    val riak = :/(host, port)
    val raw = riak / "raw"
    
    def apply(bucket: Symbol) = http(
      (raw / bucket.name) >+ { *(_, bucHeaders) }
    )
    
    /**  save or update doc @return (doc,headers) */
    def apply(meta: ^, doc: String) = http(
      (raw / meta.bucket.name / meta.key <:< headers ++ meta.headers <<? withBody <<< doc.asJson) >+ { *(_, docHeaders) }
    )
    
    /** get doc */
    def apply(meta: ^) = http(
      (raw / meta.bucket.name / meta.key <:< headers) >+ { *(_, docHeaders) }
    )
    
    /** delete doc */
    def - (meta: ^)  = http(
      (raw / meta.bucket.name / meta.key DELETE) >|
    )
    
    /** walk doc links 
     * returns multipart/mixed Content-Type
     */
    def > (meta: ^, links: (Symbol, Option[String], Option[Boolean])*) = {
      def segments = ((List[String]() /: links) { (a,l) => 
         ("%s,%s,%s" format(l._1.name, l._2.getOrElse("_"), l._3.getOrElse("_"))) :: a
      }).reverse.mkString("/")
        
      http(
        (raw / meta.bucket.name / meta.key / segments) >+ { *(_, bucHeaders ++ docHeaders) }
      )
    }
    
    /** map/reduce */
    def mapred(q: Query) = http(
      (riak / mapredPath <:< headers << q.asJson) >+ { r =>
        (r as_str, r >:> { h => h })
      }
    )
    
    /** `splat` req handler to split response into (doc, headers) */
    private [ryu] def *(r: Handlers, keys: Seq[String]) =
      (r as_str, r >:> { h => h.filterKeys { keys.contains } })
  }
}

/** represents a link from on doc to another
 *  @param bucket
 *  @param key optional key to other doc
 *  @tag rel="up" if link to parent, riaktag="contained" if link to child,
 *       else riaktag="user defined tag"
 */
case class Link(bucket: Symbol, key:Option[String], tag: String) {
  def headerVal = key match {
    case None => "</raw/%s>; riaktag=\"%s\"" format(bucket.name, tag)
    case _ => "</raw/%s/%s>; riaktag=\"%s\"" format(bucket.name, key.get, tag)
  }
}

/**  document meta info */
case class ^ (bucket: Symbol, key: String, vclock: Option[String], links: Option[Seq[Link]]) {
  def headers = Map(
    "Link" -> links.getOrElse(Seq[Link]()).map(l => 
      l.headerVal
    ).mkString(", ")
  )
}

/** bucket meta into */
case class |^| (bucket: Symbol, allowMulti: Boolean, bigVclock: Int, chashKeyfun: (String, String), linkfun: (String, String), nVal: Int, oldVclock: Int, smallVclock: Int, youngVclock: Int)