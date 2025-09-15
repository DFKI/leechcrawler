package de.dfki.km.leech;



import de.dfki.km.leech.config.CrawlerContext;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;



public abstract class LeechAbstractParser implements Parser
{

    // hierwarich - ich möchte eine methode zum schicken übergeben von subItems, das hier soll vom AbstractParser erben. Meine existierenden Parser sollen dann von hier erben,
    // der SubDataEntityContentHandler kann dann zB in 'utils'



    @Override
    public void parse(InputStream stream, ContentHandler handler, Metadata metadata, ParseContext context) throws IOException, SAXException, TikaException
    {
        m_lastParseMetadata = metadata;
        m_lastParseContext = context;

    }



    public void triggerSubDataEntityHandling(Metadata metadata, String strBody, boolean bIgnoreHistory)
    {
        try
        {
            SubDataEntityContentHandler subHandler = new SubDataEntityContentHandler(context.get(CrawlerContext.class), metadata, strBody);

            if (bIgnoreHistory)
                subHandler.triggerSubDataEntityHandling();
            else
                subHandler.triggerSubDataEntityHandling(context);
        } catch (SAXException e)
        {
            throw new RuntimeException(e);
        }
    }
}
