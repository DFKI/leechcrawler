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



import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Set;

import javax.mail.URLName;

import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.DublinCore;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;

import de.dfki.km.leech.parser.incremental.IncrementalCrawlingHistory;



public class FileURLStreamProvider extends URLStreamProvider
{



    @Override
    public Metadata addFirstMetadata(URLName url2getMetadata, Metadata metadata2fill, ParseContext parseContext) throws Exception
    {
        if(metadata2fill == null) metadata2fill = new Metadata();


        // wenn das Teil schon gefüllt ist, dann machen wir gar nix
        if(!(metadata2fill.get(Metadata.SOURCE) == null || metadata2fill.get(Metadata.MODIFIED) == null
                || metadata2fill.get(IncrementalCrawlingHistory.dataEntityExistsID) == null
                || metadata2fill.get(IncrementalCrawlingHistory.dataEntityContentFingerprint) == null || metadata2fill
                    .get(Metadata.RESOURCE_NAME_KEY) == null))
        {
            // alle sind bereits gesetzt
            return metadata2fill;
        }


        // es fehlt mindestens eines, das wir hier dazupacken wollen - wir machen alles neu


        File file = new File(new URL(url2getMetadata.toString()).toURI());

        // Für Leech
        metadata2fill.set(Metadata.SOURCE, file.toURI().toURL().toString());
        // Optional
        metadata2fill.set(Metadata.MODIFIED, String.valueOf(file.lastModified()));

        // Für das inkrementelle indexieren
        String strEntityExistsId;
        try
        {
            strEntityExistsId = file.getCanonicalPath();
        }
        catch (IOException e)
        {
            strEntityExistsId = file.getAbsolutePath();
        }

        metadata2fill.set(IncrementalCrawlingHistory.dataEntityExistsID, strEntityExistsId);
        metadata2fill.set(IncrementalCrawlingHistory.dataEntityContentFingerprint, String.valueOf(file.lastModified()));


        // Für Tika
        metadata2fill.set(Metadata.RESOURCE_NAME_KEY, strEntityExistsId);


        return metadata2fill;
    }




    @Override
    public TikaInputStream getStream(URLName url2getStream, Metadata metadata, ParseContext parseContext) throws Exception
    {
        URL asUrl = new URL(url2getStream.toString());

        File ourFile = new File(asUrl.toURI());

        if(ourFile.isDirectory()) return TikaInputStream.get(new ByteArrayInputStream("leech sucks - hopefully :)".getBytes()));

        return TikaInputStream.get(ourFile);
    }




    @Override
    public Set<String> getSupportedProtocols()
    {
        return Collections.singleton("file");
    }

}
