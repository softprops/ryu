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
  
  /** one doc */
  type Doc = (String, scala.collection.Map[String, Set[String]])
  /** many docs :P */
  type MultiDoc = List[(String, dispatch.mime.Mime.Headers)]
  /** url base trio used for link walking map reduce queries [bucket],[key],[keep]*/
  type LinkSpec = (Option[Symbol], Option[String], Option[Boolean])
  
  /** Asynchronous Riak request executor */
  private [ryu] class AsyncRyu(host: String, port: Int) extends RyuBase(host, port) {
    import dispatch._
    import dispatch.futures.Futures
    import Http._
    
    protected [ryu] val http = new Http with Threads
    /** Callback fn for one document */
    type Callback = (Doc) => Any
    /** Callback fn for many documents */
    type MultiCallback = (MultiDoc) => Any
    
    /** Get bucket info */
    def apply(bucket: Symbol)(cb: Callback): Unit = http.future(
      get(bucket) ~> cb
    )
    
     /** Get a document */
    def apply(meta: ^)(cb: Callback): Unit = http.future(
      get(meta) ~> cb
    )
    
    /** Save or update a document @return (doc,headers) */
    def apply(meta: ^, doc: String)(cb: Callback): Unit = http.future(
      put(meta, doc) ~> cb
    )
    
    /** Save or update a file document @return (doc,headers) */
    //def apply(meta: ^, file: java.io.File, contentType: String)(cb: Callback): Unit = http.future(
    //  put(meta, file, contentType) ~> cb
    //)
    
    /** Delete a document */  
    def - (meta: ^) = http.future(
      delete(meta)
    )
    
    /** Walk across a Seq of document links */
    def > (meta: ^, links: Seq[(Option[Symbol], Option[String], Option[Boolean])])(cb: MultiCallback) = http.future(
      walk(meta, links) ~> cb
    )
  }
  
  /** Synchronous Riak request executor */
  private [ryu] class Ryu(host: String, port: Int) extends RyuBase(host, port) with Mapred {
    import dispatch._
    
    protected [ryu] val http = new Http
    
    /** @return a non-blocking interface */
    def ! = new AsyncRyu(host, port)
    
    /** Get a server's stats */
    def stats = http(serverStats)
    
    /** Get bucket info */
    def apply[T](bucket: Symbol) = http(
      get(bucket)
    )
    
    /** Save or update a document @return (doc,headers) */
    def apply(meta: ^, doc: String) = http(meta.key match {
      case null => post(meta, doc)
      case _ => put(meta, doc)
    })
    
    /** Save or update a file document @return (doc,headers) */
    def apply(meta: ^, file: java.io.File, contentType: String) = http(
      put(meta, file, contentType)
    )
    
    /** Save or update multiple documents @return List[(doc,headers)] */
    def ++ [T](kvs: Iterable[(^, String)]) = ((List[(String, scala.collection.Map[String, Set[String]])]() /: kvs) (
      (l, e) => apply(e._1, e._2) :: l
    )).reverse
    
    /** Get a document */
    def apply(meta: ^) = http(
      get(meta)
    )
      
    /** Delete a document */
    def - (meta: ^) = http(
      delete(meta)
    )
    
    /** Walk document links 
     * @return list of (doc, headers)
     */
    def > (meta: ^, links: (Option[Symbol], Option[String], Option[Boolean])*) = http(
      walk(meta, links)
    )   
    
    /** Submit a map/reduce query */
    def mapred(q: Query) = http(
      (riak / mapredPath <:< headers << q.asJson) >+ { r =>
        (r as_str, r >:> { h => h })
      }
    )
  }

  private [ryu] class RyuBase(host: String, port: Int) {
    import dispatch._
    import dispatch.mime.Mime._
    import scala.io.Source

    
    protected [ryu] val headers =  Map("Content-Type" -> "application/json", "X-Riak-ClientId" -> "ryu")
    protected [ryu] val withBody = Map("returnbody" -> true) 
    protected [ryu] def writes(n: Int) = Map("w" -> n)
    protected [ryu] def replicas(dw: Int) = Map("dw" -> dw)
    protected [ryu] val docHeaders = Seq("Link", "Date", "ETag", "Expires", "X-Riak-Vclock", "Content-Type", "Location")
    protected [ryu] val bucHeaders = Seq("Link", "Date", "Expires", "Content-Type")

    val riak = :/(host, port)
    val raw = riak / "riak"

    protected [ryu] def serverStats  = (riak / "stats") >+ { r =>
      (r as_str, r >:> { h => h })
    }

    protected [ryu] def get(bucket: Symbol) = (raw / bucket.name) >+ { *(_, bucHeaders) }

    /** TODO conditonal content-type handling binary vs string data */
    protected [ryu] def get(meta: ^) =
      (raw / meta.bucket.name / meta.key <:< headers) >+ { *(_, docHeaders) }

    protected [ryu] def post(meta: ^, doc: String) =
      (raw / meta.bucket.name <:< headers ++ meta.headers <<? withBody << doc.asJson) >+ { *(_, docHeaders) }

    protected [ryu] def put(meta: ^, file: java.io.File, contentType: String) =
      (raw / meta.bucket.name / meta.key <:< headers ++ meta.as(contentType).headers <<< (file, contentType)) >|

    protected [ryu] def put(meta: ^, doc: String) =
      (raw / meta.bucket.name / meta.key <:< headers ++ meta.headers <<? withBody <<< doc.asJson) >+ { *(_, docHeaders) }
    
    protected [ryu] def delete(meta: ^) = (raw / meta.bucket.name / meta.key DELETE) >|

    protected [ryu] def walk(meta: ^, links: Seq[LinkSpec]) = {
       def segments = ((List[String]() /: links) { (a,l) => 
           ("%s,%s,%s" format(l._1.getOrElse("_") match {
             case sym:Symbol => sym.name
             case str:String => str 
           }, l._2.getOrElse("_"), l._3 match {
             case Some(true) => 1
             case Some(false) => 0
             case _ => "_"
           })) :: a
        }).reverse.mkString("/")
        raw / meta.bucket.name / meta.key / segments >--> { (headers, stm) =>
          (Source.fromInputStream(stm).mkString, headers)
        }
    }

    /** `Splat` req handler to split response into (doc, headers) */
    protected [ryu] def *(r: Handlers, keys: Seq[String]) =
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
  def queryVal(keep:Boolean) = (Some(bucket), Some(tag), if(keep) Some(keep) else None)
}

