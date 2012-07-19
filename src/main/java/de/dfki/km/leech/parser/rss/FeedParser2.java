package de.dfki.km.leech.parser.rss;



import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.tika.exception.TikaException;
import org.apache.tika.io.CloseShieldInputStream;
import org.apache.tika.metadata.DublinCore;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AbstractParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.XHTMLContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;

import de.dfki.km.leech.config.CrawlerContext;
import de.dfki.km.leech.parser.incremental.IncrementalCrawlingHistory;
import de.dfki.km.leech.parser.incremental.IncrementalCrawlingParser;
import de.dfki.km.leech.util.TikaUtils;



/**
 * Feed parser. This version is derived from the Tika Feed Parser, but gives more metadata and every feed entry as a single document
 */
public class FeedParser2 extends AbstractParser
{


    private static final long serialVersionUID = 1326997408920690592L;

    private static final Set<MediaType> SUPPORTED_TYPES = Collections.unmodifiableSet(new HashSet<MediaType>(Arrays.asList(
            MediaType.application("rss+xml"), MediaType.application("atom+xml"))));



    @Override
    public Set<MediaType> getSupportedTypes(ParseContext context)
    {
        return SUPPORTED_TYPES;
    }



    @Override
    public void parse(InputStream stream, ContentHandler handler, Metadata metadata, ParseContext context) throws IOException, SAXException,
            TikaException
    {
        // set the encoding?
        try
        {
            CrawlerContext crawlerContext = context.get(CrawlerContext.class);
            if(crawlerContext == null) crawlerContext = new CrawlerContext();

            IncrementalCrawlingHistory crawlingHistory = crawlerContext.getIncrementalCrawlingHistory();


            SyndFeed feed = new SyndFeedInput().build(new InputSource(new CloseShieldInputStream(stream)));

            String title = stripTags(feed.getTitleEx().getValue());
            String description = stripTags(feed.getDescriptionEx().getValue());

            metadata.set(Metadata.TITLE, title);
            metadata.set(Metadata.DESCRIPTION, description);
            // store the other fields in the metadata

            XHTMLContentHandler xhtml = new XHTMLContentHandler(handler, metadata);
            xhtml.startDocument();

            xhtml.element("h1", title);
            xhtml.element("p", description);

            xhtml.endDocument();

            String strContentType = metadata.get(Metadata.CONTENT_TYPE);

            for (Object e : feed.getEntries())
            {
                SyndEntry entry = (SyndEntry) e;



                String strLink = entry.getLink();
                if(strLink != null)
                {

                    XHTMLContentHandler xhtmlSubDoc = new XHTMLContentHandler(handler, metadata);
                    xhtmlSubDoc.startDocument();

                    TikaUtils.clearMetadata(metadata);

                    // hier wollen wir mit unseren dataexistsID und contentFingerprint prüfen, ob dieser Entry schon mal indexiert wurde
                    metadata.add(IncrementalCrawlingHistory.dataEntityExistsID, strLink);
                    metadata.add(IncrementalCrawlingHistory.dataEntityContentFingerprint, entry.getPublishedDate().toString());

                    IncrementalCrawlingParser.performHistoryStuff(crawlingHistory, metadata);

                    if(IncrementalCrawlingParser.UNMODIFIED.equals(metadata.get(IncrementalCrawlingParser.DATA_ENTITY_MODIFICATION_STATE))) continue;


                    metadata.add(Metadata.CONTENT_TYPE, strContentType);
                    metadata.add(DublinCore.SOURCE, strLink);
                    metadata.add(DublinCore.TITLE, stripTags(entry.getTitle()));
                    metadata.add(DublinCore.CREATOR, entry.getAuthor());
                    metadata.add(DublinCore.MODIFIED, entry.getPublishedDate().toString());

                    
                    xhtmlSubDoc.startElement("p");
                    String strCleanedText = stripTags(entry.getDescription().getValue());
                    xhtmlSubDoc.characters(strCleanedText.toCharArray(), 0, strCleanedText.length());
                    xhtmlSubDoc.endElement("p");

                    xhtmlSubDoc.endDocument();
                }

            }




        }
        catch (Exception e)
        {
            throw new TikaException("RSS parse error", e);
        }

    }



    protected static String stripTags(String value)
    {
        if(value == null) return "";


        String[] parts = value.split("<[^>]*>");
        StringBuffer buf = new StringBuffer();

        for (String part : parts)
            buf.append(part);

        return buf.toString().trim();
    }
}
