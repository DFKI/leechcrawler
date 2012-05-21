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

    new Leech().parse("sourceUrl", new PrintlnContentHandler(Granularity.title, new MyDataSinkContentHandler()), new ParseContext());
    new Leech().parse("sourceUrl", new PrintlnContentHandler(Granularity.all).setShowOnlyErrors(true), new ParseContext());

**Generate a crawl report**
  
    CrawlReportContentHandler reportContentHandler = new CrawlReportContentHandler(new MyDataSinkContentHandler());
    new Leech().parse("sourceUrl", reportContentHandler, new ParseContext());
    System.out.println(reportContentHandler.getReport());

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

