package de.dfki.km.leech.lucene.basic;



import com.cedarsoftware.util.io.JsonReader;
import com.cedarsoftware.util.io.JsonWriter;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Field;

import java.util.HashMap;



public class FieldConfig
{



    public DynamicFieldType defaultFieldType = new DynamicFieldType();



    public HashMap<String, DynamicFieldType> fieldName2FieldType = new HashMap<String, DynamicFieldType>();



    /**
     * Creates a new Analyzer out of this {@link FieldConfig}, which is a {@link PerFieldAnalyzerWrapper} for all configured fields
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
     * Create Field instances, according to the fieldType mappings inside this {@link FieldConfig}. Number fields will be generated, if a string value is given, it will
     * be converted in the case the fieldType is a number type. Further, the method parses Strings for date if the fieldtype is of type {@link DynamicFieldType} and
     * configured accordingly. You can also give number values for generating number or String fields fields (also according to the given fieldType).
     *
     * @param strAttName the attributes name
     * @param attValue   the attributes value
     *
     * @return the field, with the configured fieldType. Null in the case the Field can not be generated out of the value.
     */
    public Field createField(String strAttName, Object attValue)
    {
        DynamicFieldType fieldType = getFieldType(strAttName);

        return fieldType.createField(strAttName, attValue);
    }





    public void fromJson(String strJson)
    {

        FieldConfig fieldConfig = (FieldConfig) JsonReader.jsonToJava(strJson);

        this.defaultFieldType = fieldConfig.defaultFieldType;

        this.fieldName2FieldType = fieldConfig.fieldName2FieldType;
    }



    /**
     * Gets the field type for a specific field, as configured. In the case there is no explicit mapping for the field, the default type will be returned.
     *
     * @param strFieldName
     *
     * @return
     */
    public DynamicFieldType getFieldType(String strFieldName)
    {
        DynamicFieldType fieldType = fieldName2FieldType.get(strFieldName);

        if (fieldType == null)
            fieldType = defaultFieldType;

        return fieldType;
    }



    public String toJson(boolean bFormatIt)
    {
        HashMap<String, Object> hsOptions = new HashMap<>();
        hsOptions.put(JsonWriter.ENUM_PUBLIC_ONLY, true);

        String strJson = JsonWriter.objectToJson(this, hsOptions);


        if (bFormatIt)
            strJson = JsonWriter.formatJson(strJson);

        // return strJson.replaceAll(",\\s*\"ordinal\":\\d+", "");
        return strJson;
    }
}
