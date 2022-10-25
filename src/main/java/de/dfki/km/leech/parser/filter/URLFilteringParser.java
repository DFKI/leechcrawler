/*
    Leech - crawling capabilities for Apache Tika
    
    Copyright (C) 2012 DFKI GmbH, Author: Christian Reuschling

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

    Contact us by mail: christian.reuschling@dfki.de
*/

package de.dfki.km.leech.parser.filter;



import de.dfki.inquisitor.collections.MultiValueHashMap;
import de.dfki.km.leech.config.CrawlerContext;
import de.dfki.km.leech.metadata.LeechMetadata;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.ParserDecorator;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;



/**
 * This is a parser decorator that blocks delegating to the wrapped parser according to constraints on metadata entries. With this you can constrain
 * the crawling process to some root directories, web domains, ... or if you want to exclude some specific directories/files/links, etc.. For
 * filtering, {@link URLFilteringParser} uses the {@link URLFilter} Object specified in the {@link ParseContext}.
 *
 * @author Christian Reuschling, Dipl.Ing.(BA)
 */
public class URLFilteringParser extends ParserDecorator
{


    public static class URLFilteringParserContext
    {

        public MultiValueHashMap<String, String> redirect2OriginSource = new MultiValueHashMap<>();

    }




    private static final long serialVersionUID = 7864760975795972594L;

    public boolean m_bAcceptSuceedingRedirects = true;

    Set<String> m_hsMetadataKeys = new HashSet<String>();




    /**
     * Creates an URLFilteringParser according to the Metadata.SOURCE metadata entries
     *
     * @param parser the parser to decorate
     */
    public URLFilteringParser(Parser parser)
    {
        this(parser, Metadata.SOURCE);
    }



    /**
     * Creates an {@link URLFilteringParser} by specifing the metadata entries
     *
     * @param parser       the parser to decorate
     * @param metadataKeys the keys under which the metadata entries should be checked
     */
    public URLFilteringParser(Parser parser, String... metadataKeys)
    {
        super(parser);

        m_hsMetadataKeys.addAll(Arrays.asList(metadataKeys));
    }




    @Override
    public void parse(InputStream stream, ContentHandler handler, Metadata metadata, ParseContext context) throws IOException, SAXException, TikaException
    {

        CrawlerContext crawlerContext = context.get(CrawlerContext.class);
        if(crawlerContext == null) crawlerContext = new CrawlerContext();


        URLFilteringParserContext urlFilteringParserContext = context.get(URLFilteringParserContext.class);
        if(urlFilteringParserContext == null)
        {
            urlFilteringParserContext = new URLFilteringParserContext();
            context.set(URLFilteringParserContext.class, urlFilteringParserContext);
        }



        String strSource = metadata.get(Metadata.SOURCE);
        if(strSource == null) strSource = metadata.get(LeechMetadata.RESOURCE_NAME_KEY);


        // beim webcrawling gibt es manchmal redirects die wir auch erfassen wollen, auch wenn sie nicht explizit spezifiziert sind. Da sind dann in den Metadaten originalsource
        // und source unterschiedlich. Die möchten wir uns merken, damit spätere - rekursiv gecrawlte Urls ohne diese Info - trotzdem akzeptiert werden
        if(m_hsMetadataKeys.contains(Metadata.SOURCE) && metadata.get(LeechMetadata.originSource) != null && m_bAcceptSuceedingRedirects)
        {
            if(!metadata.get(Metadata.SOURCE).equals(metadata.get(LeechMetadata.originSource)))
                urlFilteringParserContext.redirect2OriginSource.add(metadata.get(Metadata.SOURCE), metadata.get(LeechMetadata.originSource));
        }

        // ## URLFilter - wenn unsere zu parsende entity ausserhalb der Domäne steht, dann ignorieren wir sie auch
        for (String strKey : m_hsMetadataKeys)
        {
            String strValue = metadata.get(strKey);


            HashSet<String> sValidUrls = new HashSet<>();
            sValidUrls.add(strValue);

            if(m_bAcceptSuceedingRedirects)
            {
                // wenn wir einen redirect angegeben haben, dann haben wir im Filter eventuell das originale spezifiziert - das gilt dann auch
                String strUrlWithOrigin = null;
                for (Map.Entry<String, String> redirect2Origin : urlFilteringParserContext.redirect2OriginSource.entryList())
                {
                    if(strValue.startsWith(redirect2Origin.getKey()))
                    {
                        strUrlWithOrigin = strValue.replace(redirect2Origin.getKey(), redirect2Origin.getValue());
                        sValidUrls.add(strUrlWithOrigin);
                    }
                }
            }


            // Wir setzen http:// und https:// gleich, diese werden auch akzeptiert, wenn die Url ohne Protokoll angegeben wurde

            for (String strValidUrl : new LinkedList<>(sValidUrls))
            {
                if(strValidUrl.startsWith("http://") || strValidUrl.startsWith("https://"))
                {
                    String strUrlBlank = strValidUrl.replaceFirst("https?://", "");
                    sValidUrls.add(strUrlBlank);

                    String strUrlHttp = "http://" + strUrlBlank;
                    sValidUrls.add(strUrlHttp);

                    String strUrlHttps = "https://" + strUrlBlank;
                    sValidUrls.add(strUrlHttps);
                }
            }


            // wenn auch nur einer akzeptiert wird, dann ist gut
            boolean bSkip = true;
            for (String strValidUrl : sValidUrls)
            {
                if(crawlerContext.getURLFilter().accept(strValidUrl))
                {
                    bSkip = false;
                    break;
                }
            }



            if(bSkip)
            {
                if(crawlerContext.getVerbose()) LoggerFactory.getLogger(URLFilteringParser.class.getName())
                        .info("Data entity " + strSource + " is outside the URL constraints for this data source. Skipping.");

                return;
            }
        }


        getWrappedParser().parse(stream, handler, metadata, context);
    }

}
