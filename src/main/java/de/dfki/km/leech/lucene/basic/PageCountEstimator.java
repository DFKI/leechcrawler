package de.dfki.km.leech.lucene.basic;



import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.LegacyIntField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Terms;

import java.io.IOException;



public class PageCountEstimator
{


    /**
     * Adds a page count attribute to a document in the case no one is there. The method estimates the page cont (i.e. 400 terms => 1 page).
     * 
     * @param iDocNo the docs index number
     * @param doc2modify the document
     * @param strPageCountAttName the field for the generated page count (that should be created)
     * @param isHeuristicPageCountFlagAttName an attribute name that will be generated as hint wether a document page count is calculated or not
     * @param strBodyAttName the body attribute name to perform the calculation
     * @param reader the lucene index reader
     * 
     * @return true in the case the doc was modified, false otherwise
     * 
     * @throws Exception
     */
    static public boolean addHeuristicDocPageCounts(int iDocNo, Document doc2modify, String strPageCountAttName, String isHeuristicPageCountFlagAttName,
            String strBodyAttName, IndexReader reader) throws Exception
    {
        // sofern ein Attribut noch nicht vorhanden ist, wird es hier erzeugt - mit Hilfe einer Heuristik
        // es wird auch noch ein zusätzliches Attribut eingetragen, welches anzeigt, daß die PageCount mit Hilfe
        // einer Heuristik erzeugt wurde

        // wenn es schon einen Eintrag für die Seitenzahlen gibt, wird das Dokument ignoriert (das war zumindest so, solange schöne Zahln im Index
        // standen)
        String strPageCountValue = doc2modify.get(strPageCountAttName);
        // if(strPageCountValue != null)
        if(strPageCountValue != null && doc2modify.get(isHeuristicPageCountFlagAttName) == null)
        {

            // wenn da so ein verkrutztes Leech-Ding drin steht, dann machen wir da ne schöne Zahl draus :)
            int iIndexOfKrutzel = strPageCountValue.indexOf("^^");
            if(iIndexOfKrutzel == -1) return false;

            String strPageCountValueNice = strPageCountValue.substring(0, iIndexOfKrutzel);
            doc2modify.removeFields(strPageCountAttName);

            LegacyIntField field = new LegacyIntField(strPageCountAttName, Integer.parseInt(strPageCountValueNice), Store.YES);

            if(field != null) doc2modify.add(field);

            return true;
        }

        // wenn es keinen Eintrag für den Content gibt, wird das Dokument ebenfalls ignoriert
        String strBodyValue = doc2modify.get(strBodyAttName);
        if(strBodyValue == null) return false;

        // wir haben einen Eintrag für den Body und keinen für die Seitenzahlen - also frisch ans Werk ;)

        int iPageCount = 0;

        // die Heuristik: 400 Terme ergeben eine Seite

        int iDocTermCount = getDocumentTermCount(iDocNo, strBodyAttName, reader);

        // ich sag jetzt mal einfach, daß ungefähr 400 Wörter auf einer Seite sind...
        iPageCount = (iDocTermCount / 400) + 1;

        // die geschätzte PageCount
        doc2modify.removeFields(strPageCountAttName);
        LegacyIntField field = new LegacyIntField(strPageCountAttName, iPageCount, Store.YES);
        if(field != null) doc2modify.add(field);
        // ein Flag, welches anzeigt, daß dieser TermCount geschätzt wurde
        doc2modify.removeFields(isHeuristicPageCountFlagAttName);
        StringField newField = new StringField(isHeuristicPageCountFlagAttName, "true", Store.YES);
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
