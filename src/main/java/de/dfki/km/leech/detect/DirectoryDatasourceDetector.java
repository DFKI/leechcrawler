package de.dfki.km.leech.detect;



import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.DublinCore;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;



/**
 * A detector implementation that detects filesystem directories by checking whether the path is a directory
 * 
 * @author Christian Reuschling, Dipl.Ing.(BA)
 */
public class DirectoryDatasourceDetector implements Detector
{

    private static final long serialVersionUID = 4373590026286228337L;



    @Override
    public MediaType detect(InputStream input, Metadata metadata) throws IOException
    {

        try
        {

            String strName = metadata.get(DublinCore.SOURCE);
            if(strName == null) strName = metadata.get(Metadata.RESOURCE_NAME_KEY);

            // octet stream wird zurück gegeben, wenn wir nix erkennen konnten
            if(strName == null) return MediaType.OCTET_STREAM;


            File fName4Check;
            try
            {
                fName4Check = new File(new URL(strName).toURI());
            }
            catch (Exception e)
            {
                fName4Check = new File(strName);
            }

            if(!fName4Check.exists()) return MediaType.OCTET_STREAM;

            if(fName4Check.isDirectory()) return DatasourceMediaTypes.DIRECTORY;



            return MediaType.OCTET_STREAM;


        }
        catch (Exception e)
        {
            return MediaType.OCTET_STREAM;
        }
    }
}
