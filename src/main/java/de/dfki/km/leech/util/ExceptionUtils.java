/*
 * Leech - crawling capabilities for Apache Tika
 * 
 * Copyright (C) 2012 DFKI GmbH, Author: Christian Reuschling
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Contact us by mail: christian.reuschling@dfki.de
 */

package de.dfki.km.leech.util;



import de.dfki.km.leech.config.CrawlerContext;
import de.dfki.km.leech.metadata.LeechMetadata;
import de.dfki.km.leech.parser.CrawlerParser;
import de.dfki.km.leech.parser.incremental.IncrementalCrawlingHistory;
import de.dfki.km.leech.parser.incremental.IncrementalCrawlingParser;
import de.dfki.km.leech.sax.DataSinkContentHandler;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.EmptyParser;
import org.apache.tika.parser.ParseContext;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.*;



/**
 * A simple ultility class for dealing with Exceptions
 * 
 * @author Christian Reuschling, Dipl.Ing.(BA)
 */
public class ExceptionUtils
{
    /**
     * Creates the String out of the stacktrace and the cause of a Throwable. This is usefull for e.g. logging out Exception Messages
     * 
     * @param t the according Throwable
     * 
     * @return the message String together with the stacktrace and the cause
     */
    static public String createStackTraceString(Throwable t)
    {
        // pwir geben den Stacktrace und den Stacktrace des Grundes aus, falls einer existiert
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PrintStream stackTrace = new PrintStream(byteArrayOutputStream);
        t.printStackTrace(stackTrace);
        stackTrace.flush();
        try
        {
            byteArrayOutputStream.flush();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        StringBuilder strbWholeMessage = new StringBuilder();
        strbWholeMessage.append(byteArrayOutputStream.toString());

        // den rekursiven Aufruf brauchen wir nicht - den macht die printStackTrace-methode schon selber :)

        // if(t.getCause() != null)
        // strbWholeMessage.append("Cause of:\n").append(ExceptionUtils.createStackTraceString(t.getCause()));


        return strbWholeMessage.toString();
    }





    /**
     * Gets a throwable, create a string error message out of it (including the stacktrace), tries to create a {@link IncrementalCrawlingHistory}
     * .dataEntityId for identifying the entity, puts this all together into the metadata object, flag it as an error entity (with
     * metadata.set(IncrementalCrawlingParser.DATA_ENTITY_MODIFICATION_STATE, IncrementalCrawlingParser.ERROR);), and finally delegates it to an
     * EmptyParser with the given content handler and a dummy stream.<br>
     * This is for processing errors/error entities that occur during a crawl also by implementing the {@link ContentHandler} interface - or even
     * better the {@link DataSinkContentHandler} interface, which is recommended.
     * 
     * @param e thhe exception occured during the crawl
     * @param strSourceId some referencing ID - in the case it is null, the method will get Metadata.SOURCE from the metadata or, in the case this is
     *            also null, LeechMetadata.RESOURCE_NAME_KEY
     * @param metadata the metadata object for the data entity. Will be enhanced with the error message and given to the EmptyParser invocation
     * @param crawlerContext the original crawler configuration object
     * @param context the original ParseContext
     * @param iCurrentCrawlingDepth the current crawling depth. In the case the crawling process should be interrupted in the case of an exception,
     *            this method will also throw an exception. In the case the crawling depth is 0 in this case, the method will additionally show a log
     *            message
     * @param handler2use4recursiveCall the handler that is used for the crawling process
     * 
     * @throws TikaException will be thrown in the case the crawling should be interrupted in the case of an exception (as configured inside the
     *             CrawlerContext
     * @throws SAXException will be thrown in the case the EmptyParser.parse invocation will throw it
     */
    static public void handleException(Throwable e, String strSourceId, Metadata metadata, CrawlerContext crawlerContext, ParseContext context,
            int iCurrentCrawlingDepth, ContentHandler handler2use4recursiveCall) throws TikaException, SAXException
    {
        try
        {


            if(crawlerContext == null) crawlerContext = new CrawlerContext();

            String strUrlOrSource4SubEntity = strSourceId;

            if(strUrlOrSource4SubEntity == null) strUrlOrSource4SubEntity = metadata.get(Metadata.SOURCE);
            if(strUrlOrSource4SubEntity == null) strUrlOrSource4SubEntity = metadata.get(LeechMetadata.RESOURCE_NAME_KEY);
            if(strUrlOrSource4SubEntity == null)
                strUrlOrSource4SubEntity =
                        "no data entity id known - in the case of a sub-entity, set it inside the metadata at your implementation of getSubDataEntitiesInformation(..) "
                                + "under the key CrawlerParser.SOURCEID. Otherwise you maybe try to process an unsupported/broken URL, or it is totally strange.";




            metadata.set(IncrementalCrawlingParser.DATA_ENTITY_MODIFICATION_STATE, IncrementalCrawlingParser.ERROR);
            metadata.set(IncrementalCrawlingHistory.dataEntityId, strUrlOrSource4SubEntity);
            metadata.set("errorMessage", e.getMessage());
            metadata.set("errorStacktrace", ExceptionUtils.createStackTraceString(e));


            InputStream dummyStream = new ByteArrayInputStream("leech sucks - hopefully :)".getBytes("UTF-8"));


            // /////////////////////
            if(crawlerContext.getInterruptIfException())
            {
                if(iCurrentCrawlingDepth == 0)
                    LoggerFactory.getLogger(CrawlerParser.class.getName()).error("Error while processing " + strUrlOrSource4SubEntity, e);

                // wir geben nun einfach den Error weiter - in der Metadata
                EmptyParser.INSTANCE.parse(dummyStream, handler2use4recursiveCall, metadata, context);

                throw new TikaException("Error while processing " + strUrlOrSource4SubEntity, e);
            }

            // wir geben nun einfach den Error weiter - in der Metadata
            EmptyParser.INSTANCE.parse(dummyStream, handler2use4recursiveCall, metadata, context);


        }
        catch (UnsupportedEncodingException e1)
        {
            LoggerFactory.getLogger(ExceptionUtils.class.getName()).error("Error", e);
        }

    }

}
