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

package de.dfki.km.leech.detect;



import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.tika.config.ServiceLoader;
import org.apache.tika.detect.CompositeDetector;
import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MediaTypeRegistry;
import org.apache.tika.mime.MimeTypes;
import org.apache.tika.parser.CompositeParser;
import org.apache.tika.parser.EmptyParser;
import org.apache.tika.parser.Parser;

import de.dfki.km.leech.util.TikaUtils;



/**
 * A detector implementation that detects everything from the tika DefaultDetector, plus some extra datasource detectors (e.g. for directories)
 * 
 * @author Christian Reuschling, Dipl.Ing.(BA)
 */
public class LeechDefaultDetector extends CompositeDetector
{

    private static final long serialVersionUID = -4879286813440313595L;



    protected static List<Detector> getDefaultDetectors(MimeTypes types, ServiceLoader loader)
    {
        List<Detector> detectors = new ArrayList<Detector>();

        detectors.add(new DirectoryDatasourceDetector());
        detectors.add(new ImapDatasourceDetector());

        detectors.addAll(loader.loadServiceProviders(Detector.class));
        detectors.add(types);



        return detectors;
    }



    private CompositeParser m_usedCompoParserFromCrawlConfig = null;



    protected final MediaTypeRegistry registry;



    public LeechDefaultDetector()
    {
        this(MimeTypes.getDefaultMimeTypes());
    }



    public LeechDefaultDetector(ClassLoader loader)
    {
        this(MimeTypes.getDefaultMimeTypes(), loader);
    }



    /**
     * Initializes the detector with the Parser instance that is used for crawling. In the case this parser is known, the detector checks whether a known media type will
     * result into an EmptyParser. In this case, it will try to detect the a possibly better media type by checking the stream.
     * 
     * @param compoParser the Parser instance that is used for crawling
     */
    public LeechDefaultDetector(CompositeParser compoParser)
    {
        this(MimeTypes.getDefaultMimeTypes());
        this.m_usedCompoParserFromCrawlConfig = compoParser;
    }



    public LeechDefaultDetector(MimeTypes types)
    {
        this(types, new ServiceLoader());
    }



    public LeechDefaultDetector(MimeTypes types, ClassLoader loader)
    {
        this(types, new ServiceLoader(loader));
    }



    private LeechDefaultDetector(MimeTypes types, ServiceLoader loader)
    {
        super(types.getMediaTypeRegistry(), getDefaultDetectors(types, loader));
        registry = types.getMediaTypeRegistry();
    }



    @Override
    public MediaType detect(InputStream input, Metadata metadata) throws IOException
    {
        // wenn in den Metadaten schon eins drin steht, dann werten wir den stream hier nicht nochmal extra aus.
        // wenn wir den media type schon wissen, aber lediglich der EmptyParser damit assoziiert wäre, dann kucken wir trotzdem noch mal nach
        // (magic bytes und so)
        String strType = metadata.get(Metadata.CONTENT_TYPE);
        if(strType != null)
        {
            // wenn es schon bekannt ist, checken wir ab, was für ein Parser hierfür ausgewählt werden würde.
            MediaType mediaType = MediaType.parse(strType);

            // wir müssen noch abchecken, ob es nicht noch einen spezialisierteren Parser gibt
            if(registry != null)
            {
                MediaType detectedType2check = super.detect(input, metadata);

                if((mediaType == null && detectedType2check != null) || registry.isSpecializationOf(detectedType2check.getBaseType(), mediaType.getBaseType()))
                {
                    metadata.remove(Metadata.CONTENT_TYPE);
                    metadata.set(Metadata.CONTENT_TYPE, detectedType2check.toString());

                    mediaType = detectedType2check;
                }
            }

            if(m_usedCompoParserFromCrawlConfig == null) return mediaType;

            Parser parser4Type = TikaUtils.getParser4Type(m_usedCompoParserFromCrawlConfig, mediaType, null);

            if(!(parser4Type instanceof EmptyParser)) return mediaType;
        }

        return super.detect(input, metadata);
    }


}
