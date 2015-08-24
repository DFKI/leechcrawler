package de.dfki.km.leech.util;



import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.tika.exception.TikaException;
import org.apache.tika.parser.ParseContext;
import org.xml.sax.SAXException;

import de.dfki.inquisition.collections.MultiValueHashMap;
import de.dfki.inquisition.processes.StopWatch;
import de.dfki.km.leech.Leech;
import de.dfki.km.leech.config.CrawlerContext;
import de.dfki.km.leech.parser.wikipedia.WikipediaDumpParser;
import de.dfki.km.leech.parser.wikipedia.WikipediaDumpParser.WikipediaDumpParserConfig;
import de.dfki.km.leech.sax.CrawlReportContentHandler;
import de.dfki.km.leech.sax.PrintlnContentHandler;
import de.dfki.km.leech.sax.PrintlnContentHandler.Verbosity;
import de.dfki.km.leech.solr.ToSolrContentHandler;



/**
 * A very simple Lucene Index creator. FieldConfig is from {@link WikipediaDumpParser#getFieldConfig4ParserAttributes()}, currently you can only specify the source
 * dir/file and the target dir for the lucene index
 * 
 * @author Christian Reuschling, Dipl.Ing.(BA)
 * 
 */
public class SolrIndexCreator
{

    public static long cyclicReportTime = 1000 * 60;



    public static void createIndex(List<String> lUrls2Crawl, String strSolrUrl, MultiValueHashMap<String, String> hsStaticAttValuePairs, boolean bPrintErrors)
            throws IOException, Exception, SAXException, TikaException
    {
        createIndex(lUrls2Crawl, strSolrUrl, hsStaticAttValuePairs, bPrintErrors, null);

    }



    public static void createIndex(List<String> lUrls2Crawl, String strSolrUrl, MultiValueHashMap<String, String> hsStaticAttValuePairs, boolean bPrintErrors,
            ParseContext context) throws IOException, Exception, SAXException, TikaException
    {

        if(context == null) context = new ParseContext();
        if(hsStaticAttValuePairs == null) hsStaticAttValuePairs = new MultiValueHashMap<>();


        Logger.getLogger(SolrIndexCreator.class.getName()).info("Crawling " + lUrls2Crawl);

        if(hsStaticAttValuePairs.keySize() > 0)
            Logger.getLogger(SolrIndexCreator.class.getName()).info("Will add static attribute value pairs to each document: " + hsStaticAttValuePairs);




        Leech leech = new Leech();

        long startTime = StopWatch.startAndLogTime(Level.INFO);


        CrawlReportContentHandler reportContentHandler;


        ToSolrContentHandler toSolrContentHandler = new ToSolrContentHandler(strSolrUrl).setStaticAttributeValuePairs(hsStaticAttValuePairs);

        if(bPrintErrors)
            reportContentHandler = new CrawlReportContentHandler(new PrintlnContentHandler(Verbosity.all, toSolrContentHandler).setShowOnlyErrors(true));
        else
            reportContentHandler = new CrawlReportContentHandler(toSolrContentHandler);




        leech.parse(lUrls2Crawl.toArray(new String[0]), reportContentHandler.setCyclicReportPrintln(cyclicReportTime), context);


        StopWatch.stopAndLogDistance(startTime, Level.INFO);

    }



    /**
     * @param args args[0] is the source dir/file, args[1] the lucene target directory
     * 
     * @throws Exception
     */
    public static void main(String[] args) throws Exception
    {

        if(args.length == 0 || (args.length != 0 && (args[0].equals("-?") || args[0].equals("-h") || args[0].equals("--help"))))
        {

            System.out.println("Usage: SolrIndexCreator [-noPageRedirects] [-noParseGeoCoordinates] [-parseInfoBoxes] [-parseLinksAndCategories]\n"
                    + " [-<staticAttName>=<staticAttValue>] [-printErrors] [-crawlingDepth=<depth>]\n"
                    + " <fileOrDir2CrawlPath1> .. <fileOrDir2CrawlPathN> <solrURL>\n\nComments: - you can specify several static attribute value pairs.\n");

            System.out.println();

            return;
        }


        LinkedList<String> llFile2CrawlPath = new LinkedList<>();
        String strSolrUrl = null;

        int iCrawlingDepth = Integer.MAX_VALUE;
        boolean bPrintErrors = false;




        ParseContext context = new ParseContext();
        WikipediaDumpParserConfig wikipediaDumpParserConfig =
                new WikipediaDumpParserConfig().setDeterminePageRedirects(true).setParseGeoCoordinates(true).setParseInfoBoxes(false).setParseLinksAndCategories(false);
        context.set(WikipediaDumpParserConfig.class, wikipediaDumpParserConfig);


        MultiValueHashMap<String, String> hsStaticAttValuePairs = new MultiValueHashMap<String, String>();


        for (int i = 0; i < args.length; i++)
        {
            String strArg = args[i];

            if(strArg.equals("-noPageRedirects"))
            {
                wikipediaDumpParserConfig.setDeterminePageRedirects(false);
            }
            else if(strArg.equals("-noParseGeoCoordinates"))
            {
                wikipediaDumpParserConfig.setParseGeoCoordinates(false);
            }
            else if(strArg.equals("-parseInfoBoxes"))
            {
                wikipediaDumpParserConfig.setParseInfoBoxes(true);
            }
            else if(strArg.equals("-parseLinksAndCategories"))
            {
                wikipediaDumpParserConfig.setParseInfoBoxes(true);
            }
            else if(strArg.startsWith("-crawlingDepth="))
            {
                iCrawlingDepth = Integer.valueOf(strArg.replace("-crawlingDepth=", ""));
            }
            else if(strArg.startsWith("-printErrors"))
            {
                bPrintErrors = true;
            }
            else if(strArg.startsWith("-"))
            {
                strArg = strArg.substring(1);
                if(!strArg.contains("=")) continue;
                String[] split = strArg.split("=");
                hsStaticAttValuePairs.add(split[0], split[1]);
            }
            else if(llFile2CrawlPath.size() == 0 || i != (args.length - 1))
            {
                llFile2CrawlPath.add(args[i]);
            }
            else
                strSolrUrl = args[i];

        }

        Logger.getLogger(SolrIndexCreator.class.getName()).info("crawling depth is " + iCrawlingDepth);


        CrawlerContext crawlerContext = new CrawlerContext().setCrawlingDepth(iCrawlingDepth);
        context.set(CrawlerContext.class, crawlerContext);

        createIndex(llFile2CrawlPath, strSolrUrl, hsStaticAttValuePairs, bPrintErrors, context);


    }







}
