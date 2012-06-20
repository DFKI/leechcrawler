package de.dfki.km.leech.lucene;



import java.util.HashMap;

import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.Field.TermVector;



public class FieldConfig
{

    static public enum FieldType {
        STRING, DATE, TIME, INTEGER, LONG, FLOAT, DOUBLE;
    }


    public FieldMapping defaultFieldMapping = new FieldMapping();


    public HashMap<String, FieldMapping> hsFieldName2FieldMapping = new HashMap<String, FieldConfig.FieldMapping>();




    public static class FieldMapping
    {

        public FieldMapping()
        {
        }



        public FieldMapping(String analyzer, Store store, Index index, TermVector termVector, FieldType fieldType)
        {
            super();
            this.analyzer = analyzer;
            this.store = store;
            this.index = index;
            this.termVector = termVector;
            this.fieldType = fieldType;
        }


        /**
         * The class name of the analyzer as String, e.g. "org.apache.lucene.analysis.KeywordAnalyzer" (which is also the default)
         */
        public String analyzer = "org.apache.lucene.analysis.KeywordAnalyzer";

        public Store store = Store.YES;

        public Index index = Index.ANALYZED;

        public TermVector termVector = TermVector.WITH_OFFSETS;

        /**
         * One out of {@link FieldType#STRING}, {@link FieldType#DATE}, {@link FieldType#TIME}, {@link FieldType#INTEGER}, {@link FieldType#LONG},
         * {@link FieldType#FLOAT}, {@link FieldType#DOUBLE}
         */
        public FieldType fieldType = FieldType.STRING;
    }


}
