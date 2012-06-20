package de.dfki.km.leech.lucene;



import java.io.File;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;
import org.junit.Test;

import de.dfki.km.leech.Leech;
import de.dfki.km.leech.parser.wikipedia.WikipediaDumpParser;
import de.dfki.km.leech.sax.CrawlReportContentHandler;
import de.dfki.km.leech.sax.PrintlnContentHandler;
import de.dfki.km.leech.sax.PrintlnContentHandler.Verbosity;
import de.dfki.km.leech.util.StopWatch;



public class ToLuceneContentHandlerTest
{

    @Test
    public void test() throws Exception
    {
        System.out.println("test crawling wikipedia file");

        // File fWikiDump = new File("resource/testData_wikipedia/de_wikidump_mit_header_short_test4allocation.xml");
        // File fWikiDump = new File("resource/testData_wikipedia/de_wikidump_mit_header_short.xml");

        File fWikiDump = new File("resource/testData_wikipedia/dewiki-latest-pages-articles.xml");
        // File fWikiDump = new File("resource/testData_wikipedia/enwiki-latest-pages-articles.xml");



        Leech leech = new Leech();

        long lStart = StopWatch.stopAndPrintTime();

        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_36, new LeechSimpleAnalyzer());
        config.setOpenMode(OpenMode.CREATE);

        IndexWriter indexWriter = new IndexWriter(new SimpleFSDirectory(new File("./luceneIndex_wikipedia")), config);



        ToLuceneContentHandler toLuceneContentHandler =
                new ToLuceneContentHandler(WikipediaDumpParser.getFieldConfig4ParserAttributes(), indexWriter);

        CrawlReportContentHandler reportContentHandler =
                new CrawlReportContentHandler(new PrintlnContentHandler(Verbosity.title, toLuceneContentHandler).setShowOnlyErrors(true));



        leech.parse(fWikiDump, reportContentHandler.setCyclicReportPrintln(7000));
        
        System.out.println(reportContentHandler.getReport());


        indexWriter.close(true);
        StopWatch.stopAndPrintDistance(lStart);
        System.out.println("..finished crawling wikipedia file");
    }
}
