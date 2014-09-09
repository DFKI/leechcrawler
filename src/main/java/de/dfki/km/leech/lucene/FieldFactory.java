package de.dfki.km.leech.lucene;



import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.Field.TermVector;
import org.apache.lucene.document.FloatField;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.FieldInfo.IndexOptions;

import de.dfki.km.leech.lucene.FieldConfig.FieldMapping;
import de.dfki.km.leech.lucene.FieldConfig.FieldType;
import de.dfki.km.leech.util.StringUtils;



/**
 * Factory for creating Lucene Field instances
 * 
 * @author reuschling
 * 
 */
public class FieldFactory
{


    protected static LinkedList<FieldType> m_llNumberTypes = new LinkedList<FieldType>();



    static
    {

        m_llNumberTypes.add(FieldType.INTEGER);
        m_llNumberTypes.add(FieldType.LONG);
        m_llNumberTypes.add(FieldType.FLOAT);
        m_llNumberTypes.add(FieldType.DOUBLE);
    }




    @Deprecated
    public static final org.apache.lucene.document.FieldType translateFieldType(Store store, Index index, TermVector termVector)
    {
        final org.apache.lucene.document.FieldType ft = new org.apache.lucene.document.FieldType();

        ft.setStored(store == Store.YES);

        switch (index)
        {
            case ANALYZED:
                ft.setIndexed(true);
                ft.setTokenized(true);
                break;
            case ANALYZED_NO_NORMS:
                ft.setIndexed(true);
                ft.setTokenized(true);
                ft.setOmitNorms(true);
                break;
            case NOT_ANALYZED:
                ft.setIndexed(true);
                ft.setTokenized(false);
                break;
            case NOT_ANALYZED_NO_NORMS:
                ft.setIndexed(true);
                ft.setTokenized(false);
                ft.setOmitNorms(true);
                break;
            case NO:
                break;
        }

        switch (termVector)
        {
            case NO:
                ft.setIndexOptions(IndexOptions.DOCS_ONLY);
                break;
            case YES:
                ft.setStoreTermVectors(true);
                ft.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
                break;
            case WITH_POSITIONS:
                ft.setStoreTermVectors(true);
                ft.setStoreTermVectorPositions(true);
                ft.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
                break;
            case WITH_OFFSETS:
                ft.setStoreTermVectors(true);
                ft.setStoreTermVectorOffsets(true);
                ft.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
                break;
            case WITH_POSITIONS_OFFSETS:
                ft.setStoreTermVectors(true);
                ft.setStoreTermVectorPositions(true);
                ft.setStoreTermVectorOffsets(true);
                ft.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
                break;
        }
        ft.freeze();
        return ft;
    }






    /**
     * Create Field instances, according to the attribute configurations inside the fieldConfig parameter
     * 
     * @param strAttName the attributes name
     * @param strAttValue the attributes value
     * @param fieldConfig the field configuration. Here you can specify whether a specific field should be analyzed, etc. You can also set default values.
     * 
     * @return the field, with Store, Index and TermVector configuration as given in fieldConfig. Null in the case the field could'nt generated
     */
    static public IndexableField createField(String strAttName, String strAttValue, FieldConfig fieldConfig)
    {

        // Der Store

        Store store = fieldConfig.defaultFieldMapping.store;

        FieldMapping fieldMapping4Att = fieldConfig.hsFieldName2FieldMapping.get(strAttName);


        Index index = fieldMapping4Att != null ? fieldMapping4Att.index : null;

        if(index == null) index = fieldConfig.defaultFieldMapping.index;


        TermVector termVector = fieldMapping4Att != null ? fieldMapping4Att.termVector : null;
        if(termVector == null) termVector = fieldConfig.defaultFieldMapping.termVector;



        // welches Field erzeugt wird, steht ebenfalls in der config
        FieldType fieldType = fieldMapping4Att != null ? fieldMapping4Att.fieldType : null;
        if(fieldType == null) fieldType = fieldConfig.defaultFieldMapping.fieldType;


        Field newField = null;
        if(strAttValue == null) strAttValue = "";



        if(FieldType.STRING.equals(fieldType))
        {
            newField = new Field(strAttName, strAttValue, translateFieldType(store, index, termVector));
        }
        else if(StringUtils.nullOrWhitespace(strAttValue))
        {
            // wir können keine leeren numericValues eintragen - wir probieren, ob man string und number mit einem attributnamen mischen kann^^
            // scheint zu gehen
            // wenn das value leer ist und wir ein numerisches Field haben, dann indexieren wir dieses value nicht - ansonsten gibt es
            // Probleme beim Sortieren nach diesem numerischen Field
            newField = new Field(strAttName, strAttValue, translateFieldType(store, Index.NO, TermVector.NO));
        }
        else if(m_llNumberTypes.contains(fieldType))
        {

            if(FieldType.INTEGER.equals(fieldType))
            {
                newField = new IntField(strAttName, Integer.parseInt(strAttValue), store);
            }
            else if(FieldType.LONG.equals(fieldType))
            {
                newField = new LongField(strAttName, Long.parseLong(strAttValue), store);
            }
            else if(FieldType.FLOAT.equals(fieldType))
            {
                newField = new FloatField(strAttName, Float.parseFloat(strAttValue), store);
            }
            else if(FieldType.DOUBLE.equals(fieldType))
            {
                newField = new DoubleField(strAttName, Double.parseDouble(strAttValue), store);
            }
        }
        else if(FieldType.DATE.equals(fieldType))
        {
            Date parsedDate = DateParser.parseDateString(strAttValue);
            if(parsedDate == null)
            {
                return null;
            }
            newField = new LongField(strAttName, DateUtils.date2Number(parsedDate), store);
        }
        else if(FieldType.TIME.equals(fieldType))
        {
            Date parsedDate = DateParser.parseDateString(strAttValue);
            newField = new IntField(strAttName, Integer.parseInt(new SimpleDateFormat("HHmmssSSS").format(parsedDate)), store);
        }


        return newField;
    }
}
