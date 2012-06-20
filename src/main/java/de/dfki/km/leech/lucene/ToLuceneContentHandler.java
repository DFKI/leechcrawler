/*
 * Leech - crawling capabilities for Apache Tika
 * 
 * Copyright (C) 2012 DFKI GmbH, Author: Christian Reuschling
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Contact us by mail: christian.reuschling@dfki.de
 */

package de.dfki.km.leech.lucene;



import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.tika.metadata.Metadata;

import de.dfki.km.leech.lucene.FieldConfig.FieldMapping;
import de.dfki.km.leech.metadata.LeechMetadata;
import de.dfki.km.leech.parser.incremental.IncrementalCrawlingHistory;
import de.dfki.km.leech.sax.DataSinkContentHandler;
import de.dfki.km.leech.util.MultiValueHashMap;



/**
 * This is a content handler that allows to store crawled data into a Lucene index. You are able to configure the field types and the analyzers that
 * should be used. Further, blockindexing with {@link IndexWriter#addDocuments(java.util.Collection, Analyzer)} is supported, you can enable it with
 * {@link ToLuceneContentHandler#setBlockIndexing(boolean)}. If it is enabled, {@link ToLuceneContentHandler} checks whether inside the metadata is a
 * {@link LeechMetadata#childId} or a {@link LeechMetadata#parentId} key. Documents with a {@link LeechMetadata#childId} entry will appear as parent
 * documents, docs with an {@link LeechMetadata#parentId} as childs. {@link ToLuceneContentHandler} collects the child documents if they appear at a
 * processXXX method, and writes them as block at the time a succeeding parent document appears. In the case a non-parent doc appears, all collected
 * docs will be indexed normally, not as block.
 * 
 * @author Christian Reuschling, Dipl.Ing.(BA)
 * 
 */
public class ToLuceneContentHandler extends DataSinkContentHandler
{


    protected PerFieldAnalyzerWrapper m_analyzerOfFieldConfig;



    protected boolean m_bBlockIndexing = true;



    protected FieldConfig m_fieldConfig = new FieldConfig();



    protected HashSet<String> m_hsAttNamesNot2Store = new HashSet<String>();



    protected MultiValueHashMap<String, String> m_hsSource2TargetFieldnames = new MultiValueHashMap<String, String>();







    protected MultiValueHashMap<String, String> m_hsStaticAttValuePairs = new MultiValueHashMap<String, String>();



    protected LinkedList<Document> m_llLastChildDocuments = new LinkedList<Document>();





    protected IndexWriter m_luceneWriter;



    protected MultiValueHashMap<String, String> m_hsTarget2SourcesFieldnames = new MultiValueHashMap<String, String>();



    public ToLuceneContentHandler(FieldConfig fieldConfig, IndexWriter luceneWriter) throws Exception
    {
        super();
        setFieldConfiguration(fieldConfig);
        m_luceneWriter = luceneWriter;

        init();
    }




    public ToLuceneContentHandler(int writeLimit, FieldConfig fieldConfig, IndexWriter luceneWriter) throws Exception
    {
        super(writeLimit);
        setFieldConfiguration(fieldConfig);
        m_luceneWriter = luceneWriter;

        init();
    }



    public ToLuceneContentHandler(Metadata metadata, FieldConfig fieldConfig, IndexWriter luceneWriter) throws Exception
    {
        super(metadata);
        setFieldConfiguration(fieldConfig);
        m_luceneWriter = luceneWriter;

        init();
    }



    public ToLuceneContentHandler(Metadata metadata, int writeLimit, FieldConfig fieldConfig, IndexWriter luceneWriter) throws Exception
    {
        super(metadata, writeLimit);
        setFieldConfiguration(fieldConfig);
        m_luceneWriter = luceneWriter;

        init();
    }



    @Override
    protected void init()
    {
        Logger.getLogger(ToLuceneContentHandler.class.getName()).info("Will write crawled data into " + m_luceneWriter.getDirectory().toString());
    }



    public boolean getBlockIndexing()
    {
        return m_bBlockIndexing;
    }



