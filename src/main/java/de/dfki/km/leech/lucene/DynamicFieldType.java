package de.dfki.km.leech.lucene;

import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.FieldInfo.DocValuesType;
import org.apache.lucene.index.FieldInfo.IndexOptions;

@SuppressWarnings("javadoc")
public class DynamicFieldType extends FieldType
{


    public DynamicFieldType()
    {
        super();
    }




    public DynamicFieldType(FieldType ref)
    {
        super(ref);
    }



    /** Same functionality as in upper class method, but returns this as sugar. **/
    public DynamicFieldType freezE()
    {
        super.freeze();

        return this;
    }




    /** Same functionality as in upper class method, but returns this as sugar. **/
    public DynamicFieldType setIndexeD(boolean value)
    {
        super.setIndexed(value);

        return this;
    }




    /** Same functionality as in upper class method, but returns this as sugar. **/
    public DynamicFieldType setStoreD(boolean value)
    {
        super.setStored(value);

        return this;
    }




    /** Same functionality as in upper class method, but returns this as sugar. **/
    public DynamicFieldType setTokenizeD(boolean value)
    {
        super.setTokenized(value);

        return this;
    }




    /** Same functionality as in upper class method, but returns this as sugar. **/
    public DynamicFieldType setStoreTermVectorS(boolean value)
    {
        super.setStoreTermVectors(value);

        return this;
    }




    /** Same functionality as in upper class method, but returns this as sugar. **/
    public DynamicFieldType setStoreTermVectorOffsetS(boolean value)
    {
        super.setStoreTermVectorOffsets(value);

        return this;
    }




    /** Same functionality as in upper class method, but returns this as sugar. **/
    public DynamicFieldType setStoreTermVectorPositionS(boolean value)
    {
        super.setStoreTermVectorPositions(value);

        return this;
    }




    /** Same functionality as in upper class method, but returns this as sugar. **/
    public DynamicFieldType setStoreTermVectorPayloadS(boolean value)
    {
        super.setStoreTermVectorPayloads(value);

        return this;
    }




    /** Same functionality as in upper class method, but returns this as sugar. **/
    public DynamicFieldType setOmitNormS(boolean value)
    {
        super.setOmitNorms(value);

        return this;
    }




    /** Same functionality as in upper class method, but returns this as sugar. **/
    public DynamicFieldType setIndexOptionS(IndexOptions value)
    {
        super.setIndexOptions(value);

        return this;
    }




    /** Same functionality as in upper class method, but returns this as sugar. **/
    public DynamicFieldType setNumericTypE(NumericType type)
    {
        super.setNumericType(type);

        return this;
    }




    /** Same functionality as in upper class method, but returns this as sugar. **/
    public DynamicFieldType setNumericPrecisionSteP(int precisionStep)
    {
        super.setNumericPrecisionStep(precisionStep);

        return this;
    }




    /** Same functionality as in upper class method, but returns this as sugar. **/
    public DynamicFieldType setDocValueTypE(DocValuesType type)
    {
        super.setDocValueType(type);

        return this;
    }

}