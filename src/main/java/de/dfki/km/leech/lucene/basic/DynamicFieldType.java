package de.dfki.km.leech.lucene.basic;



import com.cedarsoftware.util.io.JsonReader;
import com.cedarsoftware.util.io.JsonWriter;
import de.dfki.inquisitor.text.DateParser;
import de.dfki.inquisitor.text.DateUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.IndexOptions;

import java.io.IOException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

// import de.dfki.inquisitor.lucene.LuceneAnalyzerFactory;



@SuppressWarnings("javadoc")
public class DynamicFieldType extends FieldType
{


    public static final DynamicFieldType doubleFieldType = new DynamicFieldType(LegacyDoubleField.TYPE_STORED).freezE();

    public static final DynamicFieldType floatFieldType = new DynamicFieldType(LegacyFloatField.TYPE_STORED).freezE();

    public static final DynamicFieldType integerFieldType = new DynamicFieldType(LegacyIntField.TYPE_STORED).freezE();

    public static final DynamicFieldType dateFieldType = new DynamicFieldType(LegacyLongField.TYPE_STORED).setDateParsing(true).freezE();

    public static final DynamicFieldType keywordFieldType =
            new DynamicFieldType().setTokenizeD(true).setStoreD(true).setStoreTermVectorS(true).setStoreTermVectorOffsetS(true)
                    .setIndexOptionS(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS).setOmitNormS(true).setAnalyzer("org.apache.lucene.analysis.core.KeywordAnalyzer")
                    .freezE();

    public static final DynamicFieldType longFieldType = new DynamicFieldType(LegacyLongField.TYPE_STORED).freezE();

    public static final DynamicFieldType tokenizedFieldType =
            new DynamicFieldType().setTokenizeD(true).setStoreD(true).setStoreTermVectorS(true).setStoreTermVectorOffsetS(true)
                    .setIndexOptionS(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS).setAnalyzer("de.dfki.km.leech.lucene.LeechSimpleAnalyzer").freezE();



    /**
     * Create Field instances, according to the configuration inside the given fieldType. Number fields will be generated, if a string value is given, it will be
     * converted in the case the fieldType is a number type. Further, the method parses Strings for date if the fieldtype is of type {@link DynamicFieldType} and
     * configured accordingly. You can also give number values for generating number or String fields fields (also according to the given fieldType).
     *
     * @param strAttName the attributes name
     * @param attValue   the attributes value
     * @param fieldType  the field type that influences the returned type of the field
     *
     * @return the field, with the configured fieldType. Null in the case the Field can not be generated out of the value.
     */
    static public Field createField(String strAttName, Object attValue, FieldType fieldType)
    {
        try
        {
            if (attValue == null)
                return null;


            if (fieldType instanceof DynamicFieldType && ((DynamicFieldType) fieldType).getDateParsing() && attValue instanceof String)
            {
                Date parsedDate = DateParser.parseDateString((String) attValue);
                if (parsedDate != null)
                    return new LegacyLongField(strAttName, DateUtils.date2Number(parsedDate), fieldType);
                else
                    return null;
            }
            else if (attValue instanceof String)
            {

                if (fieldType.numericType() == LegacyNumericType.INT)
                    return new LegacyIntField(strAttName, Integer.valueOf((String) attValue), fieldType);
                else if (fieldType.numericType() == LegacyNumericType.LONG)
                    return new LegacyLongField(strAttName, Long.valueOf((String) attValue), fieldType);
                else if (fieldType.numericType() == LegacyNumericType.FLOAT)
                    return new LegacyFloatField(strAttName, Float.valueOf((String) attValue), fieldType);
                else if (fieldType.numericType() == LegacyNumericType.DOUBLE)
                    return new LegacyDoubleField(strAttName, Double.valueOf((String) attValue), fieldType);
                else
                    return new Field(strAttName, (String) attValue, fieldType);
            }
            else if (attValue instanceof Number)
            {

                if (fieldType.numericType() == LegacyNumericType.INT)
                    return new LegacyIntField(strAttName, ((Number) attValue).intValue(), fieldType);
                else if (fieldType.numericType() == LegacyNumericType.LONG)
                    return new LegacyLongField(strAttName, ((Number) attValue).longValue(), fieldType);
                else if (fieldType.numericType() == LegacyNumericType.FLOAT)
                    return new LegacyFloatField(strAttName, ((Number) attValue).floatValue(), fieldType);
                else if (fieldType.numericType() == LegacyNumericType.DOUBLE)
                    return new LegacyDoubleField(strAttName, ((Number) attValue).doubleValue(), fieldType);
                else
                    return new Field(strAttName, String.valueOf(attValue), fieldType);
            }
            else
                return null;
        } catch (Exception e)
        {
            Logger.getLogger(FieldConfig.class.getName()).log(Level.SEVERE, "Error", e);
            return null;
        }
    }
    protected String analyzer;
    protected boolean dateParsing = false;

    public DynamicFieldType()
    {
        super();
    }



    public DynamicFieldType(FieldType ref)
    {
        super(ref);
    }



    public Analyzer createAnalyzer()
    {
        try
        {

            return LuceneAnalyzerFactory.createAnalyzer(getAnalyzer(), null);
        } catch (Exception e)
        {
            Logger.getLogger(DynamicFieldType.class.getName()).log(Level.SEVERE, "Error", e);
            return null;
        }
    }



