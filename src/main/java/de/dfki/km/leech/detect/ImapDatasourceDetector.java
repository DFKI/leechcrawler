package de.dfki.km.leech.detect;



import java.io.IOException;
import java.io.InputStream;

import javax.mail.URLName;

import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.DublinCore;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;

import de.dfki.km.leech.util.UrlUtil;



public class ImapDatasourceDetector implements Detector
{

    private static final long serialVersionUID = -71053688105617836L;



    @Override
    public MediaType detect(InputStream input, Metadata metadata) throws IOException
    {

        try
        {
            String strName = metadata.get(DublinCore.SOURCE);
            if(strName == null) strName = metadata.get(Metadata.RESOURCE_NAME_KEY);

            // octet stream wird zurück gegeben, wenn wir nix erkennen konnten
            if(strName == null) return MediaType.OCTET_STREAM;



            URLName url = new URLName(strName);
            if(url.getProtocol().equalsIgnoreCase("imap") || url.getProtocol().equalsIgnoreCase("imaps"))
            {
                //TODO für den Sven muß hier noch die MessageId gültig sein
                if(UrlUtil.extractUID(url) == null)
                    return DatasourceMediaTypes.IMAPFOLDER;
                else
                    return MediaType.parse("message/rfc822");
                    
            }


            return MediaType.OCTET_STREAM;

        }
        catch (Exception e)
        {
            return MediaType.OCTET_STREAM;
        }


    }

}
