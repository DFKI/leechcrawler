package de.dfki.km.leech.sax;




import java.io.StringWriter;

import org.apache.tika.metadata.HttpHeaders;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.ContentHandlerDecorator;
import org.apache.tika.sax.WriteOutContentHandler;
import org.xml.sax.SAXException;

import de.dfki.km.leech.parser.CrawlerParser;
import de.dfki.km.leech.parser.incremental.IncrementalCrawlingParser;



/**
 * A ContentHandler implementation to store data with a hook/push-interface. This is for crawling datasources recursively. Implement the processData
 * methods and process your data as you wish.<br>
 * <br>
 * This handler deals with the data entity modification state entries inside the metadata offered from {@link IncrementalCrawlingParser} and
 * {@link CrawlerParser} (in case of an error).
 * 
 * @author Christian Reuschling, Dipl.Ing.(BA)
 */
public abstract class DataSinkContentHandler extends ContentHandlerDecorator
{

    protected int m_iWriteLimit = 6 * 1024 * 1024;

    protected Metadata m_metadata = new Metadata();

    protected StringWriter m_writer;



    /**
     * Creates a new DataSinkContentHandler. Note that the internal metadata object has to be the same than the one given to the parser that works
     * with this contentHandler. Use {@link #setMetaData(Metadata)} or one of the Leech methods with the DataSinkContentHandlers. In the second case
     * Leech will make sure that the metadata objects will be set correctly.
     */
    public DataSinkContentHandler()
    {
    }



    /**
     * Creates a content handler that writes XHTML body character events to an internal string buffer, and forwards it together with the metadata
     * object to a callback/processing method.
     * <p>
     * <p>
     * The internal string buffer is bounded at the given number of characters. If this write limit is reached, then a {@link SAXException} is thrown.
     * 
     * @param writeLimit maximum number of characters to include in the string, or -1 to disable the write limit
     */
    public DataSinkContentHandler(int writeLimit)
    {
        m_iWriteLimit = writeLimit;
    }



    /**
     * Creates a content handler that writes XHTML body character events to an internal string buffer, and forwards it together with the metadata
     * object to a callback/processing method.
     * <p>
     * The internal string buffer is bounded at 6 * 1024 * 1024 characters. If this write limit is reached, then a {@link SAXException} is thrown.
     * 
     * @param metadata the metadata object given to the parser object that works with this ContentHandler. This is to forward this reference to the
     *            processing method, so make sure that both objects holds the same object
     */
    public DataSinkContentHandler(Metadata metadata)
    {
        m_metadata = metadata;
    }



    /**
     * Creates a content handler that writes XHTML body character events to an internal string buffer, and forwards it together with the metadata
     * object to a callback/processing method.
     * <p>
     * <p>
     * The internal string buffer is bounded at the given number of characters. If this write limit is reached, then a {@link SAXException} is thrown.
     * 
     * @param writeLimit maximum number of characters to include in the string, or -1 to disable the write limit
     * @param metadata the metadata object given to the parser object that works with this ContentHandler. This is to forward this reference to the
     *            processing method, so make sure that both objects holds the same object
     */
    public DataSinkContentHandler(Metadata metadata, int writeLimit)
    {
        m_iWriteLimit = writeLimit;
        m_metadata = metadata;
    }



    @Override
    public void endDocument() throws SAXException
    {
        super.endDocument();


        String strDataEntitiyModState = m_metadata.get(IncrementalCrawlingParser.DATA_ENTITY_MODIFICATION_STATE);
        m_metadata.remove(IncrementalCrawlingParser.DATA_ENTITY_MODIFICATION_STATE);
        m_metadata.remove(CrawlerParser.CURRENT_CRAWLING_DEPTH);


        if(IncrementalCrawlingParser.MODIFIED.equals(strDataEntitiyModState))
        {
            processModifiedData(m_metadata, this.toString());
        }
        else if(IncrementalCrawlingParser.REMOVED.equals(strDataEntitiyModState))
        {
            // these are set because of the dummy stream
            m_metadata.remove(HttpHeaders.CONTENT_ENCODING);
            m_metadata.remove(HttpHeaders.CONTENT_TYPE);
            processRemovedData(m_metadata);
        }
        else if(IncrementalCrawlingParser.ERROR.equals(strDataEntitiyModState))
        {
            processErrorData(m_metadata);
        }
        else
            processNewData(m_metadata, this.toString());



        // da wir diesen handler über die rekursiven Aufrufe wiederverwenden möchten, setzen wir hier die members zurück. Das metadata-Object wird im
        // CrawlerParser zurückgesetzt
        m_writer.getBuffer().delete(0, m_writer.getBuffer().length());

    }



    public Metadata getMetaData()
    {
        return m_metadata;
    }



    protected void init()
    {
        m_writer = new StringWriter();
        setContentHandler(new BodyContentHandler(new WriteOutContentHandler(m_writer, m_iWriteLimit)));
    }



    /**
     * Will be invoked in the case a data entity causes an error during indexing.
     * 
     * @param metadata some metadata (at least an identifying Id) to deal with the error entity
     */
    public abstract void processErrorData(Metadata metadata);



    /**
     * Will be invoked in the case a data entity was modified since the last crawl.
     * 
     * @param metadata the metadata of the data entity
     * @param strFulltext the full body text of the data entity
     */
    public abstract void processModifiedData(Metadata metadata, String strFulltext);



    /**
     * Will be invoked in the case a new data entity was found.
     * 
     * @param metadata the metadata of the data entity
     * @param strFulltext the full body text of the data entity
     */
    public abstract void processNewData(Metadata metadata, String strFulltext);



    /**
     * Will be invoked in the case a data entity was removed since the last crawl.
     * 
     * @param metadata some metadata (at least an identifying Id) to deal with the removed entity
     */
    public abstract void processRemovedData(Metadata metadata);




    public void setMetaData(Metadata metadata)
    {
        m_metadata = metadata;
    }




    @Override
    public void startDocument() throws SAXException
    {
        init();
        super.startDocument();
    }

}
