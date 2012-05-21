package de.dfki.km.leech.detect;

import org.apache.tika.mime.MediaType;

public class DatasourceMediaTypes
{
    
    static public final MediaType DIRECTORY = new MediaType("filesystemdatasource", "directory"); 
    
    //TODO: sollte das besser imapDirectory heißen?
    static public final MediaType IMAPFOLDER = new MediaType("remotedatasource", "imapfolder");
    
}