/** Able to project self as a Link */
trait LinkLike { def asLink(tag: String): Link }

/** ^ companion */
object ^ {
  def apply(bucket: Symbol, key: String) = new ^(bucket, key)
  def apply(bucket: Symbol) = new ^(bucket)
}

/** document meta info */
case class ^ (bucket: Symbol, key: String, vclock: Option[String], links: Option[Seq[Link]], contentType: String) extends LinkLike {
  /** Use when creating a new meta object that does not yet have vclock, and link info */
  def this(bucket: Symbol, key: String) = this(bucket, key, None, None, "application/json")
  /** Specifically used when storing a new object without a key */
  def this(bucket: Symbol) = this(bucket, null)
  /** project self with new content type */
  def as(cType: String) = ^(
    bucket, key, vclock, links, cType
  )
  /** Header representation of attributes */
  def headers = Map(
    "Link" -> links.getOrElse(Seq[Link]()).map(l => 
      l.headerVal
    ).mkString(", "),
    "Content-Type" -> contentType
  )
  /** Project self as a Link object */
  def asLink(tag: String) = Link(bucket, Some(key), tag)
  /** @return new ^ with given Link l */
  def + (l: Link) = ^(
    bucket, key, vclock, Some(links getOrElse(Seq[Link]()) ++ Seq(l)), contentType
  )
}

/** bucket meta into */
case class |^| (bucket: Symbol, allowMulti: Boolean, bigVclock: Int, chashKeyfun: (String, String), linkfun: (String, String), nVal: Int, oldVclock: Int, smallVclock: Int, youngVclock: Int)