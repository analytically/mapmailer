mapmailer [![Build Status](https://travis-ci.org/analytically/mapmailer.png)](https://travis-ci.org/analytically/mapmailer)
=========

Select CRM contacts to email by drawing an area on a map. Built using [Play Framework 2.2](http://www.playframework.org) (Scala).
Follow [@analytically](http://twitter.com/analytically) for updates. UNDER ACTIVE DEVELOPMENT

Works with:
  - [Capsule CRM](http://www.capsulecrm.com/)

![screenshot](screenshot.png)

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

### Background and usecase

This software was built for [Coen Recruitment](http://www.coen.co.uk/), an education recruitment agency in the UK. Since
they prioritise on location and endeavour to find teachers work close to home, their consultants need map area selection
to market teachers to schools efficiently. Parts of this project are based on [CamelCode](https://github.com/analytically/camelcode).

### License

Licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

Copyright 2014 [Mathias Bogaert](mailto:mathias.bogaert@gmail.com).