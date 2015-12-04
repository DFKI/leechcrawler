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

package de.dfki.km.leech.parser;



import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import de.dfki.inquisition.collections.MultiValueHashMap;
import de.dfki.km.leech.config.CrawlerContext;
import de.dfki.km.leech.parser.incremental.IncrementalCrawlingParser;
import de.dfki.km.leech.sax.DataSinkContentHandler;
import de.dfki.km.leech.util.ExceptionUtils;
import de.dfki.km.leech.util.TikaUtils;



/**
 * This is the upper class for all crawling parsers. If you want to write a crawling parser, implement this class. CrawlerParser will first invoke
 * {@link #processCurrentDataEntity(InputStream, Metadata, ContentHandler, ParseContext)} to process the input stream and pushing it to a ContentHandler, simply the
 * standard Tika parsing way. Next, it will call {@link #getSubDataEntitiesInformation(InputStream, ContentHandler, Metadata, ParseContext)} to determine all succeeding
 * sub data entities from this data entity. CrawlerParser then iterates over these entries and give them to
 * {@link #processSubDataEntity(MultiValueHashMap, Metadata, ContentHandler, ParseContext)} in order to further process the sub data entities individually. This is the
 * recursive call, which starts the whole parsing process again with a new entity. <br>
 * <br>
 * The crawling process can be configured with specific context classes, have a look into the 'config' package, especially {@link CrawlerContext}.
 * 
 * @author Christian Reuschling, Dipl.Ing.(BA)
 */
public abstract class CrawlerParser implements Parser
{





    private static final long serialVersionUID = -6707880965147815349L;

    static public final String CURRENT_CRAWLING_DEPTH = "currentCrawlingDepth";

    static public final String SOURCEID = "sourceId";







    /**
     * Gets information about all data entities that should be (sub)crawled by this crawler instance. This e.g. could be all files and directories inside the current
     * directory. You can return arbritrary information about a data entity - it will be offered as-is at the invocation of
     * {@link #processSubDataEntity(MultiValueHashMap, Metadata, ContentHandler, ParseContext)} in order to deal with it. <br>
     * <br>
     * To consider constraints given from the user for Url/datasource string filtering, use the potential CrawlerContext Object inside the ParseContext and use the
     * URLFilter. Same is for the stop request, which is also offered by the CrawlerContext. Leech deals automatically with stop requests and data entity filtering, but
     * you can enhance the performance when you filter subentities early in this class. This is because otherwise there will be a stream initialization or established
     * connection before filtering. <br>
     * <br>
     * While creating the information Map for a (sub) data entity, it is recommended to put at least one key entry with CrawlerParser.SOURCEID for use in potential error
     * messages, to identify a problematic data entity. In the case you do so, you can simply throw all Exceptions inside your implementation of processSubDataEntity, the
     * super class will deal with it.<br>
     * <br>
     * 
     * @param stream the stream-parameter from the Parser.parse(InputStream stream, ContentHandler handler, Metadata metadata, ParseContext context) invocation
     * @param handler the handler-parameter from the Parser.parse(InputStream stream, ContentHandler handler, Metadata metadata, ParseContext context) invocation
     * @param metadata a copy of the metadata-parameter from the Parser.parse(InputStream stream, ContentHandler handler, Metadata metadata, ParseContext context)
     *            invocation
     * @param context the context-parameter from the Parser.parse(InputStream stream, ContentHandler handler, Metadata metadata, ParseContext context) invocation
     * 
     * @return an iterator with all information about a data entity that should be crawled, that is enough to deal with it inside the other method implementations
     * 
     * @throws Exception
     */
    abstract protected Iterator<MultiValueHashMap<String, Object>> getSubDataEntitiesInformation(InputStream stream, ContentHandler handler, Metadata metadata,
            ParseContext context) throws Exception;






