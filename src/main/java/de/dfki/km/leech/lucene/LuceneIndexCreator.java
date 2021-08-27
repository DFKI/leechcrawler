package de.dfki.km.leech.lucene;



import de.dfki.inquisitor.collections.MultiValueHashMap;
import de.dfki.inquisitor.processes.StopWatch;
import de.dfki.inquisitor.text.StringUtils;
import de.dfki.km.leech.Leech;
import de.dfki.km.leech.config.CrawlerContext;
import de.dfki.km.leech.lucene.basic.FieldConfig;
import de.dfki.km.leech.parser.wikipedia.WikipediaDumpParser.WikipediaDumpParserConfig;
import de.dfki.km.leech.sax.CrawlReportContentHandler;
import de.dfki.km.leech.sax.PrintlnContentHandler;
import de.dfki.km.leech.sax.PrintlnContentHandler.Verbosity;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;



/**
 * A very simple Lucene Index creator. Currently you can only specify the source dir/file and the target dir for the lucene index
 * 
 * @author Christian Reuschling, Dipl.Ing.(BA)
 * 
 */
public class LuceneIndexCreator
{

    public static long cyclicReportTime = 1000 * 60;



    public static void createIndex(List<String> lUrls2Crawl, String strLuceneIndexPath, LinkedList<String> llLookupIndexPaths, String strBuzzwordAttName,
            int iBuzzwordCount, boolean bCalculatePageCounts, String strFrequencyClassAttName, MultiValueHashMap<String, String> hsStaticAttValuePairs,
            boolean bPrintErrors) throws IOException, Exception, SAXException, TikaException
    {
        createIndex(lUrls2Crawl, strLuceneIndexPath, llLookupIndexPaths, strBuzzwordAttName, iBuzzwordCount, bCalculatePageCounts, strFrequencyClassAttName,
                hsStaticAttValuePairs, bPrintErrors, null);

    }



    public static void createIndex(List<String> lUrls2Crawl, String strLuceneIndexPath, LinkedList<String> llLookupIndexPaths, String strBuzzwordAttName,
            int iBuzzwordCount, boolean bCalculatePageCounts, String strFrequencyClassAttName, MultiValueHashMap<String, String> hsStaticAttValuePairs,
            boolean bPrintErrors, ParseContext context) throws IOException, Exception, SAXException, TikaException
    {

        if(context == null) context = new ParseContext();
        if(llLookupIndexPaths == null) llLookupIndexPaths = new LinkedList<>();
        if(hsStaticAttValuePairs == null) hsStaticAttValuePairs = new MultiValueHashMap<>();


        boolean bOnlyPostProcessing = false;
        if(strLuceneIndexPath == null)
        {
            strLuceneIndexPath = lUrls2Crawl.iterator().next();
            lUrls2Crawl = null;

            bOnlyPostProcessing = true;
            LoggerFactory.getLogger(LuceneIndexCreator.class.getName()).info(
                    "Will perform only postprocessing (buzzwords and/or calculated page counts, as configured) on " + strLuceneIndexPath);

        }
        else
        {
            LoggerFactory.getLogger(LuceneIndexCreator.class.getName()).info("Crawling " + lUrls2Crawl);

            if(hsStaticAttValuePairs.keySize() > 0)
                LoggerFactory.getLogger(LuceneIndexCreator.class.getName()).info("Will add static attribute value pairs to each document: " + hsStaticAttValuePairs);




            Leech leech = new Leech();

            long startTime = StopWatch.startAndLogTime(LuceneIndexCreator.class);


            CrawlReportContentHandler reportContentHandler;
            IndexWriter indexWriter = null;
            SimpleFSDirectory directory = new SimpleFSDirectory(Paths.get(strLuceneIndexPath));
            FieldConfig fieldConfig = new LeechDefaultFieldConfig();

            context.set(FieldConfig.class, fieldConfig);



            @SuppressWarnings("deprecation")
            IndexWriterConfig config = new IndexWriterConfig(fieldConfig.createAnalyzer());

            config.setOpenMode(OpenMode.CREATE_OR_APPEND);


            indexWriter = new IndexWriter(directory, config);



            Map<String, String> hsFieldName2FieldValue = new HashMap<String, String>();

            // hsFieldName2FieldValue.put("infobox", "[Bb]and");
            ToLuceneContentHandler toLuceneContentHandler =
                    new ToLuceneContentHandler(fieldConfig, indexWriter).setIgnoreAllDocsWithout(hsFieldName2FieldValue).setStaticAttributeValuePairs(
                            hsStaticAttValuePairs);

            if(bPrintErrors)
                reportContentHandler = new CrawlReportContentHandler(new PrintlnContentHandler(Verbosity.all, toLuceneContentHandler).setShowOnlyErrors(true));
            else
                reportContentHandler = new CrawlReportContentHandler(toLuceneContentHandler);





            leech.parse(lUrls2Crawl.toArray(new String[0]), reportContentHandler.setCyclicReportPrintln(cyclicReportTime), context);


            if(indexWriter != null)
            {
                LoggerFactory.getLogger(LuceneIndexCreator.class.getName()).info("Will commit and merge");
                indexWriter.commit();
                indexWriter.forceMerge(1, true);
                indexWriter.close();

                StopWatch.stopAndLogDistance(startTime, LuceneIndexCreator.class);

                LoggerFactory.getLogger(LuceneIndexCreator.class.getName()).info("..finished crawling " + lUrls2Crawl);
            }
        }







        // das postprocessing

        IndexPostprocessor postprocessor = new IndexPostprocessor();

        boolean bPerformPostProcessing = false;
        // wenn die Werte null sind, ist das Teil disabled
        if(!StringUtils.nullOrWhitespace(strBuzzwordAttName))
        {
            postprocessor.enableBuzzwordGeneration(strBuzzwordAttName, iBuzzwordCount, true);
            bPerformPostProcessing = true;
        }
        if(bCalculatePageCounts)
        {
            postprocessor.enablePageCountEstimation();
            bPerformPostProcessing = true;
        }
        if(!StringUtils.nullOrWhitespace(strFrequencyClassAttName))
        {
            postprocessor.enableFrequencyClassCalculation(strFrequencyClassAttName);
            bPerformPostProcessing = true;
        }
        if(bOnlyPostProcessing && hsStaticAttValuePairs.keySize() > 0)
        {
            Metadata staticAtts2Values = new Metadata();
            for (Entry<String, String> att2Value : hsStaticAttValuePairs.entryList())
                staticAtts2Values.add(att2Value.getKey(), att2Value.getValue());

            postprocessor.enableStaticAttributeValuePairs(staticAtts2Values);
            bPerformPostProcessing = true;
        }

        if(bPerformPostProcessing)
            postprocessor.postprocessIndex(strLuceneIndexPath, new LeechDefaultFieldConfig(), llLookupIndexPaths.toArray(new String[0]));
        else
            LoggerFactory.getLogger(LuceneIndexCreator.class.getName()).info("no postprocessing necessary");
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

            System.out.println("Usage: LuceneIndexCreator [-noPageRedirects] [-noParseGeoCoordinates] [-parseInfoBoxes] [-parseLinksAndCategories]\n"
                    + " [-<staticAttName>=<staticAttValue>] [-buzzwordAttName=<attName>] [-buzzwordCount=<count>] [-calculatePageCounts] [-printErrors]\n"
                    + "[-frequencyClassAttName=<attName>] [-li <readonlyLookupIndexPath>] [-crawlingDepth=<depth>]"
                    + " <fileOrDir2CrawlPath1> .. <fileOrDir2CrawlPathN> <targetLuceneIndexPath>\n\nComments: - you can specify several static attribute value pairs.\n"
                    + "- if you leave <fileOrDir2CrawlPath>, only postprocessing will be performed.\n" + "- you can add several lookup indices (-li).\n"
                    + "- if you leave the buzzword attName or the frequency class attName, these processing steps will be skiped.");
            System.out.println();

            return;
        }


