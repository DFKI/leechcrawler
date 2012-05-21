package de.dfki.km.leech.sax;



import org.apache.tika.metadata.Metadata;



public class DataSinkContentHandlerAdapter extends DataSinkContentHandler
{

    @Override
    public void processErrorData(Metadata metadata)
    {
        // NOP - implement on your own as you wish
    }



    @Override
    public void processModifiedData(Metadata metadata, String strFulltext)
    {
        // NOP - implement on your own as you wish
    }



    @Override
    public void processNewData(Metadata metadata, String strFulltext)
    {
        // NOP - implement on your own as you wish
    }



    @Override
    public void processRemovedData(Metadata metadata)
    {
        // NOP - implement on your own as you wish
    }

}