    @Override
    public void parse(InputStream stream, ContentHandler handler, Metadata metadata, ParseContext context) throws IOException, SAXException, TikaException
    {


        CrawlerContext crawlerContext = context.get(CrawlerContext.class);
        if(crawlerContext == null) crawlerContext = new CrawlerContext();



        String strSourceURL = metadata.get(Metadata.SOURCE);
        int iCurrentCrawlingDepth = 0;
        TikaInputStream tmpStream = null;

        try
        {

            String strDepth = metadata.get(CURRENT_CRAWLING_DEPTH);
            if(strDepth != null) iCurrentCrawlingDepth = Integer.valueOf(strDepth);




            // ## die momentan zu crawlende entity (der potentielle container mit pot. eigenem Inhalt)

            String strDataEntityModState = metadata.get(IncrementalCrawlingParser.DATA_ENTITY_MODIFICATION_STATE);

            if(!IncrementalCrawlingParser.UNMODIFIED.equals(strDataEntityModState))
            {
                // hier ist es vermutlich besser, auf das tmp-file-Angebot vom TikaStream einzugehen - die Platte wird vermutlich schneller sein als
                // eine durchschnittliche Internetverbindung.Auch schreibend. Sollte kein File hinter dem stream stecken (z.B. bei einer
                // http-connection) wird Tika automatisch ein temporäres File erzeugen.
                tmpStream = TikaInputStream.get((TikaInputStream.get(stream).getPath()));

                ContentHandler handler2use4recursiveCall = TikaUtils.createContentHandler4SubCrawl(crawlerContext);

                processCurrentDataEntity(tmpStream, metadata, handler2use4recursiveCall, context);
            }






            // ## die SubEntities - machen wir nur, wenn wir nicht schon die maximale crawlingdepth erreicht haben


            Iterator<MultiValueHashMap<String, Object>> subDataEntitiesInformation;

            if(iCurrentCrawlingDepth + 1 > crawlerContext.getCrawlingDepth())
                subDataEntitiesInformation = new LinkedList<MultiValueHashMap<String, Object>>().iterator();
            else
            {
                // wir kopieren das Metadata-Teil hier, damit wir in der Schleife das Original-Objekt verwenden können (der iterator wird evtl. erst
                // während des Schleifendurchlaufs in einem anderen Thread beschickt, und da sollte das Metadata-Objekt noch gültig sein. Wir
                // verändern allerdings dessen Inhalte in der Schleife
                subDataEntitiesInformation = getSubDataEntitiesInformation(stream, handler, TikaUtils.copyMetadata(metadata), context);
            }



            int iEntityIndex = 0;
            while (subDataEntitiesInformation.hasNext() && !crawlerContext.stopRequested())
            {


                MultiValueHashMap<String, Object> subDataEntityInfo = subDataEntitiesInformation.next();

                // bei jeder Entität schauen wir, ob wir einen neuen Handler erzeugen müssen
                ContentHandler handler2use4recursiveCall = TikaUtils.createContentHandler4SubCrawl(crawlerContext);
                try
                {


                    // wir löschen die Inhalte im Metadata-Objekt, da wir zwar die Referenz behalten wollen (falls ein Handler das auch hat), aber die
                    // Inhalte für die subEntity neu gefüllt werden sollen.
                    TikaUtils.clearMetadata(metadata);

                    // wir tragen dann noch die aktuelle depth ein, damit wir gegebenenfalls abbrechen können
                    metadata.set(CURRENT_CRAWLING_DEPTH, String.valueOf(iCurrentCrawlingDepth + 1));


                    processSubDataEntity(subDataEntityInfo, metadata, handler2use4recursiveCall, context);



                }
                catch (Throwable e)
                {
                    Object sourceId = subDataEntityInfo.getFirst(SOURCEID);

                    ExceptionUtils.handleException(e, sourceId == null ? "noSourceId" : sourceId.toString(), metadata, crawlerContext, context, iCurrentCrawlingDepth,
                            handler2use4recursiveCall);
                }


                iEntityIndex++;

                if(iEntityIndex % 10000 == 0)
                {
                    // twice is full gc
                    System.gc();
                    System.gc();
                }

            }




            if(iCurrentCrawlingDepth != 0) return;



            // am Schluß auch noch die Metadata abräumen, falls man die an einem anderen Leech-Aufruf wiederverwenden will
            TikaUtils.clearMetadata(metadata);



        }
        catch (Exception e)
        {
            if(e instanceof TikaException) throw (TikaException) e;
            throw new TikaException("Error while crawling '" + strSourceURL + "'", e);
        }
        finally
        {

            if(tmpStream != null) tmpStream.close();



            // hier wollen wir auch noch brav unterbrechen, wenn ein stop requested wurde
            Boolean bStopRequested = crawlerContext.stopRequested();
            synchronized (bStopRequested)
            {
                if(bStopRequested && iCurrentCrawlingDepth == 0) bStopRequested.notifyAll();
            }
        }

    }