        LinkedList<String> llFile2CrawlPath = new LinkedList<>();
        String strLuceneIndexPath = null;
        String strBuzzwordAttName = null;
        String strFrequencyClassAttName = null;

        int iBuzzwordCount = 7;
        boolean bCalculatePageCounts = false;
        LinkedList<String> llLookupIndexPaths = new LinkedList<String>();

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
            else if(strArg.startsWith("-buzzwordAttName"))
            {
                strBuzzwordAttName = strArg.replace("-buzzwordAttName=", "").trim();
            }
            else if(strArg.startsWith("-buzzwordCount="))
            {
                iBuzzwordCount = Integer.valueOf(strArg.replace("-buzzwordCount=", ""));
            }
            else if(strArg.startsWith("-crawlingDepth="))
            {
                iCrawlingDepth = Integer.valueOf(strArg.replace("-crawlingDepth=", ""));
            }
            else if(strArg.startsWith("-frequencyClassAttName="))
            {
                strFrequencyClassAttName = strArg.replace("-frequencyClassAttName=", "").trim();
            }
            else if(strArg.startsWith("-calculatePageCounts"))
            {
                bCalculatePageCounts = true;
            }
            else if(strArg.startsWith("-printErrors"))
            {
                bPrintErrors = true;
            }
            else if(strArg.startsWith("-li"))
            {
                llLookupIndexPaths.add(args[++i]);
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
                strLuceneIndexPath = args[i];

        }

        LoggerFactory.getLogger(LuceneIndexCreator.class.getName()).info("crawling depth is " + iCrawlingDepth);


        CrawlerContext crawlerContext = new CrawlerContext().setCrawlingDepth(iCrawlingDepth);
        context.set(CrawlerContext.class, crawlerContext);

        createIndex(llFile2CrawlPath, strLuceneIndexPath, llLookupIndexPaths, strBuzzwordAttName, iBuzzwordCount, bCalculatePageCounts, strFrequencyClassAttName,
                hsStaticAttValuePairs, bPrintErrors, context);


    }







}
