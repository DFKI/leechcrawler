package de.dfki.km.leech.io;



import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
        if(!(metadata2fill.get(DublinCore.SOURCE) == null || metadata2fill.get(DublinCore.MODIFIED) == null
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
        metadata2fill.set(DublinCore.SOURCE, file.toURI().toURL().toString());
        // Optional
        metadata2fill.set(DublinCore.MODIFIED, String.valueOf(file.lastModified()));

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
    public ShiftInitInputStream getStream(URLName url2getStream, Metadata metadata, ParseContext parseContext) throws Exception
    {
        final URL asUrl = new URL(url2getStream.toString());

        return new ShiftInitInputStream()
        {
            @Override
            protected InputStream initBeforeFirstStreamDataAccess() throws Exception
            {
                return TikaInputStream.get(asUrl.openStream());
            }
        }; 
    }




    @Override
    public Set<String> getSupportedProtocols()
    {
        return Collections.singleton("file");
    }

}
