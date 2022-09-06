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



import de.dfki.inquisitor.collections.MultiValueHashMap;
import de.dfki.inquisitor.text.StringUtils;
import de.dfki.km.leech.Leech;
import de.dfki.km.leech.config.CrawlerContext;
import de.dfki.km.leech.config.HtmlCrawlerContext;
import de.dfki.km.leech.io.URLStreamProvider;
import de.dfki.km.leech.parser.incremental.IncrementalCrawlingHistory;
import de.dfki.km.leech.parser.incremental.IncrementalCrawlingHistory.Exist;
import de.dfki.km.leech.parser.incremental.IncrementalCrawlingParser;
import de.dfki.km.leech.util.UrlUtil;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.EmptyParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.html.HtmlParser;
import org.apache.tika.sax.Link;
import org.apache.tika.sax.LinkContentHandler;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import javax.mail.URLName;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;



/**
 * A CrawlerParser implementation that can crawl html files. The content of the html file is simply delegated to {@link HtmlParser}, then all links will be extracted with
 * {@link LinkContentHandler} and recursively processed again with Leech. Configure it by specifying a {@link CrawlerContext} and a {@link HtmlCrawlerContext} object
 * inside the {@link ParseContext} object for the crawl.
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
    protected boolean checkIfInConstraints(String strContainerURL, String strURL2Check, CrawlerContext crawlerContext, HtmlCrawlerContext htmlCrawlerContext)
    {
        if(crawlerContext == null) return true;

        // ist der container local?
        if(strContainerURL.startsWith("file:") && !strURL2Check.startsWith("file:") && !htmlCrawlerContext.getFollowRemoteLinksIfLocalFileCrawl())
        {
            if(crawlerContext.getVerbose())
                LoggerFactory.getLogger(CrawlerParser.class.getName()).info(
                        "URL " + strURL2Check + " is a remote link and thus will not followed while crawling a local html file (as configured). Skipping.");

            return false;
        }


        if(!crawlerContext.getURLFilter().accept(strURL2Check))
        {
            if(crawlerContext.getVerbose())
                LoggerFactory.getLogger(CrawlerParser.class.getName()).info("URL " + strURL2Check + " is outside the URL constraints for this data source. Skipping.");

            return false;
        }


        return true;
    }







    @Override
    protected Iterator<MultiValueHashMap<String, Object>> getSubDataEntitiesInformation(InputStream stream, ContentHandler handler, Metadata metadata,
            ParseContext context) throws Exception
    {

        HashSet<URLName> hsLinkzAndI = new HashSet<URLName>();
        CrawlerContext crawlerContext = context.get(CrawlerContext.class);
        HtmlCrawlerContext htmlCrawlerContext = context.get(HtmlCrawlerContext.class, new HtmlCrawlerContext());

        String strContainerURL = metadata.get(Metadata.SOURCE);


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
                if(checkIfInConstraints(strContainerURL, strExternalForm, crawlerContext, htmlCrawlerContext)) hsLinkzAndI.add(new URLName(strExternalForm));

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
    public void parse(InputStream stream, ContentHandler handler, Metadata metadata, ParseContext context) throws IOException, SAXException, TikaException
    {
        // wenn wir nicht genug metadatan haben, dann fallen wir auf den Standard-Tika-htmlparser zurück. Das ist der Fall, wenn wir ein eingebettetes
        // html-file haben, z.b. ein html-file in einem zip. Die werden dann nicht gecrawlt, der Inhalt wird ganz einfach extrahiert.
        String strSource = metadata.get(Metadata.SOURCE);

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
    protected void processSubDataEntity(MultiValueHashMap<String, Object> subDataEntityInformation, Metadata metadata, ContentHandler handler2use4recursiveCall,
            ParseContext context) throws Exception
    {

        URLName url = (URLName) subDataEntityInformation.getFirst("url");


        // Performance: wenn wir es in diesem Crawl schon mal prozessiert haben (anhand der nicht-redirect-geprüften URL), skippen wir hier. Redirects checken dauert.
        CrawlerContext crawlerContext = context.get(CrawlerContext.class);
        if(crawlerContext != null)
        {
            IncrementalCrawlingHistory crawlingHistory = crawlerContext.getIncrementalCrawlingHistory();
            if(crawlingHistory != null)
            {
                Exist exist = crawlingHistory.exists(url.toString());

                if(exist.equals(Exist.YES_PROCESSED))
                {
                    metadata.set(IncrementalCrawlingParser.DATA_ENTITY_MODIFICATION_STATE, IncrementalCrawlingParser.PROCESSED);
                    
                    InputStream dummyStream = new ByteArrayInputStream("leech sucks - hopefully :)".getBytes(StandardCharsets.UTF_8));
                    EmptyParser.INSTANCE.parse(dummyStream, handler2use4recursiveCall, metadata, context);
                    
                    return;
                }
            }
        }



        metadata = URLStreamProvider.getURLStreamProvider(url).addFirstMetadata(url, metadata, context);


        try (InputStream stream = URLStreamProvider.getURLStreamProvider(url).getStream(url, metadata, context))
        {

            if(m_leech == null) m_leech = new Leech();


            Parser parser = m_leech.getParser();

            parser.parse(stream, handler2use4recursiveCall, metadata, context);
        }

    }






}
