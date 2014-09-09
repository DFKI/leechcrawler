package de.dfki.km.leech.lucene;



import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Terms;
import org.apache.tika.metadata.Metadata;

import de.dfki.km.leech.metadata.LeechMetadata;



public class PageCountEstimator
{


    /**
     * Adds a page count attribute to a document in the case no one is there. The method estimates the page cont (i.e. 400 terms => 1 page).
     * 
     * @param iDocNo
     * @param doc2modify
     * @param reader
     * 
     * @return true in the case the doc was modified, false otherwise
     * 
     * @throws Exception
     */
    static public boolean addHeuristicDocPageCounts(int iDocNo, Document doc2modify, IndexReader reader) throws Exception
    {
        // sofern ein Attribut noch nicht vorhanden ist, wird es hier erzeugt - mit Hilfe einer Heuristik
        // es wird auch noch ein zusätzliches Attribut eingetragen, welches anzeigt, daß die PageCount mit Hilfe
        // einer Heuristik erzeugt wurde

        // wenn es schon einen Eintrag für die Seitenzahlen gibt, wird das Dokument ignoriert (das war zumindest so, solange schöne Zahln im Index
        // standen)
        String strPageCountValue = doc2modify.get(Metadata.PAGE_COUNT.getName());
        // if(strPageCountValue != null)
        if(strPageCountValue != null && doc2modify.get(LeechMetadata.isHeuristicPageCount) == null)
        {

            // wenn da so ein verkrutztes Leech-Ding drin steht, dann machen wir da ne schöne Zahl draus :)
            int iIndexOfKrutzel = strPageCountValue.indexOf("^^");
            if(iIndexOfKrutzel == -1) return false;

            String strPageCountValueNice = strPageCountValue.substring(0, iIndexOfKrutzel);
            doc2modify.removeFields(Metadata.PAGE_COUNT.getName());

            IntField field = new IntField(Metadata.PAGE_COUNT.getName(), Integer.parseInt(strPageCountValueNice), Store.YES);

            if(field != null) doc2modify.add(field);

            return true;
        }

        // wenn es keinen Eintrag für den Content gibt, wird das Dokument ebenfalls ignoriert
        String strBodyValue = doc2modify.get(LeechMetadata.body);
        if(strBodyValue == null) return false;

        // wir haben einen Eintrag für den Body und keinen für die Seitenzahlen - also frisch ans Werk ;)

        int iPageCount = 0;

        // die Heuristik: 400 Terme ergeben eine Seite

        int iDocTermCount = getDocumentTermCount(iDocNo, LeechMetadata.body, reader);

        // ich sag jetzt mal einfach, daß ungefähr 400 Wörter auf einer Seite sind...
        iPageCount = (iDocTermCount / 400) + 1;

        // die geschätzte PageCount
        doc2modify.removeFields(Metadata.PAGE_COUNT.getName());
        IntField field = new IntField(Metadata.PAGE_COUNT.getName(), iPageCount, Store.YES);
        if(field != null) doc2modify.add(field);
        // ein Flag, welches anzeigt, daß dieser TermCount geschätzt wurde
        doc2modify.removeFields(LeechMetadata.isHeuristicPageCount);
        StringField newField = new StringField(LeechMetadata.isHeuristicPageCount, "true", Store.YES);
        if(newField != null) doc2modify.add(newField);


        return true;
    }



    public static Integer getDocumentTermCount(int iDocNo, String strFieldName4TermCounting, IndexReader reader) throws IOException
    {

        long lTermCount = 0;


        Terms termVector = reader.getTermVector(iDocNo, strFieldName4TermCounting);

        // manchmal gibt es auch Dokumente, die keinen content bzw. keinen TermFreqVector haben....
        if(termVector != null) lTermCount = termVector.getSumTotalTermFreq();


        return Long.valueOf(lTermCount).intValue();
    }

}
