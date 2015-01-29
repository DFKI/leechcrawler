/*
 * Leech - crawling capabilities for Apache Tika
 * 
 * Copyright (C) 2012 DFKI GmbH, Author: Christian Reuschling
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Contact us by mail: christian.reuschling@dfki.de
 */

package de.dfki.km.leech.sax;



import org.apache.tika.metadata.Metadata;
import org.xml.sax.SAXException;



public class DataSinkContentHandlerAdapter extends DataSinkContentHandler
{



    /**
     * Creates a new {@link DataSinkContentHandlerAdapter}.<br>
     * CAUTION:Note that the internal metadata object has to be the same than the one given to the parser that works with this contentHandler. Use
     * {@link #setMetaData(Metadata)} or one of the Leech methods with the DataSinkContentHandlers. In the second case Leech will make sure that the metadata objects will
     * be set correctly.
     */
    public DataSinkContentHandlerAdapter()
    {
        super();
    }



    /**
     * Creates a content handler that writes XHTML body character events to an internal string buffer, and forwards it together with the metadata object to a
     * callback/processing method.<br>
     * CAUTION:Note that the internal metadata object has to be the same than the one given to the parser that works with this contentHandler. Use
     * {@link #setMetaData(Metadata)} or one of the Leech methods with the DataSinkContentHandlers. In the second case Leech will make sure that the metadata objects will
     * be set correctly.
     * <p>
     * <p>
     * The internal string buffer is bounded at the given number of characters. If this write limit is reached, then a {@link SAXException} is thrown.
     * 
     * @param writeLimit maximum number of characters to include in the string, or -1 to disable the write limit
     */
    public DataSinkContentHandlerAdapter(int writeLimit)
    {
        super(writeLimit);
    }



    public DataSinkContentHandlerAdapter(Metadata metadata)
    {
        super(metadata);
    }



    public DataSinkContentHandlerAdapter(Metadata metadata, int writeLimit)
    {
        super(metadata, writeLimit);
    }



    @Override
    public void crawlFinished()
    {
        // NOP - implement on your own as you wish
    }



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
    public void processProcessedData(Metadata metadata)
    {
        // NOP - implement on your own as you wish
        
    }



    @Override
    public void processRemovedData(Metadata metadata)
    {
        // NOP - implement on your own as you wish
    }



    @Override
    public void processUnmodifiedData(Metadata metadata)
    {
        // NOP - implement on your own as you wish
        
    }

}
