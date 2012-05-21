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
