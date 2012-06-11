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


    static public enum Verbosity {
        all, fulltext, metadata, nothing, title, titlePlusFulltext, titlePlusMetadata
    }




    protected Verbosity m_verbosity = Verbosity.all;


    protected boolean m_showOnlyErrors = false;



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



    public PrintlnContentHandler(Verbosity granularity)
    {
        super();
        m_verbosity = granularity;
    }



    public PrintlnContentHandler(Verbosity granularity, DataSinkContentHandler wrappedDataSinkContentHandler)
    {
        super();

        m_verbosity = granularity;
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



    public PrintlnContentHandler(Metadata metadata, Verbosity granularity)
    {
        super(metadata);
        m_verbosity = granularity;
    }




    public PrintlnContentHandler(Metadata metadata, Verbosity granularity, DataSinkContentHandler wrappedDataSinkContentHandler)
    {
        super(metadata);

        m_verbosity = granularity;
        m_wrappedDataSinkContentHandler = wrappedDataSinkContentHandler;
    }



    public Verbosity getGranularity()
    {
        return m_verbosity;
    }



    public DataSinkContentHandler getWrappedDataSinkContentHandler()
    {
        return m_wrappedDataSinkContentHandler;
    }



    public boolean isShowOnlyErrors()
    {
        return m_showOnlyErrors;
    }



    @Override
    public void processErrorData(Metadata metadata)
    {
        StringBuilder strbMessage = new StringBuilder();

        if(m_verbosity != Verbosity.nothing) strbMessage.append("## PrintlnContentHandler ERROR data ##########################\n");

        if(m_verbosity == Verbosity.all || m_verbosity == Verbosity.title || m_verbosity == Verbosity.titlePlusMetadata
                || m_verbosity == Verbosity.titlePlusFulltext)
        {
            String strInfo = metadata.get(IncrementalCrawlingHistory.dataEntityExistsID);
            if(strInfo == null) strInfo = metadata.get(DublinCore.SOURCE);
            if(strInfo == null) strInfo = metadata.get(Metadata.RESOURCE_NAME_KEY);

            strbMessage.append(strInfo).append("\n");
        }


        if(m_verbosity == Verbosity.all || m_verbosity == Verbosity.metadata || m_verbosity == Verbosity.titlePlusMetadata)
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



        if(m_verbosity != Verbosity.nothing) strbMessage.append("\n");


        if(m_verbosity != Verbosity.nothing) Logger.getLogger(PrintlnContentHandler.class.getName()).info(strbMessage.toString());


        if(m_wrappedDataSinkContentHandler != null) m_wrappedDataSinkContentHandler.processErrorData(metadata);

    }



    @Override
    public void processModifiedData(Metadata metadata, String strFulltext)
    {


        if(!m_showOnlyErrors)
        {

            StringBuilder strbMessage = new StringBuilder();


            if(m_verbosity != Verbosity.nothing) strbMessage.append("## PrintlnContentHandler MODIFIED data ##########################\n");

            if(m_verbosity == Verbosity.all || m_verbosity == Verbosity.title || m_verbosity == Verbosity.titlePlusMetadata
                    || m_verbosity == Verbosity.titlePlusFulltext)
            {
                String strInfo = metadata.get(IncrementalCrawlingHistory.dataEntityExistsID);
                if(strInfo == null) strInfo = metadata.get(DublinCore.SOURCE);
                if(strInfo == null) strInfo = metadata.get(Metadata.RESOURCE_NAME_KEY);

                strbMessage.append(strInfo).append("\n");
            }


            if(m_verbosity == Verbosity.all || m_verbosity == Verbosity.metadata || m_verbosity == Verbosity.titlePlusMetadata)
            {
                strbMessage.append("## metadata:\n");
                for (String strFieldName : metadata.names())
                {
                    for (String strValue : metadata.getValues(strFieldName))
                        strbMessage.append(strFieldName + ": '" + strValue + "'\n");
                }
            }


            if(m_verbosity == Verbosity.all || m_verbosity == Verbosity.fulltext || m_verbosity == Verbosity.titlePlusFulltext)
            {
                strFulltext = strFulltext.replaceAll("[\\n\\s]+", " ");
                strFulltext = strFulltext.substring(0, Math.min(strFulltext.length(), 2345));
                strbMessage.append("## fulltext (without newlines, reduced whitespace, fixed length): \n" + strFulltext).append("\n");
            }

            if(m_verbosity != Verbosity.nothing) strbMessage.append("\n");


            if(m_verbosity != Verbosity.nothing) Logger.getLogger(PrintlnContentHandler.class.getName()).info(strbMessage.toString());

        }

        if(m_wrappedDataSinkContentHandler != null) m_wrappedDataSinkContentHandler.processModifiedData(metadata, strFulltext);
    }




    @Override
    public void processNewData(Metadata metadata, String strFulltext)
    {

        if(!m_showOnlyErrors)
        {

            StringBuilder strbMessage = new StringBuilder();

            if(m_verbosity != Verbosity.nothing) strbMessage.append("## PrintlnContentHandler - NEW data ##########################\n");

            if(m_verbosity == Verbosity.all || m_verbosity == Verbosity.title || m_verbosity == Verbosity.titlePlusMetadata
                    || m_verbosity == Verbosity.titlePlusFulltext)
            {
                String strInfo = metadata.get(IncrementalCrawlingHistory.dataEntityExistsID);
                if(strInfo == null) strInfo = metadata.get(DublinCore.SOURCE);
                if(strInfo == null) strInfo = metadata.get(Metadata.RESOURCE_NAME_KEY);

                strbMessage.append(strInfo).append("\n");
            }


            if(m_verbosity == Verbosity.all || m_verbosity == Verbosity.metadata || m_verbosity == Verbosity.titlePlusMetadata)
            {
                strbMessage.append("## metadata:\n");
                for (String strFieldName : metadata.names())
                {
                    for (String strValue : metadata.getValues(strFieldName))
                        strbMessage.append(strFieldName + ": '" + strValue + "'\n");
                }
            }


            if(m_verbosity == Verbosity.all || m_verbosity == Verbosity.fulltext || m_verbosity == Verbosity.titlePlusFulltext)
            {
                strFulltext = strFulltext.replaceAll("[\\n\\s]+", " ");
                strFulltext = strFulltext.substring(0, Math.min(strFulltext.length(), 2345));
                strbMessage.append("## fulltext (without newlines, reduced whitespace, fixed length): \n" + strFulltext).append("\n");
            }

            if(m_verbosity != Verbosity.nothing) strbMessage.append("\n");


            if(m_verbosity != Verbosity.nothing) Logger.getLogger(PrintlnContentHandler.class.getName()).info(strbMessage.toString());

        }

        if(m_wrappedDataSinkContentHandler != null) m_wrappedDataSinkContentHandler.processNewData(metadata, strFulltext);
    }



    @Override
    public void processRemovedData(Metadata metadata)
    {
        if(!m_showOnlyErrors)
        {

            StringBuilder strbMessage = new StringBuilder();

            if(m_verbosity != Verbosity.nothing) strbMessage.append("## PrintlnContentHandler REMOVED data ##########################\n");

            if(m_verbosity == Verbosity.all || m_verbosity == Verbosity.title || m_verbosity == Verbosity.titlePlusMetadata
                    || m_verbosity == Verbosity.titlePlusFulltext)
            {
                String strInfo = metadata.get(IncrementalCrawlingHistory.dataEntityExistsID);
                if(strInfo == null) strInfo = metadata.get(DublinCore.SOURCE);
                if(strInfo == null) strInfo = metadata.get(Metadata.RESOURCE_NAME_KEY);

                strbMessage.append(strInfo).append("\n");
            }


            if(m_verbosity == Verbosity.all || m_verbosity == Verbosity.metadata || m_verbosity == Verbosity.titlePlusMetadata)
            {
                strbMessage.append("## metadata:\n");
                for (String strFieldName : metadata.names())
                {
                    for (String strValue : metadata.getValues(strFieldName))
                        strbMessage.append(strFieldName + ": '" + strValue + "'\n");
                }
            }



            if(m_verbosity != Verbosity.nothing) strbMessage.append("\n");


            if(m_verbosity != Verbosity.nothing) Logger.getLogger(PrintlnContentHandler.class.getName()).info(strbMessage.toString());

        }

        if(m_wrappedDataSinkContentHandler != null) m_wrappedDataSinkContentHandler.processRemovedData(metadata);
    }



    public PrintlnContentHandler setGranularity(Verbosity granularity)
    {
        m_verbosity = granularity;
        
        return this;
    }



    public PrintlnContentHandler setShowOnlyErrors(boolean showOnlyErrors)
    {
        m_showOnlyErrors = showOnlyErrors;
        
        return this;
    }







}
