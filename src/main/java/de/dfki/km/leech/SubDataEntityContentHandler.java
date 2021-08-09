package de.dfki.km.leech;



import de.dfki.km.leech.config.CrawlerContext;
import de.dfki.km.leech.parser.incremental.IncrementalCrawlingHistory;
import de.dfki.km.leech.parser.incremental.IncrementalCrawlingParser;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.XHTMLContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;






public class SubDataEntityContentHandler extends XHTMLContentHandler
{

    /**
     * Convenience method to trigger an (subdata )entity handling. This is also usefull if you want to delegate parsing tasks to another parser in your own parser
     */
    public static void triggerEntityHandling(ContentHandler handler, Metadata metadata, String strBodyText, ParseContext context4history) throws SAXException
    {
        new SubDataEntityContentHandler(handler, metadata, strBodyText).triggerSubDataEntityHandling(context4history);
    }



    /**
     * Convenience method to trigger an (subdata )entity handling. This is also usefull if you want to delegate parsing tasks to another parser in your own parser. This method
     * can not deal wlth possible history tasks, which is not necessary e.g. when delegating a parsing task.
     */
    public static void triggerEntityHandling(ContentHandler handler, Metadata metadata, String strBodyText) throws SAXException
    {
        new SubDataEntityContentHandler(handler, metadata, strBodyText).triggerSubDataEntityHandling();
    }

    protected Metadata m_metadata;

    protected String m_strBodyText;



    public SubDataEntityContentHandler(ContentHandler handler, Metadata metadata, String strBodyText)
    {
        super(handler, metadata);
        m_metadata = metadata;
        m_strBodyText = strBodyText;

    }



    /**
     * Triggers the sub data entity handling WITHOUT considering a possibly set history. This is not always necessary, and better in performance
     *
     * @throws Exception
     */
    public void triggerSubDataEntityHandling() throws SAXException
    {
        triggerSubDataEntityHandling(null);
    }



    /**
     * Triggers the sub data entity handling. If the context is set null, a possibly set history will be ignored cause of performance. Considering the history is not
     * always necessary in every parser.
     *
     * @param context4history null: a history in the context will be ignored. Otherwise, if a history is part of the context, it will be used for recognizing cycles and
     *                        other incremental indexing stuff. Thus, if an entity is indexed yet and is unmodified, it won't be indexed/handeled again, the method will ignore this
     *                        entity and do nothing.
     *
     * @throws SAXException
     */
    public void triggerSubDataEntityHandling(ParseContext context4history) throws SAXException
    {

        try
        {


            boolean bDoIt = false;

            // wir beziehen die history mit ein, wenn die entsprechenden Metadaten vorhanden sind
            if(context4history != null && m_metadata.get(IncrementalCrawlingHistory.dataEntityId) != null
                    && m_metadata.get(IncrementalCrawlingHistory.dataEntityContentFingerprint) != null)
            {

                CrawlerContext crawlerContext = context4history.get(CrawlerContext.class);

                IncrementalCrawlingHistory crawlingHistory = null;
                if(crawlerContext != null)
                    crawlingHistory = crawlerContext.getIncrementalCrawlingHistory();


                // the entity will be processed in the case the crawlingHistory is null
                boolean bProcessEntity = IncrementalCrawlingParser.performHistoryStuff(crawlingHistory, m_metadata);

                if(bProcessEntity)
                {

                    String strDataEntityModState = m_metadata.get(IncrementalCrawlingParser.DATA_ENTITY_MODIFICATION_STATE);

                    //                    if(!IncrementalCrawlingParser.UNMODIFIED.equals(strDataEntityModState))
                    {
                        bDoIt = true;
                    }
                }
            }
            else
                bDoIt = true;



            if(bDoIt)
            {
                // wenn wir keine Metadaten für eine Dublettenerkennung haben oder wir die history ignorieren wollen, dann tragen wir den Datensatz einfach ein

                startDocument();

                // startElement("p");
                if(m_strBodyText != null)
                    characters(m_strBodyText.toCharArray(), 0, m_strBodyText.length());
                // endElement("p");

                endDocument();
            }


        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

    }



}
