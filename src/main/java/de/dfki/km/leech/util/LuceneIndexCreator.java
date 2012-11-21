package de.dfki.km.leech.util;



import java.io.File;
import java.util.HashMap;
import java.util.Map;

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

            System.out.println("Usage: LuceneIndexCreator [-noPageRedirects] [-noParseGeoCoordinates] [-parseInfoBoxes] [-parseLinksAndCategories]"
                    + " <fileOrDir2CrawlPath> <targetLuceneIndexPath>");
            System.out.println();

            return;
        }


        String strFile2CrawlPath = null;
        String strLuceneIndexPath = null;


        ParseContext context = new ParseContext();
        WikipediaDumpParserConfig wikipediaDumpParserConfig =
                new WikipediaDumpParserConfig().setDeterminePageRedirects(true).setParseGeoCoordinates(true).setParseInfoBoxes(false)
                        .setParseLinksAndCategories(false);
        context.set(WikipediaDumpParserConfig.class, wikipediaDumpParserConfig);


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
            else if(strFile2CrawlPath == null)
            {
                strFile2CrawlPath = args[i];
            }
            else
                strLuceneIndexPath = args[i];

        }



        System.out.println("Crawling " + strFile2CrawlPath);


        File fFile2Crawl = new File(strFile2CrawlPath);


        Leech leech = new Leech();

        long lStart = StopWatch.stopAndPrintTime();


        CrawlReportContentHandler reportContentHandler;
        IndexWriter indexWriter = null;

        if(strLuceneIndexPath != null)
        {
            FieldConfig fieldConfig4Wikipedia = WikipediaDumpParser.getFieldConfig4ParserAttributes();

            IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_CURRENT, fieldConfig4Wikipedia.createAnalyzer());

            config.setOpenMode(OpenMode.CREATE);


            indexWriter = new IndexWriter(new SimpleFSDirectory(new File(strLuceneIndexPath)), config);



            Map<String, String> hsFieldName2FieldValue = new HashMap<String, String>();
            // hsFieldName2FieldValue.put("infobox", "[Bb]and");
            ToLuceneContentHandler toLuceneContentHandler =
                    new ToLuceneContentHandler(fieldConfig4Wikipedia, indexWriter).setIgnoreAllDocsWithout(hsFieldName2FieldValue);

            reportContentHandler =
                    new CrawlReportContentHandler(new PrintlnContentHandler(Verbosity.all, toLuceneContentHandler).setShowOnlyErrors(true));
        }
        else
        {
            reportContentHandler = new CrawlReportContentHandler(new PrintlnContentHandler(Verbosity.all).setShowOnlyErrors(true));
        }





        leech.parse(fFile2Crawl, reportContentHandler.setCyclicReportPrintln(7000), context);

        if(indexWriter != null)
        {
            indexWriter.forceMerge(1, true);
            indexWriter.close(true);
        }
        StopWatch.stopAndPrintDistance(lStart);

        System.out.println("..finished crawling " + strFile2CrawlPath);

    }

}