    /**
     * Gets the field copy mappings. This means that the content of every metadata key that is specified as key inside hsSource2TargetFieldnames will
     * be copied into several other fields. The field names of these fields are specified as corresponding value inside hsSource2TargetFieldnames. In
     * the case you want to rename attribute names, specify a field mapping and ignore the source field name with
     * {@link #setFieldNames2Ignore(HashSet)}
     * 
     * @return the current field mappings
     */
    public MultiValueHashMap<String, String> getFieldCopyMap()
    {
        return m_hsSource2TargetFieldnames;
    }



    /**
     * Gets the set of field names / metadata key values that will NOT be stored into the lucene index.
     * 
     * @return the set of field names / metadata key values that will NOT be stored into the lucene index.
     */
    public HashSet<String> getFields2Ignore()
    {
        return m_hsAttNamesNot2Store;
    }



    /**
     * Sets some attribute value pairs that will be added to every crawled document.
     * 
     * @return the current static attribute value pairs
     */
    public MultiValueHashMap<String, String> getStaticAttributeValuePairs()
    {
        return m_hsStaticAttValuePairs;
    }



    @Override
    public void processErrorData(Metadata metadata)
    {
        // NOP
    }



    @Override
    public void processModifiedData(Metadata metadata, String strFulltext)
    {

        try
        {

            // hier modifizieren wir ein schon vorhandenes Dokument
            Document luceneDocument = createAndFillLuceneDocument(metadata, strFulltext);
            if(luceneDocument == null) return;



            // TODO: was passiert hier mit block-indexierten Dokumenten?
            m_luceneWriter.updateDocument(
                    new Term(IncrementalCrawlingHistory.dataEntityExistsID, metadata.get(IncrementalCrawlingHistory.dataEntityExistsID)),
                    luceneDocument);

        }
        catch (Exception e)
        {
            Logger.getLogger(ToLuceneContentHandler.class.getName()).log(Level.SEVERE, "Error during writing into the index", e);
        }

    }



    protected void addStaticAttValuePairs(Document doc) throws Exception
    {
        for (Entry<String, String> fieldName2Value : getStaticAttributeValuePairs().entryList())
            doc.add(FieldFactory.createField(fieldName2Value.getKey(), fieldName2Value.getValue(), m_fieldConfig));
    }



    @Override
    public void processNewData(Metadata metadata, String strFulltext)
    {

        try
        {
            if(m_luceneWriter == null) throw new IllegalStateException("Lucene writer was not specified");

            // wir wollen noch die parent-child dokumente als solche mit writer.addDocuments(.., analyzer) indexieren. Die sind ja in den
            // Metadaten markiert, erst kommen die childs, dann die parents (vielleicht muß das aber auch nicht zwingend sein^^)
            Document doc = createAndFillLuceneDocument(metadata, strFulltext);



            // wenn es ein parent oder childDoc ist, dann merken wir uns dieses erst mal, bis wir einen ganzen Block haben. Wenn wir auf ein childDoc
            // stossen, dann schreiben wir beim nächsten parendDoc, und merken uns alle childs bis dahin
            // - wenn wir auf ein Doc ohne parent-oder child-Id stossen, dann schreiben wir alle bisherigen Docs als Einzeldokumente raus - nicht im
            // Block

            if(getBlockIndexing())
            {


                if(metadata.get(LeechMetadata.childId) != null)
                {
                    // wir haben ein child-Doc. Das merken wir uns einfach
                    m_llLastChildDocuments.add(doc);
                }
                else if(metadata.get(LeechMetadata.parentId) != null)
                {
                    // wir haben ein parentDoc - wir schreiben zusammen mit den bisher gesammelten im block. Das parentDoc ist das letzte
                    m_llLastChildDocuments.add(doc);
                    m_luceneWriter.addDocuments(m_llLastChildDocuments, m_analyzerOfFieldConfig);

                    m_llLastChildDocuments.clear();
                }
                else
                {
                    // wir haben weder child-noch parent ID - alle gemerkten childDocs werden als Einzeldocs rausgeschrieben
                    for (Document orphanDoc : m_llLastChildDocuments)
                        m_luceneWriter.addDocument(orphanDoc, m_analyzerOfFieldConfig);

                    m_luceneWriter.addDocument(doc, m_analyzerOfFieldConfig);
                }


            }
            else
            {
                m_luceneWriter.addDocument(doc, m_analyzerOfFieldConfig);
            }




        }
        catch (Exception e)
        {
            Logger.getLogger(ToLuceneContentHandler.class.getName()).log(Level.SEVERE, "Error", e);
        }

    }




