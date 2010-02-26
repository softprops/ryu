package ryu

/** function for building riak requests */
object Ryu {
  def apply(host: String, port: Int) = new Ryu(host, port)
  
  /** riak request builder impl */
  private [ryu] class Ryu(host: String, post: Int) {
    import dispatch._
    
    private [ryu] val http = new Http
    private [ryu] val headers =  Map("Content-Type" -> "application/json")
    private [ryu] val withBody = Map("returnbody" -> true) 
    private [ryu] val docHeaders = Seq("Link", "Date", "ETag", "Expires", "X-Riak-Vclock")
    private [ryu] val bucHeaders = Seq("Link", "Date", "Expires")
    
    val raw = :/(host, post) / "raw"
    
    def apply(bucket: Symbol) = http(
      (raw / bucket.name) >+ { *(_, bucHeaders) }
    )
    
    /**  save or update doc @return (doc,headers) */
    def apply(meta: ^, doc: String) = http(
      (raw / meta.bucket.name / meta.key <:< headers <<? withBody <<< doc) >+ { *(_, docHeaders) }
    )
    
    /** get doc */
    def apply(meta: ^) = http(
      (raw / meta.bucket.name / meta.key <:< headers) >+ { *(_, docHeaders) }
    )
    
    /** delete doc */
    def - (meta: ^)  = http(
      (raw / meta.bucket.name / meta.key DELETE) >|
    )
    
    /** walk doc links */
    def > (meta: ^, links: (Symbol, Option[String], Option[Boolean])*) = {
      def segments = ((List[String]() /: links) { (a,l) => 
         ("%s,%s,%s" format(l._1.name, l._2.getOrElse("_"), l._3.getOrElse("_"))) :: a
      }).reverse.mkString("/")
        
      http(
        (raw / meta.bucket.name / meta.key / segments) >+ { *(_, bucHeaders ++ docHeaders) }
      )
    }
    
    /** `splat` req handler to split response into (doc, headers) */
    private [ryu] def *(r: Handlers, keys: Seq[String]) =
      (r as_str, r >:> { h => h.filterKeys { keys.contains } })
    
    /** map/reduce */
    //def apply(inputs: Seq[String], (map: Mapper, reduce: Reducer)*) = {}
  }
}

/** represents a link from on doc to another
 *  @param bucket
 *  @tag "up" if child doc else user defined tag
 */
case class Link(bucket: Symbol, tag: String) {
  def unapply(h:Map[String,Seq[String]]) = h match {
    // Link: </raw/hb>; rel="up", </raw/hb/fourth>; riaktag="foo"
    case _ => None
  }
}

/**  document meta info */
case class ^ (bucket: Symbol, key: String, vclock: Option[String], links: Option[Seq[Link]])

/** bucket meta into */
case class |^| (bucket: Symbol, allowMulti: Boolean, bigVclock: Int, chashKeyfun: (String, String), linkfun: (String, String), nVal: Int, oldVclock: Int, smallVclock: Int, youngVclock: Int)