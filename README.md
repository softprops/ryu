# Ryu

A `Tornado Whirlwind Kick` scala client for the `riak` `raw` http interface

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

`links` are a navigation utility for traversing to other documents

`documents` have data
  
data is expect in jsón format

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
    
    // delete Sagat
    db - ^('fighters, "Sagat", None, None)

## install

TODO

## fork/knife

    git://github.com/softprops/ryu.git

## todo

* apply links when creating/updating
* oo json via lift-json
* tests!
* map/reduce

## issues

You got issues with ryu? Take them up directly with him here

    http://github.com/softprops/ryu/issues

## references

Ryu bows to those that came before

 - another high kicking scala riak client is `riakka`, a scala client for riaks `jiak` interface
 
2010 Doug Tangren (softprops)