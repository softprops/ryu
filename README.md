# Ryu

A `Tornado Whirlwind Kick` scala client for the [riak](http://riak.basho.com/) `raw` http interface

Riak

* is a distributed json document db
* documents are stored in buckets
* documents can be linked to other documents
* queries come in 2 flavors
  * `link walking`
  * map/reduce

Scala

* is awesome

## fight

### quick example

    import ryu._
    
    val db = Ryu("localhost", 8098)
    
    // store fighters
    db(^('fighters, "Ken", None, None), """{"fighting-style":"Shotokan"}""")
    db(^('fighters, "Ryu", None, None), """{"fighting-style":"Shotokan"}""")
    db(^('fighters, "Chun-Li", None, None), """{"fighting-style":"Chūgoku Kenpō"}""")
    
    // now round up the fighters!
    val (props, headers) = db(`fighters)
    
### baby steps

documents have meta info

    val meta = ^(`bucket, "key", Some("vclock"), Some(Seq(links)))

Did I just misspell 'clock'?

No. `vclocks` function similarly to the http `vtag` header in that they represent versions of documents. `Riak` uses these to avoid collisions.

a `bucket` is a container for yer data

a `key` is the key by which you refer to your value silly

a `vclock` is a unique hash of your document version

`links` are a navigation utility used for traversing to other documents

`documents` have data (expected flavor is jsón)

### more operations

    // ref riak
    val db = Ryu(host, port)
    
    // ref a key for dan
    val dan = ^('fighters, "dan", None, Some(Seq(Link('fighters, "Sagat"))))
    
    // get dan
    db(dan)
    
    // save or update dan
    db(dan, """{"gi":"pink"}""")
    
    // walk over to Sagat
    db > (dan, (`fighters, Some("Sagat"), Some(true)))
    
    // delete (defeat) Sagat
    db - ^('fighters, "Sagat", None, None)

    // submit m/r job to find all fighter names
    db mapred(
      Query(Seq(("fighters", None)), Seq(
          Mapper named("Riak.mapValuesJson"),
          Reducer source("""
            function(values, arg) { 
              var names = []; values.forEach(v){ names.push(v["name"]); } 
              return names;
            }
          } """.stripMargin.trim),
      ))
    )
    
    // validate m/r query
    Query(Seq("fighters"), Seq(
        Linker("fighters", "dan")
    )).validate // IllegalArgumentException (no mapper or reducer!)

## install

TODO erl/riak instructions
TODO mvn repo

## fork/knife

    git://github.com/softprops/ryu.git

## goals

* provide a k-v api similar to a Map
* follow `dispatch` idioms

## todo

* apply links when creating/updating
* oo json via lift-json
* more test converage
* handle multipart/mixed response

## issues

You got issues with ryu? Take them up directly with him here

    http://github.com/softprops/ryu/issues

## references

[riakka](http://github.com/timperrett/riakka) is another high kicking scala client for riaks `jiak` interface (Ryu bows to those that came before)
 
http://riak.basho.com/programming.html - an overview of Riak's http interface 

http://blog.basho.com/2010/02/24/link-walking-by-example/ - walking the link

http://blog.basho.com/2010/02/03/the-release-riak-0.8-and-javascript-map/reduce/ - riak map/reduce cartography

http://vimeo.com/9188550 - riak map/reduce video

 
2010 Doug Tangren (softprops)