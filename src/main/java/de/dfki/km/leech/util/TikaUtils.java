package de.dfki.km.leech.util;



import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.CompositeParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.xml.sax.ContentHandler;

import de.dfki.km.leech.config.CrawlerContext;
import de.dfki.km.leech.parser.DirectoryCrawlerParser;



public class TikaUtils
{



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
     * Returns the content handler object given inside crawlerContext or creates a new handler according to the configured class (also configured
     * inside crawlerContext)
     * 
     * @param crawlerContext the crawlerContext configuration. The method will throw an exception in the case it is null
     * 
     * @return the content handler object given inside crawlerContext or creates a new handler according to the configured class (also configured
     *         inside crawlerContext)
     */
    static public ContentHandler createContentHandler4SubCrawl(CrawlerContext crawlerContext)
    {

        if(crawlerContext == null) throw new IllegalStateException("no crawlerContext was set");


        ContentHandler handler2use4recursiveCall = crawlerContext.getContentHandler();


        if(!StringUtils.nullOrWhitespace(crawlerContext.getContentHandlerClassName()))
            try
            {
                handler2use4recursiveCall = (ContentHandler) Class.forName(crawlerContext.getContentHandlerClassName()).newInstance();
            }
            catch (Exception e)
            {
                Logger.getLogger(DirectoryCrawlerParser.class.getName()).log(Level.SEVERE,
                        "Error during the instantiation of the configured content handler " + crawlerContext.getContentHandlerClassName(), e);
            }


        if(handler2use4recursiveCall == null)
            throw new IllegalStateException("No ContentHandler was set. Have a look into the class CrawlerContext");


        return handler2use4recursiveCall;
    }







    /**
     * Determines the configured Parser inside a CompositeParser for a given type.
     * 
     * @param compoParser the CompositeParser that offers several parser implementaions for several types
     * @param type the type we want to have the according parser for
     * @param context the parseContext object
     * 
     * @return the parser for the given type
     */
    public static Parser getParser4Type(CompositeParser compoParser, MediaType type, ParseContext context)
    {
        Map<MediaType, Parser> map = compoParser.getParsers(context);

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
