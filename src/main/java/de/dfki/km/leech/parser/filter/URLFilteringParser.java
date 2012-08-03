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



import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.DublinCore;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.ParserDecorator;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import de.dfki.km.leech.config.CrawlerContext;
import de.dfki.km.leech.parser.CrawlerParser;



/**
 * This is a parser decorator that blocks delegating to the wrapped parser according to constraints on metadata entries. With this you can constrain
 * the crawling process to some root directories, web domains, ... or if you want to exclude some specific directories/files/links, etc.. For
 * filtering, {@link URLFilteringParser} uses the {@link URLFilter} Object specified in the {@link ParseContext}.
 * 
 * @author Christian Reuschling, Dipl.Ing.(BA)
 */
public class URLFilteringParser extends ParserDecorator
{


    private static final long serialVersionUID = 7864760975795972594L;



    Set<String> m_hsMetadataKeys = new HashSet<String>();



    /**
     * Creates an URLFilteringParser according to the Metadata.SOURCE metadata entries
     * 
     * @param parser the parser to decorate
     */
    public URLFilteringParser(Parser parser)
    {
        this(parser, Metadata.SOURCE);
//        this(parser, Metadata.SOURCE);
    }



    /**
     * Creates an {@link URLFilteringParser} by specifing the metadata entries
     * 
     * @param parser the parser to decorate
     * @param metadataKeys the keys under which the metadata entries should be checked
     */
    public URLFilteringParser(Parser parser, String... metadataKeys)
    {
        super(parser);

        m_hsMetadataKeys.addAll(Arrays.asList(metadataKeys));
    }



    @Override
    public void parse(InputStream stream, ContentHandler handler, Metadata metadata, ParseContext context) throws IOException, SAXException,
            TikaException
    {

        CrawlerContext crawlerContext = context.get(CrawlerContext.class);
        if(crawlerContext == null) crawlerContext = new CrawlerContext();

        String strSource = metadata.get(Metadata.SOURCE);
        if(strSource == null) strSource = metadata.get(Metadata.RESOURCE_NAME_KEY);

        // ## URLFilter - wenn unsere zu parsende entity ausserhalb der Domäne steht, dann ignorieren wir sie auch
        for (String strKey : m_hsMetadataKeys)
        {
            String strValue = metadata.get(strKey);
            if(!crawlerContext.getURLFilter().accept(strValue))
            {
                if(crawlerContext.getVerbose())
                    Logger.getLogger(CrawlerParser.class.getName()).info(
                            "Data entity " + strSource + " is outside the URL constraints for this data source. Skipping.");

                return;
            }
        }


        getWrappedParser().parse(stream, handler, metadata, context);
    }

}