    protected Document createAndFillLuceneDocument(Metadata metadata, String strFulltext) throws Exception
    {
        Document doc = new Document();


        // Das man mein Field aus einem reader machen kann ist der Grund, warum processNewMetaData den Fulltext als String und nicht als reader
        // übergibt

        if(!getFields2Ignore().contains(LeechMetadata.body)) doc.add(FieldFactory.createField(LeechMetadata.body, strFulltext, m_fieldConfig));
        // die kopien
        for (String strFieldCopy : getFieldCopyMap().get(LeechMetadata.body))
            if(!getFields2Ignore().contains(strFieldCopy)) doc.add(FieldFactory.createField(strFieldCopy, strFulltext, m_fieldConfig));


        // die restlichen metadaten
        for (String strFieldName : metadata.names())
        {
            if(!getFields2Ignore().contains(strFieldName))
            {
                for (String strValue : metadata.getValues(strFieldName))
                    doc.add(FieldFactory.createField(strFieldName, strValue, m_fieldConfig));
            }

            // die kopien
            for (String strFieldCopy : getFieldCopyMap().get(strFieldName))
                if(!getFields2Ignore().contains(strFieldCopy))
                {
                    for (String strValue : metadata.getValues(strFieldName))
                        doc.add(FieldFactory.createField(strFieldCopy, strValue, m_fieldConfig));
                }
        }

        // die statischen Attribut-Value-Paare
        addStaticAttValuePairs(doc);

        // und jetzt aggregieren wir noch
        for (String strTargetAtt : getFieldAggregationMap().keySet())
        {
            // wenn es das TargetAtt schon im doc gibt, dann aggregieren wir nix
            if(doc.get(strTargetAtt) != null) continue;

            Collection<String> colSourceAtts = getFieldAggregationMap().get(strTargetAtt);

            for (String strSourceAtt : colSourceAtts)
            {
                String strNewValue = metadata.get(strSourceAtt);
                if(strNewValue == null) strNewValue = getStaticAttributeValuePairs().getFirst(strSourceAtt);

                if(strNewValue != null)
                {
                    doc.add(FieldFactory.createField(strTargetAtt, strNewValue, m_fieldConfig));

                    break;
                }
            }
        }




        return doc;
    }



    @Override
    public void processRemovedData(Metadata metadata)
    {
        // da kann man ja mit den inkremental-Ids spielen, die stehen ja evtl. noch in den Metadaten drin :) :)

        try
        {

            // TODO: was passiert hier mit block-indexierten Dokumenten?
            m_luceneWriter.deleteDocuments(new Term(IncrementalCrawlingHistory.dataEntityExistsID, metadata
                    .get(IncrementalCrawlingHistory.dataEntityExistsID)));

        }
        catch (Exception e)
        {
            Logger.getLogger(ToLuceneContentHandler.class.getName()).log(Level.SEVERE, "Error during writing into the index", e);
        }

    }



    /**
     * Sets whether block indexing with {@link IndexWriter#addDocuments(java.util.Collection, Analyzer)} is enabled or not. If it is enabled,
     * {@link ToLuceneContentHandler} checks whether inside the metadata is a {@link LeechMetadata#childId} or a {@link LeechMetadata#parentId} key.
     * Documents with a {@link LeechMetadata#childId} entry will appear as parent documents, docs with an {@link LeechMetadata#parentId} as childs.
     * {@link ToLuceneContentHandler} collects the child documents if they appear at a processXXX method, and writes them as block at the time a
     * succeeding parent document appears. In the case a non-parent doc appears, all collected docs will be indexed normally, not as block.
     * 
     * @param blockIndexing true in the case blockindexing should be inabled, false otherwise.
     */
    public void setBlockIndexing(boolean blockIndexing)
    {
        this.m_bBlockIndexing = blockIndexing;
    }




