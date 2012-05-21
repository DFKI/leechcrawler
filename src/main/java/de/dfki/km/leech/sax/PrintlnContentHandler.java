package de.dfki.km.leech.sax;





import java.util.logging.Logger;

import org.apache.tika.metadata.DublinCore;
import org.apache.tika.metadata.Metadata;

import de.dfki.km.leech.parser.incremental.IncrementalCrawlingHistory;



/**
 * A DataSinkContentHandler that simply prints out the data she recieves. You can also optionally specify another contentHandler that should be
 * wrapped - in this case the data will be printed out and then everything will be delegated to the wrapped contentHandler
 * 
 * @author Christian Reuschling, Dipl.Ing.(BA)
 */
public class PrintlnContentHandler extends DataSinkContentHandler
{


    static public enum Granularity {
        all, fulltext, metadata, nothing, title, titlePlusFulltext, titlePlusMetadata
    }




    protected Granularity m_granularity = Granularity.all;


    protected boolean m_showOnlyErrors = false;



    public boolean isShowOnlyErrors()
    {
        return m_showOnlyErrors;
    }



    public PrintlnContentHandler setShowOnlyErrors(boolean showOnlyErrors)
    {
        m_showOnlyErrors = showOnlyErrors;
        
        return this;
    }



    protected DataSinkContentHandler m_wrappedDataSinkContentHandler;



    public PrintlnContentHandler()
    {
        super();
    }



    public PrintlnContentHandler(DataSinkContentHandler wrappedDataSinkContentHandler)
    {
        super();

        m_wrappedDataSinkContentHandler = wrappedDataSinkContentHandler;
    }



    public PrintlnContentHandler(Granularity granularity)
    {
        super();
        m_granularity = granularity;
    }



    public PrintlnContentHandler setGranularity(Granularity granularity)
    {
        m_granularity = granularity;
        
        return this;
    }



    public Granularity getGranularity()
    {
        return m_granularity;
    }




    public PrintlnContentHandler(Granularity granularity, DataSinkContentHandler wrappedDataSinkContentHandler)
    {
        super();

        m_granularity = granularity;
        m_wrappedDataSinkContentHandler = wrappedDataSinkContentHandler;
    }



    public PrintlnContentHandler(Metadata metadata)
    {
        super(metadata);
    }



    public PrintlnContentHandler(Metadata metadata, DataSinkContentHandler wrappedDataSinkContentHandler)
    {
        super(metadata);

        m_wrappedDataSinkContentHandler = wrappedDataSinkContentHandler;
    }



    public PrintlnContentHandler(Metadata metadata, Granularity granularity)
    {
        super(metadata);
        m_granularity = granularity;
    }



    public PrintlnContentHandler(Metadata metadata, Granularity granularity, DataSinkContentHandler wrappedDataSinkContentHandler)
    {
        super(metadata);

        m_granularity = granularity;
        m_wrappedDataSinkContentHandler = wrappedDataSinkContentHandler;
    }



    public DataSinkContentHandler getWrappedDataSinkContentHandler()
    {
        return m_wrappedDataSinkContentHandler;
    }




    @Override
    public void processModifiedData(Metadata metadata, String strFulltext)
    {


        if(!m_showOnlyErrors)
        {

            StringBuilder strbMessage = new StringBuilder();


            if(m_granularity != Granularity.nothing) strbMessage.append("## PrintlnContentHandler MODIFIED data ##########################\n");

            if(m_granularity == Granularity.all || m_granularity == Granularity.title || m_granularity == Granularity.titlePlusMetadata
                    || m_granularity == Granularity.titlePlusFulltext)
            {
                String strInfo = metadata.get(IncrementalCrawlingHistory.dataEntityExistsID);
                if(strInfo == null) strInfo = metadata.get(DublinCore.SOURCE);
                if(strInfo == null) strInfo = metadata.get(Metadata.RESOURCE_NAME_KEY);

                strbMessage.append(strInfo).append("\n");
            }


            if(m_granularity == Granularity.all || m_granularity == Granularity.metadata || m_granularity == Granularity.titlePlusMetadata)
            {
                strbMessage.append("## metadata:\n");
                for (String strFieldName : metadata.names())
                {
                    for (String strValue : metadata.getValues(strFieldName))
                        strbMessage.append(strFieldName + ": '" + strValue + "'\n");
                }
            }


            if(m_granularity == Granularity.all || m_granularity == Granularity.fulltext || m_granularity == Granularity.titlePlusFulltext)
            {
                strFulltext = strFulltext.replaceAll("[\\n\\s]+", " ");
                strFulltext = strFulltext.substring(0, Math.min(strFulltext.length(), 2345));
                strbMessage.append("## fulltext (without newlines, reduced whitespace, fixed length): \n" + strFulltext).append("\n");
            }

            if(m_granularity != Granularity.nothing) strbMessage.append("\n");


            if(m_granularity != Granularity.nothing) Logger.getLogger(PrintlnContentHandler.class.getName()).info(strbMessage.toString());

        }

        if(m_wrappedDataSinkContentHandler != null) m_wrappedDataSinkContentHandler.processModifiedData(metadata, strFulltext);
    }



