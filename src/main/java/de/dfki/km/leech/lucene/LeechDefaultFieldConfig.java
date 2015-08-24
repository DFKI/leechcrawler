package de.dfki.km.leech.lucene;



import de.dfki.inquisition.lucene.DynamicFieldType;
import de.dfki.inquisition.lucene.FieldConfig;



public class LeechDefaultFieldConfig extends FieldConfig
{

    public LeechDefaultFieldConfig()
    {

        this.defaultFieldType = DynamicFieldType.tokenizedFieldType;


        this.fieldName2FieldType.put("Content-Type", DynamicFieldType.keywordFieldType);
        this.fieldName2FieldType.put("dynaqCategory", DynamicFieldType.keywordFieldType);
        this.fieldName2FieldType.put("isHeuristicPageCount", DynamicFieldType.keywordFieldType);
        this.fieldName2FieldType.put("leechId", DynamicFieldType.keywordFieldType);
        this.fieldName2FieldType.put("leechChildId", DynamicFieldType.keywordFieldType);
        this.fieldName2FieldType.put("leechParentId", DynamicFieldType.keywordFieldType);
        this.fieldName2FieldType.put("isPostProcessed", DynamicFieldType.keywordFieldType);
        this.fieldName2FieldType.put("Content-Encoding", DynamicFieldType.keywordFieldType);
        this.fieldName2FieldType.put("content-language", DynamicFieldType.keywordFieldType);
        this.fieldName2FieldType.put("dc:language", DynamicFieldType.keywordFieldType);
        this.fieldName2FieldType.put("dataEntityContentFingerprint", DynamicFieldType.keywordFieldType);
        this.fieldName2FieldType.put("dataEntityId", DynamicFieldType.keywordFieldType);
        this.fieldName2FieldType.put("masterDataEntityId", DynamicFieldType.keywordFieldType);
        this.fieldName2FieldType.put("id", DynamicFieldType.keywordFieldType);
        
        
        this.fieldName2FieldType.put("domain", DynamicFieldType.keywordFieldType);
        this.fieldName2FieldType.put("authorContinent", DynamicFieldType.keywordFieldType);
        this.fieldName2FieldType.put("country", DynamicFieldType.keywordFieldType);
        this.fieldName2FieldType.put("source", DynamicFieldType.keywordFieldType);
        this.fieldName2FieldType.put("globalSource", DynamicFieldType.keywordFieldType);
        this.fieldName2FieldType.put("continentCode", DynamicFieldType.keywordFieldType);
        this.fieldName2FieldType.put("countryCode", DynamicFieldType.keywordFieldType);
        this.fieldName2FieldType.put("steep", DynamicFieldType.keywordFieldType);
        this.fieldName2FieldType.put("topic1", DynamicFieldType.keywordFieldType);
        this.fieldName2FieldType.put("queryName", DynamicFieldType.keywordFieldType);
        this.fieldName2FieldType.put("dctype", DynamicFieldType.keywordFieldType);
        this.fieldName2FieldType.put("dcdocid", DynamicFieldType.keywordFieldType);
        this.fieldName2FieldType.put("dcautoclasscode", DynamicFieldType.keywordFieldType);
        this.fieldName2FieldType.put("dclang", DynamicFieldType.keywordFieldType);
        this.fieldName2FieldType.put("dcdoi", DynamicFieldType.keywordFieldType);
        this.fieldName2FieldType.put("dcoa", DynamicFieldType.keywordFieldType);
        this.fieldName2FieldType.put("dccountry", DynamicFieldType.keywordFieldType);
        this.fieldName2FieldType.put("dccontinent", DynamicFieldType.keywordFieldType);
        this.fieldName2FieldType.put("containerSourceContentType", DynamicFieldType.keywordFieldType);
        this.fieldName2FieldType.put("dcrights", DynamicFieldType.keywordFieldType);
        this.fieldName2FieldType.put("containerSource", DynamicFieldType.keywordFieldType);
        this.fieldName2FieldType.put("dcyear", DynamicFieldType.keywordFieldType);
        this.fieldName2FieldType.put("dctypenorm", DynamicFieldType.keywordFieldType);
        this.fieldName2FieldType.put("dcidentifier", DynamicFieldType.keywordFieldType);
        this.fieldName2FieldType.put("dclink", DynamicFieldType.keywordFieldType);
        this.fieldName2FieldType.put("language", DynamicFieldType.keywordFieldType);
        this.fieldName2FieldType.put("dccollection", DynamicFieldType.keywordFieldType);
        
        
        


        this.fieldName2FieldType.put("Page-Count", DynamicFieldType.integerFieldType);
        this.fieldName2FieldType.put("Word-Count", DynamicFieldType.integerFieldType);
        this.fieldName2FieldType.put("documentFrequencyClass", DynamicFieldType.integerFieldType);
        this.fieldName2FieldType.put("Image Count", DynamicFieldType.integerFieldType);
        this.fieldName2FieldType.put("Paragraph-Count", DynamicFieldType.integerFieldType);
        
        this.fieldName2FieldType.put("Character Count", DynamicFieldType.longFieldType);

        this.fieldName2FieldType.put("modified", DynamicFieldType.dateFieldType);
        this.fieldName2FieldType.put("Creation-Date", DynamicFieldType.dateFieldType);
        this.fieldName2FieldType.put("Last-Modified", DynamicFieldType.dateFieldType);
        this.fieldName2FieldType.put("date", DynamicFieldType.dateFieldType);
        this.fieldName2FieldType.put("dynaqSignificantDate", DynamicFieldType.dateFieldType);
        this.fieldName2FieldType.put("CHANGED", DynamicFieldType.dateFieldType);
        this.fieldName2FieldType.put("CREATED", DynamicFieldType.dateFieldType);
        this.fieldName2FieldType.put("created", DynamicFieldType.dateFieldType);
        this.fieldName2FieldType.put("Last-Printed", DynamicFieldType.dateFieldType);
        this.fieldName2FieldType.put("Last-Save-Date", DynamicFieldType.dateFieldType);
        
        this.fieldName2FieldType.put("longitude", DynamicFieldType.doubleFieldType);
        this.fieldName2FieldType.put("latitude", DynamicFieldType.doubleFieldType);

        
    }

}
