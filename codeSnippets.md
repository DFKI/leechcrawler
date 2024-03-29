***
**[How to start](https://github.com/leechcrawler/leech/blob/master/how2start.md) | [Code snippets / Examples](https://github.com/leechcrawler/leech/blob/master/codeSnippets.md) | [Extending LeechCrawler](https://github.com/leechcrawler/leech/blob/master/extending.md) | [Mailing list](https://github.com/leechcrawler/leech/blob/master/mailinglist.md) | [People/Legal Information](https://github.com/leechcrawler/leech/blob/master/people.md) | [Supporters](https://github.com/leechcrawler/leech/blob/master/supporters.md)| [Data Protection](https://github.com/leechcrawler/leech/blob/master/dataprotection.md)**
***

# Code snippets / examples

**Crawl something**  

    //examples: 'file://myDataDir', 'file://bla.pdf', 'http://reuschling.github.com/leech/',
    //'imap://usr:pswd@myImapServer.de:993/inbox', 'imaps://usr:pswd@myImapServer.de:993/inbox;uid=22'  
    String strSource = "resource/testData";  
    
    Leech leech = new Leech();
    leech.parse(strSource, new DataSinkContentHandlerAdapter()
    {
        public void processNewData(Metadata metadata, String strFulltext)
        {
            System.out.println("Extracted metadata:\n" + metadata + "\nExtracted fulltext:\n" + strFulltext);
        }
        public void processModifiedData(Metadata metadata, String strFulltext)
        {
        }
        public void processRemovedData(Metadata metadata)
        {
        }
        public void processErrorData(Metadata metadata)
        {
        }
    }, new ParseContext());  
    
**Set a configuration object / parse context to configure crawling behaviour**  

    ParseContext parseContext = new ParseContext();
    parseContext.set(CrawlerContext.class, new CrawlerContext());  //for general configuration, valid for all types of crawling
    parseContext.set(DirectoryCrawlerContext.class, new DirectoryCrawlerContext());  //to configure directory crawling
    parseContext.set(HtmlCrawlerContext.class, new HtmlCrawlerContext());  //to configure web crawling
    parseContext.set(ImapCrawlerContext.class, new ImapCrawlerContext());  //to configure imap server message crawling  

**Enable incremental parsing by setting a history path**  

    new CrawlerContext().setIncrementalCrawlingHistoryPath("./history/4SourceUrl");
    
    
**Stay inside a specific web domain and prune recursive depth**  

    URLFilter urlFilter = new SubstringPattern("http://www.dfki.de", SubstringPattern.STARTS_WITH).toURLFilterAsInclude();
    new CrawlerContext().setURLFilter(urlFilter).setCrawlingDepth(2);

**Print out the crawled data entities**  

    new Leech().parse("sourceUrl", new PrintlnContentHandler(Verbosity.title, new MyDataSinkContentHandler()), new ParseContext());
    new Leech().parse("sourceUrl", new PrintlnContentHandler(Verbosity.all).setShowOnlyErrors(true), new ParseContext());

**Generate a crawl report**
  
    CrawlReportContentHandler reportContentHandler = new CrawlReportContentHandler(new MyDataSinkContentHandler());
    new Leech().parse("sourceUrl", reportContentHandler, new ParseContext());
    System.out.println(reportContentHandler.getReport());
    
**Create a Lucene index**

**CAUTION**: We ship a LuceneContentHandler for `Lucene 6.4.2`, the Maven dependency is NOT per default in the pom.xml (marked as provided). We made this choice to get independent from the Lucene dependency. If you want to use the LuceneContentHandler for `Lucene 6.4.2`, add maven dependencies for `lucene-core`, `lucene-analyzers-common` and `lucene-queryparser`. It is also just easy to implement an own Lucene data sink by implementing an own [DataSinkContentHandler](https://github.com/leechcrawler/leech/blob/master/src/main/java/de/dfki/km/leech/sax/DataSinkContentHandlerAdapter.java).
      
    // we use a simple, preconfigured Field configuration here. Modify it for your own fields if necessary
    FieldConfig fieldConfig4Wikipedia = WikipediaDumpParser.getFieldConfig4ParserAttributes();
    // we create a lucene configuration for the index writer
    IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_CURRENT, fieldConfig4Wikipedia.createAnalyzer());
    config.setOpenMode(OpenMode.CREATE);
    // the Lucene index writer
    IndexWriter indexWriter = new IndexWriter(new SimpleFSDirectory(new File("./luceneIndex")), config);

    CrawlReportContentHandler reportContentHandler = new CrawlReportContentHandler(
            new PrintlnContentHandler(Verbosity.all, 
            new ToLuceneContentHandler(fieldConfig4Wikipedia, indexWriter)).setShowOnlyErrors(true));

    new Leech().parse("sourceUrl", reportContentHandler, new CrawlerContext().createParseContext());

    indexWriter.close(true);

**Crawl into SOLR**

**Caution** We ship the ToSolrContentHandler for Solrj 5.2.1, the Maven dependency is also NOT per default in the pom.xml (marked as provided). We made this choice to get independent from this dependency. If you want to use this ContentHandler include `<groupId>org.apache.solr</groupId> <artifactId>solr-solrj</artifactId>  <version>5.2.1</version>` to your dependencies.

    // the url(s) to the solr server. In the case cloudSolrClient is true, this is a list of zookeeper servers. In the case it is false, its the URL of the solr server
    String solrUrl = "http://localhost:8014/solr/ourCollection";
    // true: the class will create a CloudSolrClient instance. false: creation of ConcurrentUpdateSolrClient
    bCloudSolrClient=false;
    // only necessary if the CloudSolrClient is used. If you use ConcurrentUpdateSolrClient, specify it either in the solrUrl *OR* here (not both). Null or empty values are possible
    collection=null;
    
    CrawlReportContentHandler reportContentHandler = new CrawlReportContentHandler(
            new PrintlnContentHandler(Verbosity.all, 
            new ToSolrContentHandler(solrUrl, bCloudSolrClient, collection)).setShowOnlyErrors(true));

    new Leech().parse("sourceUrl", reportContentHandler, new CrawlerContext().createParseContext());
    

**Crawl into ElasticSearch**

LeechCrawler will send bulk HTTP calls, currently tested against ES 7.14.0.

    CrawlReportContentHandler reportContentHandler = new CrawlReportContentHandler(
            new PrintlnContentHandler(PrintlnContentHandler.Verbosity.title, 
            new ToElasticSearchContentHandler().server("localhost").port(9200).index("myIndexName").bulkSize(100))
            .setShowOnlyErrors(true)).setCyclicReportPrintln(1000*60)
    new Leech().parse("sourceUrl", reportContentHandler, new CrawlerContext().createParseContext());

**Stop current crawl**

    final CrawlerContext crawlerContext = new CrawlerContext();
    Thread stop = new Thread(new Runnable()
        {
            public void run()
            {
                try
                {
                    Thread.sleep(2000);
                    System.out.println("request stop (blocking)");
                    crawlerContext.requestStop();
                    System.out.println("stop finished");
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }

        }, "stop thread");

    stop.start();
    
    new Leech().parse("sourceUrl", new PrintlnContentHandler(Granularity.title), crawlerContext.createParseContext());

