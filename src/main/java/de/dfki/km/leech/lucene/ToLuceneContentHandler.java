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

package de.dfki.km.leech.lucene;



import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexWriter;
import org.apache.tika.metadata.Metadata;

import de.dfki.km.leech.sax.DataSinkContentHandler;


/**
 * Hier höchstens etwas exemplarisches, wenn überhaupt - unser Lucene-ContentHandler kommt hübsch nach DynaQ
 * 
 * @author Christian Reuschling, Dipl.Ing.(BA)
 *
 */
public class ToLuceneContentHandler extends DataSinkContentHandler
{


    private final IndexWriter m_luceneWriter;





    public ToLuceneContentHandler(Metadata metadata, IndexWriter luceneWriter)
    {
        super(metadata);
        m_luceneWriter = luceneWriter;
    }







    @Override
    public void processNewData(Metadata metadata, String strFulltext)
    {

        Document doc = new Document();


        // TODO hier wollen wir irgendwie die DynaQ-FieldFactory einsetzen (diese Klasse wird dann Teil von DynaQ)


        // der body mit dem reader

        // na super - man kann kein Stored Field mit einem reader erzeugen...dann kann ich jetzt gerade wieder einen String draus machen.supi.
        // String strText = new Scanner( fulltextReader ).useDelimiter("\\A").next();


        doc.add(new Field("fulltext", strFulltext, Store.YES, Index.ANALYZED));

        // die restlichen metadaten

        for (String strFieldName : metadata.names())
        {
            for (String strValue : metadata.getValues(strFieldName))
            {
                doc.add(new Field(strFieldName, strValue, Store.YES, Index.ANALYZED));
            }
        }

    }





    @Override
    public void processModifiedData(Metadata metadata, String strFulltext)
    {

        Document doc = new Document();


        // TODO hier wollen wir irgendwie die DynaQ-FieldFactory einsetzen (diese Klasse wird dann Teil von DynaQ)


        // der body mit dem reader

        // na super - man kann kein Stored Field mit einem reader erzeugen...dann kann ich jetzt gerade wieder einen String draus machen.supi.
        // String strText = new Scanner( fulltextReader ).useDelimiter("\\A").next();


        doc.add(new Field("fulltext", strFulltext, Store.YES, Index.ANALYZED));

        // die restlichen metadaten

        for (String strFieldName : metadata.names())
        {
            for (String strValue : metadata.getValues(strFieldName))
            {
                doc.add(new Field(strFieldName, strValue, Store.YES, Index.ANALYZED));
            }
        }
    }





    @Override
    public void processRemovedData(Metadata metadata)
    {
        // TODO Auto-generated method stub

    }







    @Override
    public void processErrorData(Metadata metadata)
    {
        // TODO Auto-generated method stub
        
    }







}
