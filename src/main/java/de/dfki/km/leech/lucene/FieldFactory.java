package de.dfki.km.leech.lucene;



import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

import org.apache.lucene.document.AbstractField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.Field.TermVector;
import org.apache.lucene.document.NumericField;

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


    /**
     * Create Field instances, according to the attribute configurations inside the fieldConfig parameter
     * 
     * @param strAttName the attributes name
     * @param strAttValue the attributes value
     * @param fieldConfig the field configuration. Here you can specify whether a specific field should be analyzed, etc. You can also set default
     *            values.
     * 
     * @return the field, with Store, Index and TermVector configuration as given in fieldConfig
     * 
     * @throws Exception
     */
    static public AbstractField createField(String strAttName, String strAttValue, FieldConfig fieldConfig) throws Exception
    {

        // Der Store

        Store store = fieldConfig.defaultFieldMapping.store;

        FieldMapping fieldMapping4Att = fieldConfig.hsFieldName2FieldMapping.get(strAttName);
        

        Index index = fieldMapping4Att != null ? fieldMapping4Att.index : null;

        if(index == null) index = fieldConfig.defaultFieldMapping.index;


        TermVector termVector = fieldMapping4Att != null ? fieldMapping4Att.termVector : null;
        if(termVector == null) termVector = fieldConfig.defaultFieldMapping.termVector;


        LinkedList<FieldType> llNumberTypes = new LinkedList<FieldType>();
        llNumberTypes.add(FieldType.INTEGER);
        llNumberTypes.add(FieldType.LONG);
        llNumberTypes.add(FieldType.FLOAT);
        llNumberTypes.add(FieldType.DOUBLE);

        // welches Field erzeugt wird, steht ebenfalls in der config
        FieldType fieldType = fieldMapping4Att != null ? fieldMapping4Att.fieldType : null;
        if(fieldType == null) fieldType = fieldConfig.defaultFieldMapping.fieldType;


        AbstractField newField = null;
        if(strAttValue == null) strAttValue = "";



        if(FieldType.STRING.equals(fieldType))
        {
            newField = new Field(strAttName, strAttValue, store, index, termVector);
        }
        else if(StringUtils.nullOrWhitespace(strAttValue))
        {
            // wir können keine leeren numericValues eintragen - wir probieren, ob man string und number mit einem attributnamen mischen kann^^
            // scheint zu gehen
            // wenn das value leer ist und wir ein numerisches Field haben, dann indexieren wir dieses value nicht - ansonsten gibt es
            // Probleme beim Sortieren nach diesem numerischen Field
            newField = new Field(strAttName, strAttValue, store, Index.NO, TermVector.NO);
        }
        else if(llNumberTypes.contains(fieldType))
        {
            boolean bIndex = false;
            if(index == Index.ANALYZED || index == Index.ANALYZED_NO_NORMS) bIndex = true;
            newField = new NumericField(strAttName, store, bIndex);

            if(FieldType.INTEGER.equals(fieldType))
                ((NumericField) newField).setIntValue(Integer.parseInt(strAttValue));
            else if(FieldType.LONG.equals(fieldType))
                ((NumericField) newField).setLongValue(Long.parseLong(strAttValue));
            else if(FieldType.FLOAT.equals(fieldType))
                ((NumericField) newField).setFloatValue(Float.parseFloat(strAttValue));
            else if(FieldType.DOUBLE.equals(fieldType)) ((NumericField) newField).setDoubleValue(Double.parseDouble(strAttValue));
        }
        else if(FieldType.DATE.equals(fieldType))
        {
            boolean bIndex = false;
            if(index == Index.ANALYZED || index == Index.ANALYZED_NO_NORMS) bIndex = true;
            newField = new NumericField(strAttName, store, bIndex);

            Date parsedDate = DateParser.parseDateString(strAttValue);
            if(parsedDate == null) throw new Exception("Date String '" + strAttValue + "'" + " couldn't be parsed");
            ((NumericField) newField).setLongValue(DateUtils.date2Number(parsedDate));
        }
        else if(FieldType.TIME.equals(fieldType))
        {
            boolean bIndex = false;
            if(index == Index.ANALYZED || index == Index.ANALYZED_NO_NORMS) bIndex = true;
            newField = new NumericField(strAttName, store, bIndex);

            Date parsedDate = DateParser.parseDateString(strAttValue);
            ((NumericField) newField).setIntValue(Integer.parseInt(new SimpleDateFormat("HHmmssSSS").format(parsedDate)));
        }


        return newField;
    }
}
