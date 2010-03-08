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

documents have meta info which ryu uses as keys to access documents

    val meta = ^(`bucket, "key", Some("vclock"), Some(
      Seq(Link('bucket, Some("otherkey"), "linkTag"))
    ))

Did I just misspell 'clock'?

No. `vclocks` function similarly to the http `vtag` header in that they represent versions of documents. `Riak` uses these to avoid collisions.

a `bucket` is a container for yer data

a `key` is the key by which you refer to your value silly

a `vclock` is a unique hash of your document version

`links` are a navigation utility used for traversing to other documents (think html anchors)

`documents` have data (expected flavor is jsón)

### more operations

    import ryu._
    
    // ref riak
    val db = Ryu(host, port)
    
    // ref a key for sagat
    val sagat = ^('fighters, "sagat", None, None)
    
    // create a link to sagat 
    val sagatLink = sagat asLink("boss")
    
    // ref a key for dan
    val dan = ^('fighters, "dan", None, Some(Seq(sagatLink)))
    
    // save or update sagat
    db(sagat, """{"hp":10}""")
    
    // save or update dan
    db(dan, """{"hp":100}""")
    
    // get dan
    db(dan)
    
    // dan walks over to Sagat
    db > (dan, sagatLink.queryVal(true))
  
    // submit m/r job to find all fighter hp's
    db mapred(
      Query(Seq(("fighters", None)), Seq(
          Mapper named("Riak.mapValuesJson") keep(false),
          Reducer source("function(values){ var hps = []; values.forEach(function(v){ hps.push(v['hp']); }); return hps; }"),
      ))
    )
    
    // delete (defeat) Sagat
    db - sagat
    
    // validate m/r query
    Query(Seq(("fighters",None)), Seq(
        Linker tag("dan")
    )).validate // IllegalArgumentException (must contain a Mapper or Reducer)

## install

install [erlang](http://gist.github.com/302327)

download and install [riak](http://bitbucket.org/basho/riak/get/riak-0.8.tar.gz) 0.8 or later (just follow the readme)

TODO mvn repo

## fork/knife

contribute git://github.com/softprops/ryu.git

## goals

* provide a k-v api similar to a Map
* follow [dispatch](http://github.com/softprops/Databinder-Dispatch) idioms

## todo

* extract Link objects when fetching documents
* oo json via lift-json
* more test coverage
* cleaner api

## issues

You got issues with ryu? Take them up directly with him [here](http://github.com/softprops/ryu/issues).

## references

[riakka](http://github.com/timperrett/riakka) is another high kicking scala client for riaks `jiak` interface (Ryu bows to those that came before)
 
[an overview of Riak's http interface](http://riak.basho.com/programming.html) 

[walking the link](http://blog.basho.com/2010/02/24/link-walking-by-example/)

[riak map/reduce cartography](http://blog.basho.com/2010/02/03/the-release-riak-0.8-and-javascript-map/reduce/)

[riak map/reduce video](http://vimeo.com/9188550)

[riak server config](http://riak.basho.com/basic-setup.html)
 
2010 Doug Tangren (softprops)