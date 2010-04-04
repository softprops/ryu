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
    db(^('fighters, "Ken"), """{"fighting-style":"Shotokan"}""")
    db(^('fighters, "Ryu"), """{"fighting-style":"Shotokan"}""")
    db(^('fighters, "Chun-Li"), """{"fighting-style":"Chūgoku Kenpō"}""")
    
    // now round up the fighters!
    val (props, headers) = db(`fighters)
    
### baby steps

documents have meta info which ryu uses as keys to access documents

    val meta = ^('bucket, "key", Some("vclock"), Some(
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
    val db = Ryu("localhost", 8098)
    
    // ref a key for sagat
    val sagat = ^('fighters, "sagat")
    
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
  
    // submit map reduce job to find all fighter hp's
    db mapred(
      Query(Seq(("fighters", None, None)), Seq(
          Mapper named("Riak.mapValuesJson") keep(false),
          Reducer source("function(values){ var hps = []; values.forEach(function(v){ hps.push(v['hp']); }); return hps; }"),
      ))
    )
    
    // delete (defeat) Sagat
    db - sagat
    
    // validate m/r query
    Query(Seq(("fighters", None, None)), Seq(
        Linker tag("dan")
    )).validate // IllegalArgumentException (must contain a Mapper or Reducer)

### A Ryu that doesn't have to block to win

Ryu also comes with an asynchronous interface for most ryu methods that accepts a callback for the response object.

    // ! is for asynchonisity!
    val db = Ryu("localhost", 8098) ! 

    db(^('fighters, "ken"), "punch later") { ken => 
      println(ken._1) 
    }
    println("kick") 
    // > kick
    // > punch later
    

The asynchronous interfaces are `curry flavored` so you can bind the request to be handled later

    val db = Ryu("localhost", 8098) !
    
    val curriedPunch = db(^('fighters, "ken"), "punch later")_
    
    println("kick, kick, punch, kick")
    
    // time to fight back!
    curriedPunch { ken =>
      println(ken._1)
    }

## install

* riak dependencies
  * install [erlang](http://gist.github.com/302327)
  * download and install [riak](http://downloads.basho.com/riak/) 0.9.1 or later

* ryu
  * TODO mvn repo

## fork or knife ryu to fight back

contribute git://github.com/softprops/ryu.git

## goals

* provide a persisent k-v api with an interface akin to a Map
* follow [dispatch](http://github.com/softprops/Databinder-Dispatch) idioms

## todo

* all methods return Option values `db(^('fighters, "dan")).getOrElse(default)`
* extract Link objects when fetching documents
* module for json <-> string conversions
* use keep-alive for multi stage processing
* finalize api

## issues

You got issues with ryu? Take them up directly with him [here](http://github.com/softprops/ryu/issues).

## references

[riakka](http://github.com/timperrett/riakka) is another high kicking scala client for riaks `jiak` interface (Ryu bows to those that came before)
 
[riak rest api](https://wiki.basho.com/display/RIAK/REST+API)

[walking the link](http://blog.basho.com/2010/02/24/link-walking-by-example/)

[riak map/reduce cartography](http://blog.basho.com/2010/02/03/the-release-riak-0.8-and-javascript-map/reduce/)

[riak map/reduce video](http://vimeo.com/9188550)
 
2010 Doug Tangren (softprops)