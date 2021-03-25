package de.dfki.km.leech.lucene.basic;


import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;



public class TextWithTermVectorOffsetsField extends Field
{



    /**
     * Creates a new {@link TextWithTermVectorOffsetsField}. Default is to generate a stored field.
     * 
     * @param name field name
     * @param value String value
     * @throws IllegalArgumentException if the field name or value is null.
     */
    public TextWithTermVectorOffsetsField(String name, String value)
    {

        super(name, value, new DynamicFieldType(TextField.TYPE_STORED).setStoreTermVectorS(true).setStoreTermVectorOffsetS(true).freezE());

    }



    /**
     * Creates a new {@link TextWithTermVectorOffsetsField}
     * 
     * @param name field name
     * @param value String value
     * @param stored Store.YES if the content should also be stored
     * @throws IllegalArgumentException if the field name or value is null.
     */
    public TextWithTermVectorOffsetsField(String name, String value, Store stored)
    {


        super(name, value, new DynamicFieldType(stored == Store.YES ? TextField.TYPE_STORED : TextField.TYPE_NOT_STORED).setStoreTermVectorS(true)
                .setStoreTermVectorOffsetS(true).freezE());

    }


}
