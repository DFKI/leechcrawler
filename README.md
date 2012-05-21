Leech
=====

Crawling capabilities for Apache Tika. Crawl content out of e.g. file systems, http(s) sources (webcrawling) or imap(s) servers. Leech offers additional Tika parsers providing crawling capabilities.  
It is the RDF free successor of Aperture from the DFKI GmbH Knowledge Management group.

The key intentions of Leech:
* Ease of use - crawl a data source with a few lines of code.
* Low learning curve - Leech integrates seamlessly into the Tika world.
* Extensibility - write your own crawlers, support new data source protocols and plug them in by simply adding your jar into the classpath.
* All parsing capabilities from Apache Tika are supported, including your own parsers.
* Incremental crawling, offered for existing and new crawlers.

***

Crawl something incrementally in 1 minute:

    String strSourceUrl = "URL4FileOrDirOrWebsiteOrImapfolderOrImapmessageOrSomething";

    Leech leech = new Leech();
    CrawlerContext crawlerContext = new CrawlerContext();
    crawlerContext.setIncrementalCrawlingHistoryPath("./history/4SourceUrl");
    leech.parse(strSourceUrl, new DataSinkContentHandlerAdapter()
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
    }, crawlerContext.createParseContext());