    /**
     * Create Field instances, according to the configuration inside the given fieldType. Number fields will be generated, if a string value is given, it will be
     * converted in the case the fieldType is a number type. Further, the method parses Strings for date if the fieldtype is of type {@link DynamicFieldType} and
     * configured accordingly. You can also give number values for generating number or String fields fields (also according to the given fieldType).
     *
     * @param strAttName the attributes name
     * @param attValue   the attributes value
     *
     * @return the field, with the configured fieldType. Null in the case the Field can not be generated out of the value.
     */
    public Field createField(String strAttName, Object attValue)
    {
        return createField(strAttName, attValue, this);
    }



    /**
     * Same functionality as in upper class method, but returns this as sugar.
     **/
    public DynamicFieldType freezE()
    {
        super.freeze();

        return this;
    }



    /**
     * works only if this is not frozen yet
     */
    public void fromJson(String strJson)
    {
        try
        {
            DynamicFieldType ref = (DynamicFieldType) JsonReader.jsonToJava(strJson);

            // this.setIndexed(ref.indexed());
            this.setStored(ref.stored());
            this.setTokenized(ref.tokenized());
            this.setStoreTermVectors(ref.storeTermVectors());
            this.setStoreTermVectorOffsets(ref.storeTermVectorOffsets());
            this.setStoreTermVectorPositions(ref.storeTermVectorPositions());
            this.setStoreTermVectorPayloads(ref.storeTermVectorPayloads());
            this.setOmitNorms(ref.omitNorms());
            this.setIndexOptions(ref.indexOptions());
            this.setDocValuesType(ref.docValuesType());
            this.setNumericType(ref.numericType());
            this.setNumericPrecisionStep(ref.numericPrecisionStep());

            this.setAnalyzer(ref.getAnalyzer());
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }



    /**
     * Get the analyzer for this class. This is additionaly to the upper Lucene Fieldtype, for convinience. Returns this as sugar.
     */
    public String getAnalyzer()
    {
        return this.analyzer;
    }



    public boolean getDateParsing()
    {
        return dateParsing;
    }



    /**
     * Set the analyzer for this class. The given String is the full class name of the analyzer, that can be used with Class.forName(..). This is additionaly to the upper
     * Lucene Fieldtype, for convinience. Returns this as sugar.
     */
    public DynamicFieldType setAnalyzer(String analyzer)
    {
        this.analyzer = analyzer;

        return this;
    }



    /**
     * Specifies whether the values of this field should be parsed as date values or not. If true, all input strings will be parsed and written as according number into
     * the index
     *
     * @return this as sugar
     */
    public DynamicFieldType setDateParsing(boolean enableDateParsing)
    {
        this.dateParsing = enableDateParsing;

        return this;
    }



    /**
     * Same functionality as in upper class method, but returns this as sugar.
     **/
    public DynamicFieldType setDocValuesTypE(DocValuesType type)
    {
        super.setDocValuesType(type);

        return this;
    }



    /**
     * Same functionality as in upper class method, but returns this as sugar.
     **/
    public DynamicFieldType setIndexOptionS(IndexOptions value)
    {
        super.setIndexOptions(value);

        return this;
    }



    // /**
    //  * Same functionality as in upper class method, but returns this as sugar.
    //  **/
    // public DynamicFieldType setIndexeD(boolean value)
    // {
    //     super.setIndexed(value);
    //
    //     return this;
    // }



    /**
     * Same functionality as in upper class method, but returns this as sugar.
     **/
    public DynamicFieldType setNumericPrecisionSteP(int precisionStep)
    {
        super.setNumericPrecisionStep(precisionStep);

        return this;
    }




    /**
     * Same functionality as in upper class method, but returns this as sugar.
     **/
    public DynamicFieldType setNumericTypE(LegacyNumericType type)
    {
        super.setNumericType(type);

        return this;
    }




    /**
     * Same functionality as in upper class method, but returns this as sugar.
     **/
    public DynamicFieldType setOmitNormS(boolean value)
    {
        super.setOmitNorms(value);

        return this;
    }




    /**
     * Same functionality as in upper class method, but returns this as sugar.
     **/
    public DynamicFieldType setStoreD(boolean value)
    {
        super.setStored(value);

        return this;
    }




    /**
     * Same functionality as in upper class method, but returns this as sugar.
     **/
    public DynamicFieldType setStoreTermVectorOffsetS(boolean value)
    {
        super.setStoreTermVectorOffsets(value);

        return this;
    }




    /**
     * Same functionality as in upper class method, but returns this as sugar.
     **/
    public DynamicFieldType setStoreTermVectorPayloadS(boolean value)
    {
        super.setStoreTermVectorPayloads(value);

        return this;
    }




    /**
     * Same functionality as in upper class method, but returns this as sugar.
     **/
    public DynamicFieldType setStoreTermVectorPositionS(boolean value)
    {
        super.setStoreTermVectorPositions(value);

        return this;
    }




    /**
     * Same functionality as in upper class method, but returns this as sugar.
     **/
    public DynamicFieldType setStoreTermVectorS(boolean value)
    {
        super.setStoreTermVectors(value);

        return this;
    }




    /**
     * Same functionality as in upper class method, but returns this as sugar.
     **/
    public DynamicFieldType setTokenizeD(boolean value)
    {
        super.setTokenized(value);

        return this;
    }




    public String toJson(boolean bFormatIt)
    {
        try
        {
            String strJson = JsonWriter.objectToJson(this);

            if (bFormatIt)
                strJson = JsonWriter.formatJson(strJson);

            // TODO abchecken, ob das noch nötig ist: https://github.com/jdereg/json-io/issues/19
            return strJson.replaceAll(",\\s*\"ordinal\":\\d+", "");
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
