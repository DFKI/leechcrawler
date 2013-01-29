package de.dfki.km.leech.util;



import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;
import org.apache.tika.parser.ParseContext;

import de.dfki.km.leech.Leech;
import de.dfki.km.leech.lucene.FieldConfig;
import de.dfki.km.leech.lucene.ToLuceneContentHandler;
import de.dfki.km.leech.parser.wikipedia.WikipediaDumpParser;
import de.dfki.km.leech.parser.wikipedia.WikipediaDumpParser.WikipediaDumpParserConfig;
import de.dfki.km.leech.sax.CrawlReportContentHandler;
import de.dfki.km.leech.sax.PrintlnContentHandler;
import de.dfki.km.leech.sax.PrintlnContentHandler.Verbosity;



/**
 * A very simple Lucene Index creator. FieldConfig is from {@link WikipediaDumpParser#getFieldConfig4ParserAttributes()}, currently you can only
 * specify the source dir/file and the target dir for the lucene index
 * 
 * @author Christian Reuschling, Dipl.Ing.(BA)
 * 
 */
public class LuceneIndexCreator
{

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
                    + " [-<staticAttName>=<staticAttValue>] [-buzzwordAttName=<attName>] [-buzzwordCount=<count>] [-calculatePageCounts]\n"
                    + " <fileOrDir2CrawlPath> <targetLuceneIndexPath>");
            System.out.println();

            return;
        }


        String strFile2CrawlPath = null;
        String strLuceneIndexPath = null;
        String strBuzzwordAttName = null;
        int iBuzzwordCount = 7;
        boolean bCalculatePageCounts = false;




        ParseContext context = new ParseContext();
        WikipediaDumpParserConfig wikipediaDumpParserConfig =
                new WikipediaDumpParserConfig().setDeterminePageRedirects(true).setParseGeoCoordinates(true).setParseInfoBoxes(false)
                        .setParseLinksAndCategories(false);
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
            else if(strArg.startsWith("-calculatePageCounts"))
            {
                bCalculatePageCounts = true;
            }
            else if(strArg.startsWith("-"))
            {
                strArg = strArg.substring(1);
                if(!strArg.contains("=")) continue;
                String[] split = strArg.split("=");
                hsStaticAttValuePairs.add(split[0], split[1]);
            }
            else if(strFile2CrawlPath == null)
            {
                strFile2CrawlPath = args[i];
            }
            else
                strLuceneIndexPath = args[i];

        }

        if(strLuceneIndexPath == null)
        {
            strLuceneIndexPath = strFile2CrawlPath;
            strFile2CrawlPath = null;
            Logger.getLogger(LuceneIndexCreator.class.getName()).info(
                    "Will perform only postprocessing (buzzwords and/or calculated page counts, as configured) on " + strLuceneIndexPath);

        }
        else
        {
            Logger.getLogger(LuceneIndexCreator.class.getName()).info("Crawling " + strFile2CrawlPath);

            if(hsStaticAttValuePairs.keySize() > 0)
                Logger.getLogger(LuceneIndexCreator.class.getName()).info(
                        "Will add static attribute value pairs to each document: " + hsStaticAttValuePairs);
        }





        Leech leech = new Leech();

        long lStart = StopWatch.stopAndPrintTime();


        CrawlReportContentHandler reportContentHandler;
        IndexWriter indexWriter = null;
        SimpleFSDirectory directory = new SimpleFSDirectory(new File(strLuceneIndexPath));
        FieldConfig fieldConfig4Wikipedia = WikipediaDumpParser.getFieldConfig4ParserAttributes();

        if(strLuceneIndexPath != null)
        {


            IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_31, fieldConfig4Wikipedia.createAnalyzer());

            config.setOpenMode(OpenMode.CREATE_OR_APPEND);
            // config.setOpenMode(OpenMode.APPEND);


            indexWriter = new IndexWriter(directory, config);
            // indexWriter = new IndexWriter(new NIOFSDirectory(new File(strLuceneIndexPath)), config);
            // indexWriter =
            // new IndexWriter(new SimpleFSDirectory(new File(strLuceneIndexPath)), fieldConfig4Wikipedia.createAnalyzer(), false,
            // IndexWriter.MaxFieldLength.UNLIMITED);



            Map<String, String> hsFieldName2FieldValue = new HashMap<String, String>();

            // hsFieldName2FieldValue.put("infobox", "[Bb]and");
            ToLuceneContentHandler toLuceneContentHandler =
                    new ToLuceneContentHandler(fieldConfig4Wikipedia, indexWriter).setIgnoreAllDocsWithout(hsFieldName2FieldValue)
                            .setStaticAttributeValuePairs(hsStaticAttValuePairs);

            reportContentHandler =
                    new CrawlReportContentHandler(new PrintlnContentHandler(Verbosity.all, toLuceneContentHandler).setShowOnlyErrors(true));
        }
        else
        {
            reportContentHandler = new CrawlReportContentHandler(new PrintlnContentHandler(Verbosity.all).setShowOnlyErrors(true));
        }





        if(strFile2CrawlPath != null)
        {
            File fFile2Crawl = new File(strFile2CrawlPath);

            leech.parse(fFile2Crawl, reportContentHandler.setCyclicReportPrintln(7000), context);
        }


        if(indexWriter != null)
        {
            indexWriter.commit();
            indexWriter.forceMerge(1, true);
            indexWriter.close(true);

            StopWatch.stopAndPrintDistance(lStart);

            System.out.println("..finished crawling " + strFile2CrawlPath);
        }



        // das postprocessing


        if(StringUtils.nullOrWhitespace(strBuzzwordAttName) && !bCalculatePageCounts) return;

        IndexPostprocessor postprocessor = new IndexPostprocessor();

        postprocessor.enableBuzzwordGeneration(strBuzzwordAttName, iBuzzwordCount, true);
        postprocessor.enablePageCountEstimation();

        postprocessor.postprocessIndex(strLuceneIndexPath, fieldConfig4Wikipedia);


    }







}
