# Search Engine for HKUST
This is a project we conducted for COMP4321: Search Engines for Web and Enterprise Data. We built a scraper and indexer in Java, and connect our system to a [RocksDB database](https://rocksdb.org/). We also built a frontend using [apache tomcat server](http://tomcat.apache.org/). 

## Notes

This Java project is managed by Maven so it's best to use a Maven to setup this project. It is preferred to use an IDE to make the execution process much easier.

Once you setup the project, you can directly compile and execute the ``com.comp4321.dao.code.Indexer.java`` file. By running the test program ``com.comp4321.dao.code.Indexer.java``, you will build up the spider that can fetch the webpages which can then be indexed, and will also output the ``spider_result.txt`` from the saved files in the database.

The ``com.comp4321.util.code.Crawler.java`` file contains the com.comp4321.util.code.Crawler class, a dependency for the com.comp4321.dao.code.Indexer class in ``com.comp4321.dao.code.Indexer.java``.