    public ToLuceneContentHandler setFieldConfiguration(FieldConfig fieldConfig) throws Exception
    {
        m_fieldConfig = fieldConfig;


        String strDefaultAnalyzerName = m_fieldConfig.defaultFieldMapping.analyzer;
        Analyzer defaultAnalyzer = LuceneAnalyzerFactory.createAnalyzer(strDefaultAnalyzerName, null);

        HashMap<String, Analyzer> hsFieldName2Analyzer = new HashMap<String, Analyzer>();
        for (Entry<String, FieldMapping> fieldname2FieldMapping : m_fieldConfig.hsFieldName2FieldMapping.entrySet())
        {
            String strFieldName = fieldname2FieldMapping.getKey();
            String strAnalyzer4Field = fieldname2FieldMapping.getValue().analyzer;
            hsFieldName2Analyzer.put(strFieldName, LuceneAnalyzerFactory.createAnalyzer(strAnalyzer4Field, null));
        }

        m_analyzerOfFieldConfig = new PerFieldAnalyzerWrapper(defaultAnalyzer, hsFieldName2Analyzer);

        return this;
    }







    /**
     * Sets the field copy mappings. This means that the content of every metadata key that is specified as key inside hsSource2TargetFieldnames will
     * be copied into several other fields. The field names of these fields are specified as corresponding value inside hsSource2TargetFieldnames. In
     * the case you want to rename attribute names, specify a field mapping and ignore the source field name with
     * {@link #setFieldNames2Ignore(HashSet)}
     * 
     * @param hsSource2TargetFieldnames keys: source field names, given as metadata keys. values: target field names - the content will also appear
     *            under these fields inside a lucene document
     */
    public void setFieldCopyMap(MultiValueHashMap<String, String> hsSource2TargetFieldnames)
    {
        m_hsSource2TargetFieldnames = hsSource2TargetFieldnames;
    }



    /**
     * Sets the field aggregation map. This means that you want to generate a field entry, whereby its value should be copied from another, existing
     * metadata entry. You can specify a list of these source-attributes, the first who have an entry wins and appears as new attribute, so the source
     * field name list is in fact a priorized list.
     * 
     * @param hsTarget2SourcesFieldnames the field aggregation map
     */
    public void setFieldAggregationMap(MultiValueHashMap<String, String> hsTarget2SourcesFieldnames)
    {
        m_hsTarget2SourcesFieldnames = hsTarget2SourcesFieldnames;
    }



    /**
     * Gets the field aggregation map. This means that you want to generate a field entry, whereby its value should be copied from another, existing
     * metadata entry. You can specify a list of these source-attributes, the first who have an entry wins and appears as new attribute, so the source
     * field name list is in fact a priorized list.
     * 
     * @return the current field aggregation map
     */
    public MultiValueHashMap<String, String> getFieldAggregationMap()
    {
        return m_hsTarget2SourcesFieldnames;
    }




    /**
     * Sets the set of field names / metadata key values that will NOT be stored into the lucene index. Nevertheless, you can consider these in
     * {@link #setFieldCopyMap(MultiValueHashMap)}. In this case you have 'moved' the attribute value into another attribute (or several ones).
     * 
     * @param hsAttNamesNot2Store the set of attribute/field names that will not stored into the lucene index
     */
    public void setFieldNames2Ignore(HashSet<String> hsAttNamesNot2Store)
    {
        m_hsAttNamesNot2Store = hsAttNamesNot2Store;
    }







    /**
     * Sets some attribute value pairs that will be added to every crawled document.
     * 
     * @param hsStaticAttValuePairs a multi value map containing the additional attribute value pairs
     */
    public void setStaticAttributeValuePairs(MultiValueHashMap<String, String> hsStaticAttValuePairs)
    {
        m_hsStaticAttValuePairs = hsStaticAttValuePairs;
    }







}