    @Override
    public void processNewData(Metadata metadata, String strFulltext)
    {

        if(!m_showOnlyErrors)
        {

            StringBuilder strbMessage = new StringBuilder();

            if(m_granularity != Granularity.nothing) strbMessage.append("## PrintlnContentHandler - NEW data ##########################\n");

            if(m_granularity == Granularity.all || m_granularity == Granularity.title || m_granularity == Granularity.titlePlusMetadata
                    || m_granularity == Granularity.titlePlusFulltext)
            {
                String strInfo = metadata.get(IncrementalCrawlingHistory.dataEntityExistsID);
                if(strInfo == null) strInfo = metadata.get(DublinCore.SOURCE);
                if(strInfo == null) strInfo = metadata.get(Metadata.RESOURCE_NAME_KEY);

                strbMessage.append(strInfo).append("\n");
            }


            if(m_granularity == Granularity.all || m_granularity == Granularity.metadata || m_granularity == Granularity.titlePlusMetadata)
            {
                strbMessage.append("## metadata:\n");
                for (String strFieldName : metadata.names())
                {
                    for (String strValue : metadata.getValues(strFieldName))
                        strbMessage.append(strFieldName + ": '" + strValue + "'\n");
                }
            }


            if(m_granularity == Granularity.all || m_granularity == Granularity.fulltext || m_granularity == Granularity.titlePlusFulltext)
            {
                strFulltext = strFulltext.replaceAll("[\\n\\s]+", " ");
                strFulltext = strFulltext.substring(0, Math.min(strFulltext.length(), 2345));
                strbMessage.append("## fulltext (without newlines, reduced whitespace, fixed length): \n" + strFulltext).append("\n");
            }

            if(m_granularity != Granularity.nothing) strbMessage.append("\n");


            if(m_granularity != Granularity.nothing) Logger.getLogger(PrintlnContentHandler.class.getName()).info(strbMessage.toString());

        }

        if(m_wrappedDataSinkContentHandler != null) m_wrappedDataSinkContentHandler.processNewData(metadata, strFulltext);
    }



    @Override
    public void processRemovedData(Metadata metadata)
    {
        if(!m_showOnlyErrors)
        {

            StringBuilder strbMessage = new StringBuilder();

            if(m_granularity != Granularity.nothing) strbMessage.append("## PrintlnContentHandler REMOVED data ##########################\n");

            if(m_granularity == Granularity.all || m_granularity == Granularity.title || m_granularity == Granularity.titlePlusMetadata
                    || m_granularity == Granularity.titlePlusFulltext)
            {
                String strInfo = metadata.get(IncrementalCrawlingHistory.dataEntityExistsID);
                if(strInfo == null) strInfo = metadata.get(DublinCore.SOURCE);
                if(strInfo == null) strInfo = metadata.get(Metadata.RESOURCE_NAME_KEY);

                strbMessage.append(strInfo).append("\n");
            }


            if(m_granularity == Granularity.all || m_granularity == Granularity.metadata || m_granularity == Granularity.titlePlusMetadata)
            {
                strbMessage.append("## metadata:\n");
                for (String strFieldName : metadata.names())
                {
                    for (String strValue : metadata.getValues(strFieldName))
                        strbMessage.append(strFieldName + ": '" + strValue + "'\n");
                }
            }



            if(m_granularity != Granularity.nothing) strbMessage.append("\n");


            if(m_granularity != Granularity.nothing) Logger.getLogger(PrintlnContentHandler.class.getName()).info(strbMessage.toString());

        }

        if(m_wrappedDataSinkContentHandler != null) m_wrappedDataSinkContentHandler.processRemovedData(metadata);
    }



    @Override
    public void processErrorData(Metadata metadata)
    {
        StringBuilder strbMessage = new StringBuilder();

        if(m_granularity != Granularity.nothing) strbMessage.append("## PrintlnContentHandler ERROR data ##########################\n");

        if(m_granularity == Granularity.all || m_granularity == Granularity.title || m_granularity == Granularity.titlePlusMetadata
                || m_granularity == Granularity.titlePlusFulltext)
        {
            String strInfo = metadata.get(IncrementalCrawlingHistory.dataEntityExistsID);
            if(strInfo == null) strInfo = metadata.get(DublinCore.SOURCE);
            if(strInfo == null) strInfo = metadata.get(Metadata.RESOURCE_NAME_KEY);

            strbMessage.append(strInfo).append("\n");
        }


        if(m_granularity == Granularity.all || m_granularity == Granularity.metadata || m_granularity == Granularity.titlePlusMetadata)
        {
            // errorMessage
            // errorStacktrace
            strbMessage.append("## metadata:\n");
            for (String strFieldName : metadata.names())
            {
                for (String strValue : metadata.getValues(strFieldName))
                    strbMessage.append(strFieldName + ": '" + strValue + "'\n");
            }
        }



        if(m_granularity != Granularity.nothing) strbMessage.append("\n");


        if(m_granularity != Granularity.nothing) Logger.getLogger(PrintlnContentHandler.class.getName()).info(strbMessage.toString());


        if(m_wrappedDataSinkContentHandler != null) m_wrappedDataSinkContentHandler.processErrorData(metadata);

    }







}
