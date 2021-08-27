/*
 * Leech - crawling capabilities for Apache Tika
 *
 * Copyright (C) 2012 DFKI GmbH, Author: Christian Reuschling
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * Contact us by mail: christian.reuschling@dfki.de
 */

package de.dfki.km.leech.sax;




import de.dfki.km.leech.metadata.LeechMetadata;
import de.dfki.km.leech.parser.CrawlerParser;
import de.dfki.km.leech.parser.incremental.IncrementalCrawlingHistory;
import de.dfki.km.leech.parser.incremental.IncrementalCrawlingParser;
import de.dfki.km.leech.util.UrlUtil;
import org.apache.tika.metadata.HttpHeaders;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.ContentHandlerDecorator;
import org.apache.tika.sax.WriteOutContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.StringWriter;



/**
 * A ContentHandler implementation to store data with a hook/push-interface. This is for crawling datasources recursively. Implement the processData methods and process
 * your data as you wish.<br>
 * <br>
 * This handler deals with the data entity modification state entries inside the metadata offered from {@link IncrementalCrawlingParser} and {@link CrawlerParser} (in
 * case of an error).
 *
 * @author Christian Reuschling, Dipl.Ing.(BA)
 */
public abstract class DataSinkContentHandler extends ContentHandlerDecorator
{

    protected int m_iWriteLimit = -1;

    protected Metadata m_metadata = new Metadata();

    protected StringWriter m_writer;



    /**
     * Creates a new {@link DataSinkContentHandler}.<br>
     * CAUTION:Note that the internal metadata object has to be the same than the one given to the parser that works with this contentHandler. Use
     * {@link #setMetaData(Metadata)} or one of the Leech methods with the DataSinkContentHandlers. In the second case Leech will make sure that the metadata objects will
     * be set correctly.
     */
    public DataSinkContentHandler()
    {
    }



    /**
     * Creates a content handler that writes XHTML body character events to an internal string buffer, and forwards it together with the metadata object to a
     * callback/processing method.<br>
     * CAUTION:Note that the internal metadata object has to be the same than the one given to the parser that works with this contentHandler. Use
     * {@link #setMetaData(Metadata)} or one of the Leech methods with the DataSinkContentHandlers. In the second case Leech will make sure that the metadata objects will
     * be set correctly.
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
     * Creates a content handler that writes XHTML body character events to an internal string buffer, and forwards it together with the metadata object to a
     * callback/processing method.
     * <p>
     * The internal string buffer is bounded at 6 * 1024 * 1024 characters. If this write limit is reached, then a {@link SAXException} is thrown.
     *
     * @param metadata the metadata object given to the parser object that works with this ContentHandler. This is to forward this reference to the processing method, so
     *                 make sure that both objects holds the same object
     */
    public DataSinkContentHandler(Metadata metadata)
    {
        m_metadata = metadata;
    }



    /**
     * Creates a content handler that writes XHTML body character events to an internal string buffer, and forwards it together with the metadata object to a
     * callback/processing method.
     * <p>
     * <p>
     * The internal string buffer is bounded at the given number of characters. If this write limit is reached, then a {@link SAXException} is thrown.
     *
     * @param writeLimit maximum number of characters to include in the string, or -1 to disable the write limit
     * @param metadata   the metadata object given to the parser object that works with this ContentHandler. This is to forward this reference to the processing method, so
     *                   make sure that both objects holds the same object
     */
    public DataSinkContentHandler(Metadata metadata, int writeLimit)
    {
        m_iWriteLimit = writeLimit;
        m_metadata = metadata;
    }



    /**
     * This method will be invoked by the leech class at the end of the parse method. You can perform some shutdown stuff after the crawl if you implement this method.
     */
    public abstract void crawlFinished();



