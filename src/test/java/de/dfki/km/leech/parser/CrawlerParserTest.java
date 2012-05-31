/*
    Leech - crawling capabilities for Apache Tika
    
    Copyright (C) 2012 DFKI GmbH, Author: Christian Reuschling

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

    Contact us by mail: christian.reuschling@dfki.de
*/

package de.dfki.km.leech.parser;



import java.io.File;
import java.io.IOException;

import javax.mail.URLName;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import de.dfki.km.leech.Leech;
import de.dfki.km.leech.config.CrawlerContext;
import de.dfki.km.leech.config.ImapCrawlerContext;
import de.dfki.km.leech.parser.filter.SubstringPattern;
import de.dfki.km.leech.parser.filter.URLFilter;
import de.dfki.km.leech.sax.CrawlReportContentHandler;
import de.dfki.km.leech.sax.DataSinkContentHandler;
import de.dfki.km.leech.sax.DataSinkContentHandlerAdapter;
import de.dfki.km.leech.sax.PrintlnContentHandler;
import de.dfki.km.leech.sax.PrintlnContentHandler.Granularity;
import de.dfki.km.leech.util.StopWatch;



public class CrawlerParserTest
{

    protected class StopRunnable implements Runnable
    {

        @Override
        public void run()
        {
            try
            {
                Thread.sleep(2000);

                System.out.println("request stop");
                m_crawlerContext.requestStop();

                System.out.println("stopped finished");
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }

        }
    }



    public static void main(String[] args) throws Exception
    {
        // new DirectoryCrawlerParserTest().testCrawlDirectory();

        // new CrawlerParserTest().testCrawlDirectoryTree();

        new CrawlerParserTest().testCrawlWeb();


        // new DirectoryCrawlerParserTest().testCrawlDirectoryWithStop();


    }



     CrawlerContext m_crawlerContext;



    @Before
    public void setUp() throws Exception
    {
    }



    @After
    public void tearDown() throws Exception
    {
    }



    @Test
    public void testCrawlDirectory() throws Exception
    {

        System.out.println("test inkremental indexing");

        // die alte history wird gelöscht
        File fHistoryDir = new File("./history/forResourceDir");
        if(!fHistoryDir.exists()) fHistoryDir.mkdirs();

        for (File fSubFile : fHistoryDir.listFiles())
            fSubFile.delete();

        File fBla = new File("resource/testData_short/blaRenamed.txt");
        if(fBla.exists()) fBla.renameTo(new File("resource/testData_short/bla.txt"));


        Leech leech = new Leech();


        Granularity bVerbose = Granularity.nothing;


        CrawlerContext crawlerContext = new CrawlerContext().setIncrementalCrawlingHistoryPath("./history/forResourceDir");

        System.out.println("§§§ will index the very first time");

        CrawlReportContentHandler reportContentHandler = new CrawlReportContentHandler(new PrintlnContentHandler(bVerbose));
        leech.parse(new File("resource/testData_short"), reportContentHandler, crawlerContext.createParseContext());
        System.out.println(reportContentHandler.getReport());


        System.out.println("\n\n§§§ will index the second time (nothing should be seen)");

        reportContentHandler = new CrawlReportContentHandler(new PrintlnContentHandler(bVerbose));
        leech.parse(new File("resource/testData_short"), reportContentHandler, crawlerContext.createParseContext());
        System.out.println(reportContentHandler.getReport());


        System.out.println("\n\n§§§ will index the third time (bla.txt renamed - one removed, one new entity)");

        fBla = new File("resource/testData_short/bla.txt");
        fBla.renameTo(new File("resource/testData_short/blaRenamed.txt"));

        reportContentHandler = new CrawlReportContentHandler(new PrintlnContentHandler(bVerbose));
        leech.parse(new File("resource/testData_short"), reportContentHandler, crawlerContext.createParseContext());
        System.out.println(reportContentHandler.getReport());


        System.out.println("\n\n§§§ will index the forth time (single unmodified file, CheckForRemovedEntities(false) - nothing should be seen)");

        crawlerContext.setCheckForRemovedEntities(false);
        reportContentHandler = new CrawlReportContentHandler(new PrintlnContentHandler(bVerbose));
        leech.parse(new File("resource/testData_short/blaRenamed.txt"), reportContentHandler, crawlerContext.createParseContext());
        System.out.println(reportContentHandler.getReport());






        System.out.println("..finished crawling directory");
    }



