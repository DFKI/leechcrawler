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



import de.dfki.inquisitor.collections.ValueBox;
import de.dfki.inquisitor.text.StringUtils;
import de.dfki.km.leech.Leech;
import de.dfki.km.leech.SubDataEntityContentHandler;
import de.dfki.km.leech.config.CrawlerContext;
import de.dfki.km.leech.io.URLStreamProvider;
import de.dfki.km.leech.parser.DirectoryCrawlerParser;
import de.dfki.km.leech.sax.DataSinkContentHandler;
import de.dfki.km.leech.sax.DataSinkContentHandlerAdapter;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.CompositeParser;
import org.apache.tika.parser.DefaultParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import javax.mail.URLName;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;



public class TikaUtils
{



    protected static DefaultParser defaultParserWithoutHistoryAndUrl = new DefaultParser();

    protected static Parser defaultLeechParser = new Leech().getParser();



    /**
     * Clears all data from this metadta object
     *
     * @param metadata2clear
     */
    public static void clearMetadata(Metadata metadata2clear)
    {
        for (String strKey : metadata2clear.names())
            metadata2clear.remove(strKey);
    }



    /**
     * Creates a new metadta object and copies all data from the given object inside
     *
     * @param metadata2copy
     *
     * @return the copy
     */
    public static Metadata copyMetadata(Metadata metadata2copy)
    {
        Metadata metadataCopy = new Metadata();
        for (String strKey : metadata2copy.names())
            for (String strVal : metadata2copy.getValues(strKey))
                metadataCopy.add(strKey, strVal);

        return metadataCopy;
    }



    /**
     * Copies all data from one metadata object to another
     *
     * @param metadata2copyFrom source
     * @param metadata2copyTo   target
     */
    public static void copyMetadataFromTo(Metadata metadata2copyFrom, Metadata metadata2copyTo)
    {
        for (String strKey : metadata2copyFrom.names())
            for (String strVal : metadata2copyFrom.getValues(strKey))
                metadata2copyTo.add(strKey, strVal);
    }



    /**
     * Returns the content handler object given inside crawlerContext or creates a new handler according to the configured class (also configured
     * inside crawlerContext)
     *
     * @param crawlerContext the crawlerContext configuration. The method will throw an exception in the case it is null
     *
     * @return the content handler object given inside crawlerContext or creates a new handler according to the configured class (also configured
     * inside crawlerContext)
     */
    static public ContentHandler createContentHandler4SubCrawl(CrawlerContext crawlerContext)
    {

        if(crawlerContext == null) throw new IllegalStateException("no crawlerContext was set");


        ContentHandler handler2use4recursiveCall = crawlerContext.getContentHandler();


        if(!StringUtils.nullOrWhitespace(crawlerContext.getContentHandlerClassName())) try
        {
            handler2use4recursiveCall = (ContentHandler) Class.forName(crawlerContext.getContentHandlerClassName()).newInstance();
        }
        catch (Exception e)
        {
            LoggerFactory.getLogger(DirectoryCrawlerParser.class.getName())
                    .error("Error during the instantiation of the configured content handler " + crawlerContext.getContentHandlerClassName(), e);
        }


        if(handler2use4recursiveCall == null) throw new IllegalStateException("No ContentHandler was set. Have a look into the class CrawlerContext");


        return handler2use4recursiveCall;
    }



    /**
     * Delegates parsing a stream with according metadata to the Tika default parser which selects the right parser implementation automatically according the mimetype in the given
     * metadata. The metadata object will be filled with new metadata from the selected parser. The parsed fulltext is returned. This is all WITHOUT history, etc. so you can use
     * this method without side effects inside own parser implementations
     */
    public static String delegateParsing(InputStream stream, Metadata metadata) throws TikaException, IOException, SAXException
    {
        ValueBox<String> strBodyTextBox = new ValueBox<>();
        DataSinkContentHandler newHandler = new DataSinkContentHandlerAdapter(metadata)
        {
            @Override
            public void processNewData(Metadata metadata, String strFulltext)
            {
                strBodyTextBox.setValue(strFulltext);
            }
        };

        defaultParserWithoutHistoryAndUrl.parse(stream, newHandler, metadata, new ParseContext());

        return strBodyTextBox.getValue();
    }



    /**
     * Can be used to delegate recursive crawling with history and url filtering - i.e. full fletched leech crawling. Configure the crawl as usual inside the ParseContext
     * parameter object. This method makes the same as Leech.parse() with the difference that you can add own metadata attributes to the result
     */
    public static void delegateCrawling(String strUrl, Metadata metadata2use4recursiveCall, ContentHandler handler2use4recursiveCall, ParseContext context) throws Exception
    {
        URLName url = new URLName(strUrl);


        url = UrlUtil.normalizeURL(url);

        context.set(Parser.class, defaultLeechParser);

        CrawlerContext crawlerContext = context.get(CrawlerContext.class);
        if(crawlerContext == null)
        {
            crawlerContext = new CrawlerContext();
            context.set(CrawlerContext.class, crawlerContext);
        }
        crawlerContext.setContentHandler(handler2use4recursiveCall);




        metadata2use4recursiveCall = URLStreamProvider.getURLStreamProvider(url).addFirstMetadata(url, metadata2use4recursiveCall, context);

        try (InputStream stream = URLStreamProvider.getURLStreamProvider(url).getStream(url, metadata2use4recursiveCall, context))
        {
            defaultLeechParser.parse(stream, handler2use4recursiveCall, metadata2use4recursiveCall, context);
        }


    }



    /**
     * Convenience method to trigger an (subdata )entity handling. This is also usefull if you want to delegate parsing tasks to another parser in your own parser. This method
     * can not deal wlth possible history tasks, which is not necessary e.g. when delegating a parsing task.
     */
    public static void writeData2TikaHandler(ContentHandler handler, Metadata metadata, String strBodyText) throws SAXException
    {
        SubDataEntityContentHandler.triggerEntityHandling(handler, metadata, strBodyText);
    }




    /**
     * Determines the configured Parser inside a CompositeParser for a given type.
     *
     * @param compoParser the CompositeParser that offers several parser implementaions for several types
     * @param type        the type we want to have the according parser for
     * @param context     the parseContext object. Can be null for context-insensitive processing
     *
     * @return the parser for the given type
     */
    public static Parser getParser4Type(CompositeParser compoParser, MediaType type, ParseContext context)
    {
        Map<MediaType, Parser> map;

        if(context != null) map = compoParser.getParsers(context);
        else map = compoParser.getParsers();

        // We always work on the normalised, canonical form
        if(type != null) type = compoParser.getMediaTypeRegistry().normalize(type);

        while (type != null)
        {
            // Try finding a parser for the type
            Parser parser = map.get(type);
            if(parser != null)
            {
                if(parser instanceof CompositeParser) return getParser4Type((CompositeParser) parser, type, context);

                return parser;
            }

            // Failing that, try for the parent of the type
            type = compoParser.getMediaTypeRegistry().getSupertype(type);
        }


        return compoParser.getFallback();
    }







}
