package de.dfki.km.leech.sax;



import org.apache.tika.metadata.Metadata;



/**
 * A simple class that delegates all method invocations to the wrapped {@link DataSinkContentHandler}. This class is for convienience, that someone who writes a decorator
 * mustn't implement all methods. Thus, a better name for this class would be DataSinkContentHandlerDecoratorAdapter... but this was too much for me ;)
 * 
 * @author reuschling
 */
public class DataSinkContentHandlerDecorator extends DataSinkContentHandler
{

    protected DataSinkContentHandler m_wrappedDataSinkContentHandler;



    public DataSinkContentHandlerDecorator()
    {
    }



    public DataSinkContentHandlerDecorator(Metadata metadata)
    {
        super(metadata);
    }



    public DataSinkContentHandlerDecorator(DataSinkContentHandler wrappedDataSinkContentHandler)
    {
        m_wrappedDataSinkContentHandler = wrappedDataSinkContentHandler;
    }



    @Override
    public void crawlFinished()
    {
        if(m_wrappedDataSinkContentHandler != null) m_wrappedDataSinkContentHandler.crawlFinished();
    }



    public DataSinkContentHandler getWrappedDataSinkContentHandler()
    {
        return m_wrappedDataSinkContentHandler;
    }






    @Override
    public void processErrorData(Metadata metadata)
    {
        if(m_wrappedDataSinkContentHandler != null) m_wrappedDataSinkContentHandler.processErrorData(metadata);
    }



    @Override
    public void processModifiedData(Metadata metadata, String strFulltext)
    {
        if(m_wrappedDataSinkContentHandler != null) m_wrappedDataSinkContentHandler.processModifiedData(metadata, strFulltext);
    }



    @Override
    public void processNewData(Metadata metadata, String strFulltext)
    {
        if(m_wrappedDataSinkContentHandler != null) m_wrappedDataSinkContentHandler.processNewData(metadata, strFulltext);
    }



    @Override
    public void processProcessedData(Metadata metadata)
    {
        if(m_wrappedDataSinkContentHandler != null) m_wrappedDataSinkContentHandler.processProcessedData(metadata);
    }



    @Override
    public void processRemovedData(Metadata metadata)
    {
        if(m_wrappedDataSinkContentHandler != null) m_wrappedDataSinkContentHandler.processRemovedData(metadata);
    }



    @Override
    public void processUnmodifiedData(Metadata metadata)
    {
        if(m_wrappedDataSinkContentHandler != null) m_wrappedDataSinkContentHandler.processUnmodifiedData(metadata);
    }



    /**
     * Sets the Object to decorate / wrapp
     * 
     * @return this for convenience
     */
    public DataSinkContentHandlerDecorator setWrappedDataSinkContentHandler(DataSinkContentHandler wrappedDataSinkContentHandler)
    {
        this.m_wrappedDataSinkContentHandler = wrappedDataSinkContentHandler;

        return this;
    }

}
