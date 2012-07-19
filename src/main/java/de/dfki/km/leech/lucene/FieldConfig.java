package de.dfki.km.leech.lucene;



import java.util.HashMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.AbstractField;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.Field.TermVector;



public class FieldConfig
{

    public static class FieldMapping
    {

        /**
         * The class name of the analyzer as String, e.g. "org.apache.lucene.analysis.KeywordAnalyzer" (which is also the default)
         */
        public String analyzer = "org.apache.lucene.analysis.KeywordAnalyzer";



        /**
         * One out of {@link FieldType#STRING}, {@link FieldType#DATE}, {@link FieldType#TIME}, {@link FieldType#INTEGER}, {@link FieldType#LONG},
         * {@link FieldType#FLOAT}, {@link FieldType#DOUBLE}
         */
        public FieldType fieldType = FieldType.STRING;


        public Index index = Index.ANALYZED;

        public Store store = Store.YES;

        public TermVector termVector = TermVector.WITH_OFFSETS;



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
    }




    static public enum FieldType {
        DATE, DOUBLE, FLOAT, INTEGER, LONG, STRING, TIME;
    }


    public FieldMapping defaultFieldMapping = new FieldMapping();



    public HashMap<String, FieldMapping> hsFieldName2FieldMapping = new HashMap<String, FieldConfig.FieldMapping>();



    /**
     * Creates a new Analyzer out of this {@link FieldConfig}
     * 
     * @return the according analyzer
     * 
     * @throws Exception
     */
    public Analyzer createAnalyzer() throws Exception
    {
        return LuceneAnalyzerFactory.createAnalyzer(this);
    }




    /**
     * Create Field instances, according to the attribute configurations inside this {@link FieldConfig}
     * 
     * @param strAttName the attributes name
     * @param strAttValue the attributes value
     * 
     * @return the field, with Store, Index and TermVector configuration as given in fieldConfig
     * 
     * @throws Exception
     */
    public AbstractField createField(String strAttName, String strAttValue) throws Exception
    {
        return FieldFactory.createField(strAttName, strAttValue, this);
    }


}
