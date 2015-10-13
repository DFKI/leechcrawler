LeechCrawler
=====

Crawling capabilities for Apache Tika. Crawl content out of e.g. file systems, http(s) sources (webcrawling) or imap(s) servers. LeechCrawler offers additional Tika parsers providing these crawling capabilities.  
It is the RDF free successor of Aperture from the DFKI GmbH Knowledge Management group. In the case you want to make a project with us, feel free to [contact us](https://github.com/leechcrawler/leech/blob/master/people.md)!

LeechCrawler is published under the [3-Clause BSD License](http://opensource.org/licenses/BSD-3-Clause), Owner/Organization: [DFKI GmbH](http://www.dfki.de), 2013.

The key intentions of LeechCrawler:
* Ease of use - crawl a data source with a few lines of code.
* Low learning curve - Leech integrates seamlessly into the Tika world.
* Extensibility - write your own crawlers, support new data source protocols and plug them in by simply adding your jar into the classpath.
* All parsing capabilities from Apache Tika are supported, including your own parsers.
* Incremental crawling (second run crawls only the differences inside a data source, according to the last crawl). Offered for existing and new crawlers.
* Create easily Lucene and SOLR indices.

***
[How to start](https://github.com/leechcrawler/leech/blob/master/how2start.md) | [Code snippets / Examples](https://github.com/leechcrawler/leech/blob/master/codeSnippets.md) | [Extending LeechCrawler](https://github.com/leechcrawler/leech/blob/master/extending.md) | [Mailing list](https://github.com/leechcrawler/leech/blob/master/mailinglist.md) | [People / Contact] (https://github.com/leechcrawler/leech/blob/master/people.md) | [Supporters](https://github.com/leechcrawler/leech/blob/master/supporters.md)
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
        public void processErrorData(Metadata metadata)
        {
        }
    }, crawlerContext.createParseContext());