    @Test
    public void testCrawlDirectoryTree() throws Exception
    {







        String strRoot = "resource/testData";
        // String strRoot = "/home/reuschling";

        System.out.println("test inkremental indexing on directory tree " + strRoot);
        long startTime = StopWatch.stopAndPrintTime();

        // nochmal kucken ohne löschen, wie schnell das inkrementelle geht - und was machen eigentlich die zips?
        // die alte history wird gelöscht
        File fHistoryDir = new File("./history/forResourceDir");
        if(!fHistoryDir.exists()) fHistoryDir.mkdirs();

        for (File fSubFile : fHistoryDir.listFiles())
            fSubFile.delete();


        Leech leech = new Leech();


        Granularity bVerbose = Granularity.nothing;


        CrawlerContext crawlerContext =
                new CrawlerContext().setIncrementalCrawlingHistoryPath("./history/forResourceDir").setInterruptIfException(false);



        // testen, ob man mit dem html-context das crawlen von remote links wieder aktivieren kann
        CrawlReportContentHandler reportContentHandler = new CrawlReportContentHandler(new PrintlnContentHandler(bVerbose).setShowOnlyErrors(true));
        leech.parse(new File(strRoot), reportContentHandler, crawlerContext.createParseContext());
        System.out.println(reportContentHandler.getReport());

        StopWatch.stopAndPrintDistance(startTime);


        System.out.println("\n\n§§§ will index the second time (nothing should be seen)");
        startTime = StopWatch.stopAndPrintTime();

        reportContentHandler = new CrawlReportContentHandler(new PrintlnContentHandler(bVerbose));
        leech.parse(new File(strRoot), reportContentHandler, crawlerContext.createParseContext());
        System.out.println(reportContentHandler.getReport());

        StopWatch.stopAndPrintDistance(startTime);

        System.out.println("..finished crawling directory tree");
    }



