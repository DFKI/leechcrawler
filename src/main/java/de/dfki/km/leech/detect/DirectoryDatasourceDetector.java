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

            String strName = metadata.get(Metadata.SOURCE);
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
