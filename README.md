mapmailer [![Build Status](https://travis-ci.org/analytically/mapmailer.png)](https://travis-ci.org/analytically/mapmailer)
=========

Email CRM contacts by drawing (polygon and circle) an area on a map. Built using [Play Framework 2.2](http://www.playframework.org) (Scala).
Follow [@analytically](http://twitter.com/analytically) for updates.

![screenshot](screenshot.png)

Works with:
  - [Capsule CRM](http://www.capsulecrm.com/)

### Requirements

- Java 6 or later
- [Play Framework 2.2.1](http://www.playframework.org)
- [MongoDB](http://www.mongodb.org)

### Setup

Edit `conf/application.conf` and point it to a MongoDB installation (defaults to `localhost:27017`), add CRM details, and execute

``` sh
play run
```

Then drop the [CodePoint Open CSV](https://www.ordnancesurvey.co.uk/opendatadownload/products.html) (scroll halfway down, 20mb)
files in the `codepointopen` directory.

After each file is imported, it will be moved to the `codepointopen/done` directory.

Then visit [http://localhost:9000](http://localhost:9000) and you should see the welcome screen.

### Technology

* [Play Framework 2.2.1](http://www.playframework.org), as web framework
* [Apache Camel](http://camel.apache.org) to [process and monitor](https://github.com/analytically/mapmailer/blob/master/app/Global.scala#L34) the `codepointopen` directory and to tell the actors about the postcodes
* [Akka](http://akka.io) provides a nice concurrency model [to process the 1.7 million postcodes](https://github.com/analytically/mapmailer/blob/master/app/actors/actors.scala#L41) in under one minute on modern hardware
* [GeoTools](http://www.geotools.org) converts the eastings/northings to latitude/longitude
* [ReactiveMongo](http://reactivemongo.org/) is a scala MongoDB driver that provides fully non-blocking and asynchronous I/O operations
* [MongoDB](http://www.mongodb.org) as database with two-dimensional geospatial indexes (see [Geospatial Indexing](http://www.mongodb.org/display/DOCS/Geospatial+Indexing))
* [Leaflet](http://leafletjs.com/) for the map
* [Bootstrap](http://getbootstrap.com/) and [Font Awesome](http://fortawesome.github.com/Font-Awesome/) for the UI

### Background and usecase

This software was built for [Coen Recruitment](http://www.coen.co.uk/), an education recruitment agency in the UK. Since
they prioritise on location and endeavour to find teachers work close to home, their consultants need map area selection
to market teachers to schools efficiently. Parts of this project are based on [CamelCode](https://github.com/analytically/camelcode).

### License

Licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

Copyright 2014 [Mathias Bogaert](mailto:mathias.bogaert@gmail.com).