package de.dfki.km.leech.parser;



import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.logging.Logger;

import javax.mail.URLName;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.DublinCore;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.html.HtmlParser;
import org.apache.tika.sax.Link;
import org.apache.tika.sax.LinkContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import de.dfki.km.leech.Leech;
import de.dfki.km.leech.config.CrawlerContext;
import de.dfki.km.leech.config.HtmlCrawlerContext;
import de.dfki.km.leech.io.URLStreamProvider;
import de.dfki.km.leech.parser.incremental.IncrementalCrawlingParser;
import de.dfki.km.leech.util.MultiValueHashMap;
import de.dfki.km.leech.util.StringUtils;
import de.dfki.km.leech.util.UrlUtil;



/**
 * A CrawlerParser implementation that can crawl html files. The content of the html file is simply delegated to {@link HtmlParser}, then all links
 * will be extracted with {@link LinkContentHandler} and recursively processed again with Leech. Configure it by specifying a {@link CrawlerContext}
 * and a {@link HtmlCrawlerContext} object inside the {@link ParseContext} object for the crawl.
 * 
 * @author Christian Reuschling, Dipl.Ing.(BA)
 */
public class HtmlCrawlerParser extends CrawlerParser
{

    private static final long serialVersionUID = -8214006342702249257L;

    protected static final Set<MediaType> SUPPORTED_TYPES = Collections.unmodifiableSet(new HashSet<MediaType>(Arrays.asList(MediaType.text("html"),
            MediaType.application("xhtml+xml"), MediaType.application("vnd.wap.xhtml+xml"), MediaType.application("x-asp"))));



    protected Leech m_leech;




    protected HtmlParser m_tikaHtmlParser = new HtmlParser();



    /**
     * Checks whether this URL is inside the configured constraints (domainname, some other strings, regex contstraints) or not
     * 
     * @param strContainerURL the url from the current container to check whether it is a remote or a local one
     * @param strURL2Check the URL to check whether it is in the configured constraints
     * @param crawlerContext the context object with the general constraints
     * @param htmlCrawlerContext the context object specific for the html parser
     * 
     * @return true in the case the URL is inside the constraints, false otherwise
     */
    protected boolean checkIfInConstraints(String strContainerURL, String strURL2Check, CrawlerContext crawlerContext,
            HtmlCrawlerContext htmlCrawlerContext)
    {
        if(crawlerContext == null) return true;

        // ist der container local?
        if(strContainerURL.startsWith("file:") && !strURL2Check.startsWith("file:") && !htmlCrawlerContext.getFollowRemoteLinksIfLocalFileCrawl())
        {
            if(crawlerContext.getVerbose())
                Logger.getLogger(CrawlerParser.class.getName()).info(
                        "URL " + strURL2Check
                                + " is a remote link and thus will not followed while crawling a local html file (as configured). Skipping.");

            return false;
        }


        if(!crawlerContext.getURLFilter().accept(strURL2Check))
        {
            if(crawlerContext.getVerbose())
                Logger.getLogger(CrawlerParser.class.getName()).info(
                        "URL " + strURL2Check + " is outside the URL constraints for this data source. Skipping.");

            return false;
        }


        return true;
    }







    @Override
    protected Iterator<MultiValueHashMap<String, Object>> getSubDataEntitiesInformation(InputStream stream, ContentHandler handler,
            Metadata metadata, ParseContext context) throws Exception
    {

        HashSet<URLName> hsLinkzAndI = new HashSet<URLName>();
        CrawlerContext crawlerContext = context.get(CrawlerContext.class);
        HtmlCrawlerContext htmlCrawlerContext = context.get(HtmlCrawlerContext.class, new HtmlCrawlerContext());

        String strContainerURL = metadata.get(DublinCore.SOURCE);


        // ## die Linkz - wir werden hier mal unsere links mit Tika auslesen - das ist konsequent und wird von anderen weiterentwickelt ;)

        LinkContentHandler linkContentHandler = new LinkContentHandler();

        m_tikaHtmlParser.parse(stream, linkContentHandler, metadata, context);


        for (Link link : linkContentHandler.getLinks())
        {
            if(StringUtils.nullOrWhitespace(link.getUri())) continue;

            try
            {
                String strExternalForm = new URL(link.getUri()).toExternalForm();
                strExternalForm = UrlUtil.normalizeURL(new URLName(strExternalForm)).toString();


                // wir verfolgen auch nur die Links, die innerhalb der userconstraints liegen
                if(checkIfInConstraints(strContainerURL, strExternalForm, crawlerContext, htmlCrawlerContext))
                    hsLinkzAndI.add(new URLName(strExternalForm));

            }
            catch (Exception e)
            {
                // ignore link
            }
        }



        LinkedList<MultiValueHashMap<String, Object>> llDataEntityInfos = new LinkedList<MultiValueHashMap<String, Object>>();

        for (URLName url4link : hsLinkzAndI)
        {
            MultiValueHashMap<String, Object> entityInfo = new MultiValueHashMap<String, Object>();

            url4link = UrlUtil.normalizeURL(url4link);
            entityInfo.add(CrawlerParser.SOURCEID, url4link.toString());
            entityInfo.add("url", url4link);

            llDataEntityInfos.add(entityInfo);
        }


        return llDataEntityInfos.iterator();
    }



    @Override
    public Set<MediaType> getSupportedTypes(ParseContext context)
    {
        return SUPPORTED_TYPES;
    }



    @Override
    public void parse(InputStream stream, ContentHandler handler, Metadata metadata, ParseContext context) throws IOException, SAXException,
            TikaException
    {
        // wenn wir nicht genug metadatan haben, dann fallen wir auf den Standard-Tika-htmlparser zurück. Das ist der Fall, wenn wir ein eingebettetes
        // html-file haben, z.b. ein html-file in einem zip. Die werden dann nicht gecrawlt, der Inhalt wird ganz einfach extrahiert.
        String strSource = metadata.get(DublinCore.SOURCE);

        if(StringUtils.nullOrWhitespace(strSource))
        {
            m_tikaHtmlParser.parse(stream, handler, metadata, context);

            return;
        }


        super.parse(stream, handler, metadata, context);
    }



    @Override
    protected void processCurrentDataEntity(InputStream stream, Metadata metadata, ContentHandler handler, ParseContext context) throws Exception
    {

        // der Inhalt der momentanen Seite wird verarbeitet - aber nur, wenn sich der Inhalt auch verändert hat (nicht unmodified)
        String strDataEntityModState = metadata.get(IncrementalCrawlingParser.DATA_ENTITY_MODIFICATION_STATE);

        if(IncrementalCrawlingParser.UNMODIFIED.equals(strDataEntityModState)) return;


        m_tikaHtmlParser.parse(stream, handler, metadata, context);
    }



    @Override
    protected void processSubDataEntity(MultiValueHashMap<String, Object> subDataEntityInformation, Metadata metadata,
            ContentHandler handler2use4recursiveCall, ParseContext context) throws Exception
    {

        URLName url = (URLName) subDataEntityInformation.getFirst("url");

        metadata = URLStreamProvider.getURLStreamProvider(url).addFirstMetadata(url, metadata, context);
        InputStream stream = URLStreamProvider.getURLStreamProvider(url).getStream(url, metadata, context);


        try
        {

            if(m_leech == null) m_leech = new Leech();


            Parser parser = m_leech.getParser();

            parser.parse(stream, handler2use4recursiveCall, metadata, context);

        }
        finally
        {
            if(stream != null) stream.close();
        }

    }






}