    /**
     * Processes the current data entity that should be parsed. This method extracts the content by e.g. delegating the stream to a specific Parser in order to push the
     * content to the ContentHandler, whereby the {@link #getSubDataEntitiesInformation(InputStream, ContentHandler, Metadata, ParseContext)} method extracts all the
     * links to other data entites out of this content, for further processing them individually. <br>
     * <br>
     * For example, the {@link HtmlCrawlerParser} simply delegates the parameters to a Tika HtmlParser Object when its unmodified (this info is inside the metadata
     * possibly generated by {@link IncrementalCrawlingParser}).
     * 
     * @param stream a 'cloned' stream from the stream given from the parse method
     * @param metadata the metadata given from the parse method
     * @param handler the origin content handler instance from the parse method, OR an instance created newly at every data entity as configured inside CrawlerContext
     * @param context the ParseContext Object given from the parse method
     * 
     * @throws Exception
     */
    abstract protected void processCurrentDataEntity(InputStream stream, Metadata metadata, ContentHandler handler, ParseContext context) throws Exception;



    /**
     * Processes a sub data entity from this parsed 'container' data entity - this can be the recursive call, in the case you have other complex data types behind your
     * sub data entities, which needs further parsing again. In this case, normally you invoke some kind of Leech.parse(...) method here, e.g.<br>
     * <br>
     * <code>
     * Parser parser = m_leech.getParser();<br>
     * parser.parse(stream, handler2use4recursiveCall, metadata, context);<br>
     * <br>
     * </code> In the other case, you have all the information yet, ready for the final handler. In this case, you can send it directly, without further processing: <br>
     * <br>
     * <code>
     *  SubDataEntityContentHandler subHandler = new SubDataEntityContentHandler(handler, metadata, strBody);<br>
     *   if(ignoreHistory)<br>
     *         &nbsp;&nbsp;subHandler.triggerSubDataEntityHandling();<br>
     *   else<br>
     *       &nbsp;&nbsp;subHandler.triggerSubDataEntityHandling(context);<br>
     *  </code> <br>
     * The stream and possible additional metadata entries you get(or create) out of information inside the subDataEntityInformation. Make sure that you reuse the
     * metadata Object for the case that the handler has also an internal metadata member that must be the same object (as inside {@link DataSinkContentHandler})
     * 
     * @param subDataEntityInformation one entry out of the formerly returned iterator from
     *            {@link #getSubDataEntitiesInformation(InputStream, ContentHandler, Metadata, ParseContext)}
     * @param metadata2use4recursiveCall the metadata object that should be used for handling / recursive calls
     * @param handler2use4recursiveCall the origin content handler instance from the root crawl invocation, OR an instance created newly at every data entity as
     *            configured inside CrawlerContext
     * @param context the origin ParseContext instance given from the parse method
     * 
     * @throws Exception
     */
    abstract protected void processSubDataEntity(MultiValueHashMap<String, Object> subDataEntityInformation, Metadata metadata2use4recursiveCall,
            ContentHandler handler2use4recursiveCall, ParseContext context) throws Exception;




}
