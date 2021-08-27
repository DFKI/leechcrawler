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
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Set;



public class FileURLStreamProvider extends URLStreamProvider
{



    @Override
    public Metadata addFirstMetadata(URLName url2getMetadata, Metadata metadata2fill, ParseContext parseContext) throws Exception
    {
        if(metadata2fill == null) metadata2fill = new Metadata();


        // wenn das Teil schon gefüllt ist, dann machen wir gar nix
        if(!(metadata2fill.get(Metadata.SOURCE) == null || metadata2fill.get(TikaCoreProperties.MODIFIED) == null
                || metadata2fill.get(IncrementalCrawlingHistory.dataEntityId) == null
                || metadata2fill.get(IncrementalCrawlingHistory.dataEntityContentFingerprint) == null || metadata2fill
                    .get(LeechMetadata.RESOURCE_NAME_KEY) == null))
        {
            // alle sind bereits gesetzt
            return metadata2fill;
        }


        // es fehlt mindestens eines, das wir hier dazupacken wollen - wir machen alles neu


        File file = new File(new URL(url2getMetadata.toString()).toURI());

        // Für Leech
        metadata2fill.set(Metadata.SOURCE, file.toURI().toURL().toString());
        // Optional
        metadata2fill.set(TikaCoreProperties.MODIFIED.getName(),
                new SimpleDateFormat("yyyy.MM.dd HH:mm:ss:SSS").format(new Date(file.lastModified())));

        // Für das inkrementelle indexieren
        String strEntityId;
        try
        {
            strEntityId = file.getCanonicalPath();
        }
        catch (IOException e)
        {
            strEntityId = file.getAbsolutePath();
        }

        metadata2fill.set(IncrementalCrawlingHistory.dataEntityId, strEntityId);
        metadata2fill.set(IncrementalCrawlingHistory.dataEntityContentFingerprint, String.valueOf(file.lastModified()));


        // Für Tika
        metadata2fill.set(LeechMetadata.RESOURCE_NAME_KEY, strEntityId);


        return metadata2fill;
    }




    @Override
    public TikaInputStream getStream(URLName url2getStream, Metadata metadata, ParseContext parseContext) throws Exception
    {
        URL asUrl = new URL(url2getStream.toString());

        File ourFile = new File(asUrl.toURI());

        if(ourFile.isDirectory()) return TikaInputStream.get(new ByteArrayInputStream("leech sucks - hopefully :)".getBytes("UTF-8")));

        return TikaInputStream.get(ourFile);
    }




    @Override
    public Set<String> getSupportedProtocols()
    {
        return Collections.singleton("file");
    }

}
