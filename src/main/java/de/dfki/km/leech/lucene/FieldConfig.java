package de.dfki.km.leech.lucene;



import java.util.HashMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.Field.TermVector;
import org.apache.lucene.index.IndexableField;

import de.dfki.km.leech.util.StringUtils;



public class FieldConfig
{

    public static class FieldMapping
    {

        /**
         * The class name of the analyzer as String, e.g. "org.apache.lucene.analysis.core.KeywordAnalyzer" (which is also the default)
         */
        public String analyzer = "org.apache.lucene.analysis.core.KeywordAnalyzer";



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
        
        
        /**
         * Creates a new {@link FieldMapping} out of config parameter String representations.
         * 
         * @param strAnalyzer the analyzer class name as String, "de.dfki.km.leech.lucene.LeechSimpleAnalyzer"
         * @param strStore YES, NO, independent from the case and leading or trailing whitespace.
         * @param strIndex ANALYZED, ANALYZED_NO_NORMS, NO, NOT_ANALYZED, NOT_ANALYZED_NO_NORMS, independent from the case and leading or trailing whitespace.
         * @param strTermVector YES, NO, WITH_OFFSETS, WITH_POSITIONS, WITH_POSITIONS_OFFSETS, independent from the case and leading or trailing whitespace.
         * @param strFieldType DATE, DOUBLE, FLOAT, INTEGER, LONG, STRING, TIME, independent from the case and leading or trailing whitespace.
         * 
         * @throws Exception
         */
        public FieldMapping(String strAnalyzer, String strStore, String strIndex, String strTermVector, String strFieldType) throws Exception
        {
            super();
            this.analyzer = strAnalyzer;
            this.store = storeFromString(strStore);
            this.index = indexFromString(strIndex);
            this.termVector = termVectorFromString(strTermVector);
            this.fieldType = fieldTypeFromString(strFieldType);
        }
    }



    /**
     * Creates a field type object out of a String representation. possible values are DATE, DOUBLE, FLOAT, INTEGER, LONG, STRING, TIME, independent
     * from the case and leading or trailing whitespace.
     * 
     * @param strFieldType the field type string representation
     * 
     * @return the according field type
     * 
     * @throws Exception
     */
    static public FieldType fieldTypeFromString(String strFieldType) throws Exception
    {
        if(StringUtils.nullOrWhitespace(strFieldType)) throw new Exception("unable to parse according field type config from " + strFieldType);

        strFieldType = strFieldType.trim().toUpperCase();

        if("DATE".equals(strFieldType))
            return FieldType.DATE;
        else if("DATE".equals(strFieldType))
            return FieldType.DATE;
        else if("DOUBLE".equals(strFieldType))
            return FieldType.DOUBLE;
        else if("FLOAT".equals(strFieldType))
            return FieldType.FLOAT;
        else if("INTEGER".equals(strFieldType))
            return FieldType.INTEGER;
        else if("LONG".equals(strFieldType))
            return FieldType.LONG;
        else if("STRING".equals(strFieldType))
            return FieldType.STRING;
        else if("TIME".equals(strFieldType)) return FieldType.TIME;


        throw new Exception("unable to parse according field type config from " + strFieldType);
    }



    /**
     * Creates an index object out of a String representation. Possible values are ANALYZED, ANALYZED_NO_NORMS, NO, NOT_ANALYZED,
     * NOT_ANALYZED_NO_NORMS, independent from the case and leading or trailing whitespace.
     * 
     * @param strIndex the index string representation
     * 
     * @return the according index object
     * 
     * @throws Exception
     */
    static public Index indexFromString(String strIndex) throws Exception
    {
        if(StringUtils.nullOrWhitespace(strIndex)) throw new Exception("unable to parse according index config from " + strIndex);

        strIndex = strIndex.trim().toUpperCase();

        if(strIndex.equals("ANALYZED"))
            return Index.ANALYZED;
        else if(strIndex.equals("ANALYZED_NO_NORMS"))
            return Index.ANALYZED_NO_NORMS;
        else if(strIndex.equals("NO"))
            return Index.NO;
        else if(strIndex.equals("NOT_ANALYZED"))
            return Index.NOT_ANALYZED;
        else if(strIndex.equals("NOT_ANALYZED_NO_NORMS")) return Index.NOT_ANALYZED_NO_NORMS;


        throw new Exception("unable to parse according index config from " + strIndex);
    }



    /**
     * Creates a TermVector object out of a String representation. Possible values are YES, NO, WITH_OFFSETS, WITH_POSITIONS, WITH_POSITIONS_OFFSETS,
     * independent from the case and leading or trailing whitespace.
     * 
     * @param strTermVector the term vector string representation
     * 
     * @return the according term vector object
     * 
     * @throws Exception
     */
    static public TermVector termVectorFromString(String strTermVector) throws Exception
    {
        if(StringUtils.nullOrWhitespace(strTermVector)) throw new Exception("unable to parse according term vector config from " + strTermVector);

        strTermVector = strTermVector.trim().toUpperCase();

        if(strTermVector.equals("NO"))
            return TermVector.NO;
        else if(strTermVector.equals("WITH_OFFSETS"))
            return TermVector.WITH_OFFSETS;
        else if(strTermVector.equals("WITH_POSITIONS"))
            return TermVector.WITH_POSITIONS;
        else if(strTermVector.equals("WITH_POSITIONS_OFFSETS"))
            return TermVector.WITH_POSITIONS_OFFSETS;
        else if(strTermVector.equals("YES")) return TermVector.YES;


        throw new Exception("unable to parse according term vector config from " + strTermVector);
    }




    /**
     * Creates a Store object out of a String representation. Possible values are YES, NO, independent from the case and leading or trailing
     * whitespace.
     * 
     * @param strStore the strore string representation
     * 
     * @return the according store object
     * 
     * @throws Exception
     */
    static public Store storeFromString(String strStore) throws Exception
    {
        if(StringUtils.nullOrWhitespace(strStore)) throw new Exception("unable to parse according store config from " + strStore);

        strStore = strStore.trim().toUpperCase();

        if(strStore.equals("YES"))
            return Store.YES;
        else if(strStore.equals("NO")) return Store.NO;


        throw new Exception("unable to parse according store config from " + strStore);
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
    public IndexableField createField(String strAttName, String strAttValue) throws Exception
    {
        return FieldFactory.createField(strAttName, strAttValue, this);
    }


}
