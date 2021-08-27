package de.dfki.km.leech.lucene.basic;



// import de.dfki.inquisitor.lucene.DynamicFieldType;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.*;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;



public class DocumentFrqClass implements Closeable
{


    protected Map<String, Long> m_hsTerm2IndexFrq;

    protected long m_lMaxFrq = 0;

    protected DB m_mapDB;

    protected IndexReader m_reader;

    protected String m_strFieldName4Calculation;

    protected String m_strMaxFrqTerm = "";



    @SuppressWarnings("unchecked")
    public DocumentFrqClass(IndexReader reader, String strFieldName4Calculation)
    {
        m_reader = reader;
        m_strFieldName4Calculation = strFieldName4Calculation;

        try
        {
            LoggerFactory.getLogger(DocumentFrqClass.class.getName()).info("load overall term index frequencies");


            // OLD: m_mapDB = DBMaker.newTempFileDB().deleteFilesAfterClose().closeOnJvmShutdown().transactionDisable().make();
            // m_hsTerm2IndexFrq = m_mapDB.getTreeMap("temp");
            m_mapDB = DBMaker.tempFileDB().closeOnJvmShutdown().fileDeleteAfterOpen().fileDeleteAfterClose().fileLockDisable().fileMmapEnableIfSupported().make();
            m_hsTerm2IndexFrq = (Map<String, Long>) m_mapDB.treeMap("temp").create();



            Terms terms;

            terms = MultiFields.getTerms(reader, strFieldName4Calculation);


            if(terms != null)
            {
                TermsEnum termsEnum = terms.iterator();

                while (termsEnum.next() != null)
                {
                    long lFrequency = termsEnum.totalTermFreq();
                    String strTerm = termsEnum.term().utf8ToString();

                    m_hsTerm2IndexFrq.put(strTerm, lFrequency);
                    if(lFrequency > m_lMaxFrq)
                    {
                        m_lMaxFrq = lFrequency;
                        m_strMaxFrqTerm = strTerm;
                    }
                }
            }


            LoggerFactory.getLogger(DocumentFrqClass.class.getName()).info("...finished");

        }
        catch (Throwable e)
        {
            LoggerFactory.getLogger(DocumentFrqClass.class.getName()).error("Error", e);
        }

    }



    public boolean addDocumentFrequencyClass(int iDocNo, Document doc2modify, String strNewField4FrqClass) throws Exception
    {

        boolean bModified = false;
        if(doc2modify.getField(strNewField4FrqClass) != null) bModified = true;

        doc2modify.removeFields(strNewField4FrqClass);

        if(doc2modify.getField(m_strFieldName4Calculation) == null) return bModified;


        double dAverageFrqClass = 0;
        int iFrqClassesCount = 0;



        Terms termVector = m_reader.getTermVector(iDocNo, m_strFieldName4Calculation);
        if(termVector == null) return bModified;

        TermsEnum termsEnum = termVector.iterator();

        while (termsEnum.next() != null)
        {
            String strTerm = termsEnum.term().utf8ToString();
            // reine Zahlen sind draussen
            if(strTerm.matches("\\d*")) continue;
            // das zählt nur für dieses doc, siehe ApiDoc reader.getTermVector(..)
            long lFrequencyInDoc = termsEnum.totalTermFreq();


            Long lFrequencyInIndex = m_hsTerm2IndexFrq.get(strTerm);
            if(lFrequencyInIndex == null) continue;

            int iFrqClass;
            if(m_lMaxFrq <= 0 || lFrequencyInIndex <= 0)
                iFrqClass = -1;
            else
                iFrqClass = (int) Math.floor((Math.log((m_lMaxFrq / lFrequencyInIndex)) / Math.log(2)));

            if(iFrqClass >= 2)
            {
                dAverageFrqClass += iFrqClass * lFrequencyInDoc;
                iFrqClassesCount += lFrequencyInDoc;
            }
        }



        if(iFrqClassesCount >= 0) dAverageFrqClass = dAverageFrqClass / iFrqClassesCount;

        // wir diskretisieren auf halbe Werte
        dAverageFrqClass = Math.round(dAverageFrqClass * 2);
        // als Integer, ohne Nachkommastellen (der eigentliche Wert mal 10)
        int iAverageFrqClass = (int) (dAverageFrqClass * 5d);



        // und an das doc dran
        FieldType fieldType =
                new DynamicFieldType().setIndexOptionS(IndexOptions.DOCS).setStoreD(true).setStoreTermVectorS(true)
                        .setStoreTermVectorOffsetS(true).setTokenizeD(true).freezE();

        Field field4buzzwords = new Field(strNewField4FrqClass, String.valueOf(iAverageFrqClass), fieldType);


        doc2modify.add(field4buzzwords);


        return true;
    }



    @Override
    public void close() throws IOException
    {
        if(m_mapDB != null) m_mapDB.close();
        m_mapDB = null;
        m_hsTerm2IndexFrq = null;
        m_reader = null;
    }

}
