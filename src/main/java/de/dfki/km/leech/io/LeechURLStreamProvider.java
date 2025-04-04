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

package de.dfki.km.leech.io;



import de.dfki.km.leech.metadata.LeechMetadata;
import de.dfki.km.leech.parser.incremental.IncrementalCrawlingHistory;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.ParseContext;

import javax.mail.URLName;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;



/**
 * This class is for specifying arbitrary urls that should be processed by new, arbitrary Parsers. You can specify any (conecction) information into the URL, as far as it is
 * prefixed with "leech://". The rest of the URL is then passed to the Parser as a String. The Parser can then do whatever it wants with this information.
 * <br>
 * 1. This URLStreamProvider gives just dummy content for the stream. The parser should collect all relevant content outgoing from the URL.
 * 2. To specify a new Parser, register it inside META-INF/services/org.apache.tika.parser.Parser. Tika will then find it.
 * 3. Register a new mimetype for the new Parser inside org.apache.tika.mime/custom-mimetypes.xml.
 * 4. Let your Detector return the new mimetype for the new Parser, with the help of the leech://-URL.
 */
public class LeechURLStreamProvider extends URLStreamProvider
{



    @Override
    public Metadata addFirstMetadata(URLName url2getMetadata, Metadata metadata2fill, ParseContext parseContext) throws Exception
    {
        if(metadata2fill == null)
            metadata2fill = new Metadata();


        // wenn das Teil schon gefüllt ist, dann machen wir gar nix
        if(!(metadata2fill.get(Metadata.SOURCE) == null || metadata2fill.get(TikaCoreProperties.MODIFIED) == null
                || metadata2fill.get(IncrementalCrawlingHistory.dataEntityId) == null || metadata2fill.get(IncrementalCrawlingHistory.dataEntityContentFingerprint) == null
                || metadata2fill.get(LeechMetadata.RESOURCE_NAME_KEY) == null))
        {
            // alle sind bereits gesetzt
            return metadata2fill;
        }


        // es fehlt mindestens eines, das wir hier dazupacken wollen - wir machen alles neu

        String strEntityId =url2getMetadata.toString();
        // Für Leech
        metadata2fill.set(Metadata.SOURCE, strEntityId);

        // Für das inkrementelle indexieren


        metadata2fill.set(IncrementalCrawlingHistory.dataEntityId, strEntityId);
        // hier müssen wir darauf achten, dass immer etwas anderes kommt - sonst wird das Dokument nicht neu indexiert, weil es als unmodified erkannt wird.
        metadata2fill.set(IncrementalCrawlingHistory.dataEntityContentFingerprint, String.valueOf(System.currentTimeMillis()));


        // Für Tika
        metadata2fill.set(LeechMetadata.RESOURCE_NAME_KEY, strEntityId);


        return metadata2fill;
    }




    @Override
    public TikaInputStream getStream(URLName url2getStream, Metadata metadata, ParseContext parseContext) throws Exception
    {
        return TikaInputStream.get(new ByteArrayInputStream("LeechUrlStreamProvider - the parser is responsible to get the content.".getBytes(StandardCharsets.UTF_8)));
    }




    @Override
    public Set<String> getSupportedProtocols()
    {
        return Collections.singleton("leech");
    }

}