    @Override
    public void endDocument() throws SAXException
    {
        super.endDocument();


        String strDataEntitiyModState = m_metadata.get(IncrementalCrawlingParser.DATA_ENTITY_MODIFICATION_STATE);

        // wir entfernen die Dinge, die wir gar nicht drin haben wollen
        m_metadata.remove(IncrementalCrawlingParser.DATA_ENTITY_MODIFICATION_STATE);
        m_metadata.remove(CrawlerParser.CURRENT_CRAWLING_DEPTH);

        // und passen auf, daß nicht noch Passwörter in einer URL stehen
        String strBadAttName = IncrementalCrawlingHistory.dataEntityId;
        String[] straUrlsWithPwd = m_metadata.getValues(strBadAttName);
        m_metadata.remove(strBadAttName);
        for (String strPossiblePwdUrlString : straUrlsWithPwd)
            m_metadata.add(strBadAttName, UrlUtil.urlNameWithoutPassword(strPossiblePwdUrlString));

        strBadAttName = Metadata.SOURCE;
        straUrlsWithPwd = m_metadata.getValues(strBadAttName);
        m_metadata.remove(strBadAttName);
        for (String strPossiblePwdUrlString : straUrlsWithPwd)
            m_metadata.add(strBadAttName, UrlUtil.urlNameWithoutPassword(strPossiblePwdUrlString));

        strBadAttName = LeechMetadata.RESOURCE_NAME_KEY;
        straUrlsWithPwd = m_metadata.getValues(strBadAttName);
        m_metadata.remove(strBadAttName);
        for (String strPossiblePwdUrlString : straUrlsWithPwd)
            m_metadata.add(strBadAttName, UrlUtil.urlNameWithoutPassword(strPossiblePwdUrlString));




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
        else if(IncrementalCrawlingParser.UNMODIFIED.equals(strDataEntitiyModState))
        {
            // these are set because of the dummy stream
            m_metadata.remove(HttpHeaders.CONTENT_ENCODING);
            m_metadata.remove(HttpHeaders.CONTENT_TYPE);
            processUnmodifiedData(m_metadata);
        }
        else if(IncrementalCrawlingParser.PROCESSED.equals(strDataEntitiyModState))
        {
            // these are set because of the dummy stream
            m_metadata.remove(HttpHeaders.CONTENT_ENCODING);
            m_metadata.remove(HttpHeaders.CONTENT_TYPE);
            processProcessedData(m_metadata);
        }
        else
            processNewData(m_metadata, this.toString());



        // da wir diesen handler über die rekursiven Aufrufe wiederverwenden möchten, setzen wir hier die members zurück. Das metadata-Object wird im
        // CrawlerParser zurückgesetzt
        if(m_writer != null)
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
     * @param metadata    the metadata of the data entity
     * @param strFulltext the full body text of the data entity
     */
    public abstract void processModifiedData(Metadata metadata, String strFulltext);



    /**
     * Will be invoked in the case a new data entity was found.
     *
     * @param metadata    the metadata of the data entity
     * @param strFulltext the full body text of the data entity
     */
    public abstract void processNewData(Metadata metadata, String strFulltext);



    /**
     * This is invoked if we have an entity that was processed in this crawl yet. This is if we have somehow a double entry, or if we have cycles, e.g. during a web
     * crawl, where we sometimes come back to a link we started from.
     *
     * @param metadata some metadata (at least an identifying Id) to deal with the entity
     */
    abstract public void processProcessedData(Metadata metadata);



    /**
     * Will be invoked in the case a data entity was removed since the last crawl.
     *
     * @param metadata some metadata (at least an identifying Id) to deal with the removed entity
     */
    public abstract void processRemovedData(Metadata metadata);



    /**
     * This is invoked if we have an entity that was crawled at another crawl in the past, according to the crawling history, and was not modified, according to the
     * dataEntityContentFingerprint.
     *
     * @param metadata some metadata (at least an identifying Id) to deal with the entity
     */
    abstract public void processUnmodifiedData(Metadata metadata);



    public void setMetaData(Metadata metadata)
    {
        m_metadata = metadata;
    }



    /**
     * This is to make Tika setContentHandler public
     */
    public void setTikaContentHandler(ContentHandler handler)
    {
        setContentHandler(handler);
    }



    @Override
    public void startDocument() throws SAXException
    {
        init();
        super.startDocument();
    }

}