    @Test
    public void testCrawlDirectoryWithStop() throws Exception
    {


        System.out.println("test inkremental indexing with stop");

        // die alte history wird gelöscht
        File fHistoryDir = new File("./history/forResourceDir");
        if(!fHistoryDir.exists()) fHistoryDir.mkdirs();

        for (File fSubFile : fHistoryDir.listFiles())
            fSubFile.delete();


        Leech leech = new Leech();



        Granularity granularity = Granularity.title;


        final CrawlerContext crawlerContext = new CrawlerContext().setIncrementalCrawlingHistoryPath("./history/forResourceDir");

        System.out.println("§§§ will start indexing");


        // jetzt noch ein Thread, der des Teil stoppt

        Thread stop = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    Thread.sleep(2000);

                    System.out.println("request stop");
                    crawlerContext.requestStop();

                    System.out.println("stopped finished");
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }

        }, "stop thread");

        stop.start();


        leech.parse(new File("resource/testData"), new PrintlnContentHandler(granularity), crawlerContext.createParseContext());



        System.out.println("\nnext try");

        // m_crawlerContext = new CrawlerContext().setIncrementalCrawlingHistoryPath("./history/forResourceDir");
        stop = new Thread(new StopRunnable(), "stop 2");
        stop.start();


        leech.parse(new File("resource/testData"), new PrintlnContentHandler(granularity), crawlerContext.createParseContext());



        System.out.println("..finished crawling directory");
    }



    @Test
    public void testCrawlImapFolder() throws Exception
    {

        System.out.println("test imap folder crawl");

        // die alte history wird gelöscht
        File fHistoryDir = new File("./history/forImapDir");
        if(!fHistoryDir.exists()) fHistoryDir.mkdirs();

        for (File fSubFile : fHistoryDir.listFiles())
            fSubFile.delete();



        // imap[s]://uname:pwd@hostname:port/folder;uidvalidity=385759045/;uid=20

        String strSourceURL = "imaps://doe:-Jane906090-@imap-fbad.kl.dfki.de:993/inbox";
        // einzelmessage testen
        // String strSourceURL = "imaps://doe:-Jane906090-@imap-fbad.kl.dfki.de:993/inbox;uid=362";

        Leech leech = new Leech();


        Granularity granularity = Granularity.nothing;


        CrawlerContext crawlerConfig = new CrawlerContext().setIncrementalCrawlingHistoryPath("./history/forImapDir");
        ParseContext parseContext = crawlerConfig.createParseContext();
        ImapCrawlerContext imapCrawlerContext = new ImapCrawlerContext();
        parseContext.set(ImapCrawlerContext.class, imapCrawlerContext);

        long startTime = StopWatch.stopAndPrintTime();

        System.out.println("will crawl first time");
        CrawlReportContentHandler reportContentHandler = new CrawlReportContentHandler(new PrintlnContentHandler(granularity));
        leech.parse(strSourceURL, reportContentHandler, parseContext);
        System.out.println(reportContentHandler.getReport());
        StopWatch.stopAndPrintDistance(startTime);


        System.out.println("will crawl second time (nothing should be seen)");
        startTime = StopWatch.stopAndPrintTime();
        reportContentHandler = new CrawlReportContentHandler(new PrintlnContentHandler(granularity));
        leech.parse(strSourceURL, reportContentHandler, parseContext);
        System.out.println(reportContentHandler.getReport());
        StopWatch.stopAndPrintDistance(startTime);




        System.out.println("..finished imap folder crawl test");
    }



    @Test
    public void testCrawlSingleFile() throws Exception
    {

        System.out.println("test inkremental indexing with single file");

        // die alte history wird gelöscht
        File fHistoryDir = new File("./history/forResourceDir");
        if(!fHistoryDir.exists()) fHistoryDir.mkdirs();

        for (File fSubFile : fHistoryDir.listFiles())
            fSubFile.delete();

        File fBla = new File("resource/testData_short/blaRenamed.txt");
        if(fBla.exists()) fBla.renameTo(new File("resource/testData_short/bla.txt"));


        Leech leech = new Leech();


        Granularity bVerbose = Granularity.titlePlusFulltext;


        CrawlerContext crawlerContext = new CrawlerContext().setIncrementalCrawlingHistoryPath("./history/forResourceDir");

        System.out.println("§§§ will index first time");

        leech.parse(new File("resource/testData_short/bla.txt"), new PrintlnContentHandler(bVerbose), crawlerContext.createParseContext());

        System.out.println("\n\n§§§ will index the second time (nothing should be seen)");

        leech.parse(new File("resource/testData_short/bla.txt"), new PrintlnContentHandler(bVerbose), crawlerContext.createParseContext());



        System.out.println("..finished crawling single file");
    }



    @Test
    public void test4Website() throws Exception
    {

        Leech leech = new Leech();
        CrawlerContext crawlerContext = new CrawlerContext().setIncrementalCrawlingHistoryPath("./history/4dataDirOrFile");




        leech.parse("URL4FileOrDirOrWebsiteOrImapfolderOrImapmessageOrSomething", new DataSinkContentHandler()
        {
            @Override
            public void processNewData(Metadata metadata, String strFulltext)
            {
                System.out.println("Extracted metadata:\n" + metadata + "\nExtracted fulltext:\n" + strFulltext);
            }
            @Override
            public void processModifiedData(Metadata metadata, String strFulltext)
            {
            }
            @Override
            public void processRemovedData(Metadata metadata)
            {
            }
            @Override
            public void processErrorData(Metadata metadata)
            {
            }

        }, crawlerContext.createParseContext());

    }



    @Test
    public void testCrawlWeb() throws Exception
    {


        System.out.println("test web indexing");

        System.out.println("§§§ will start indexing");

        // die alte history wird gelöscht
        File fHistoryDir = new File("./history/forWebCrawl");
        if(!fHistoryDir.exists()) fHistoryDir.mkdirs();

        for (File fSubFile : fHistoryDir.listFiles())
            fSubFile.delete();



        Granularity bVerbose = Granularity.nothing;

        URLName sourceUrl = new URLName("http://tika.apache.org");
        // die ist gezippt:
        // URL sourceUrl = new URL("http://tika.apache.org/1.1/formats.html");

        // URL sourceUrl = new URL("http://tika.apache.org/1.1/api/org/apache/tika/detect/ContainerAwareDetector.html");
        // URL sourceUrl = new URL("http://tika.apache.org/asf-logo.gif");


        Leech leech = new Leech();

        URLFilter urlFilter = new SubstringPattern("http://tika.apache.org", SubstringPattern.STARTS_WITH).toURLFilterAsInclude();
        urlFilter.addExcludePattern(new SubstringPattern("http://tika.apache.org/0.", SubstringPattern.STARTS_WITH));
        m_crawlerContext =
                new CrawlerContext().setIncrementalCrawlingHistoryPath("./history/forWebCrawl").setURLFilter(urlFilter).setCrawlingDepth(1)
                        .setVerbose(false);



        CrawlReportContentHandler reportContentHandler = new CrawlReportContentHandler(new PrintlnContentHandler(bVerbose));
        leech.parse(sourceUrl, reportContentHandler, m_crawlerContext.createParseContext());
        System.out.println(reportContentHandler.getReport());


        System.out.println("\n\n\n\n\n\n\n SECOND RUN \n\n\n\n\n\n\n");

        reportContentHandler = new CrawlReportContentHandler(new PrintlnContentHandler(bVerbose));
        leech.parse(sourceUrl, reportContentHandler, m_crawlerContext.createParseContext());
        System.out.println(reportContentHandler.getReport());


        System.out.println("..finished web crawling");
    }



    @Test
    public void testCrawlWebWithoutHistory() throws Exception
    {


        System.out.println("test web indexing without history specification");

        System.out.println("§§§ will start indexing");


        Granularity bVerbose = Granularity.title;

        URLName sourceUrl = new URLName("http://tika.apache.org/1.0/formats.html");
        // URL sourceUrl = new URL("http://tika.apache.org/asf-logo.gif");


        Leech leech = new Leech();

        m_crawlerContext = new CrawlerContext().setCrawlingDepth(1);

        leech.parse(sourceUrl, new PrintlnContentHandler(bVerbose), m_crawlerContext.createParseContext());



        System.out.println("..finished web crawling without history specification");
    }



    @Test
    public void testCrawlZipDirectory() throws Exception
    {

        System.out.println("test directory with zip");

        // die alte history wird gelöscht
        File fHistoryDir = new File("./history/forResourceDir");
        if(!fHistoryDir.exists()) fHistoryDir.mkdirs();

        for (File fSubFile : fHistoryDir.listFiles())
            fSubFile.delete();


        Leech leech = new Leech();


        Granularity bVerbose = Granularity.titlePlusFulltext;


        CrawlerContext crawlerConfig = new CrawlerContext().setIncrementalCrawlingHistoryPath("./history/forResourceDir");


        leech.parse(new File("resource/testData_zip"), new PrintlnContentHandler(bVerbose), crawlerConfig.createParseContext());


        System.out.println("..finished crawling directory with zip");
    }



    @Test
    public void testWikipediaCrawl() throws IOException, SAXException, TikaException
    {

        System.out.println("test crawling wikipedia file");

        // File fWikiDump = new File("resource/testData_wikipedia/de_wikidump_mit_header_short_test4allocation.xml");
        File fWikiDump = new File("resource/testData_wikipedia/de_wikidump_mit_header_short.xml");
        // File fWikiDump = new File("/home/reuschling/mnt/pc-4346_cie/chris/dewiki_dump_07.2011/dewiki-latest-pages-articles.xml");



        Leech leech = new Leech();

        long lStart = StopWatch.stopAndPrintTime();
        leech.parse(fWikiDump, new PrintlnContentHandler(Granularity.all));
        StopWatch.stopAndPrintDistance(lStart);

        // System.out.println(leech.detect(fWikiDump));


        System.out.println("..finished crawling wikipedia file");

    }

}
