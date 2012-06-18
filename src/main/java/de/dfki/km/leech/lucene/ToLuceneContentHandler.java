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



import java.util.HashMap;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.tika.metadata.Metadata;

import de.dfki.km.leech.lucene.FieldConfig.FieldMapping;
import de.dfki.km.leech.sax.DataSinkContentHandler;



/**
 * Hier höchstens etwas exemplarisches, wenn überhaupt - unser Lucene-ContentHandler kommt hübsch nach DynaQ
 * 
 * @author Christian Reuschling, Dipl.Ing.(BA)
 * 
 */
public class ToLuceneContentHandler extends DataSinkContentHandler
{


    protected FieldConfig m_fieldConfig;



    protected IndexWriter m_luceneWriter;



    public ToLuceneContentHandler(FieldConfig fieldConfig, IndexWriter luceneWriter)
    {
        super();
        m_fieldConfig = fieldConfig;
        m_luceneWriter = luceneWriter;
    }







    public ToLuceneContentHandler(int writeLimit, FieldConfig fieldConfig, IndexWriter luceneWriter)
    {
        super(writeLimit);
        m_fieldConfig = fieldConfig;
        m_luceneWriter = luceneWriter;
    }



    public ToLuceneContentHandler(Metadata metadata, FieldConfig fieldConfig, IndexWriter luceneWriter)
    {
        super(metadata);
        m_fieldConfig = fieldConfig;
        m_luceneWriter = luceneWriter;
    }





    public ToLuceneContentHandler(Metadata metadata, int writeLimit, FieldConfig fieldConfig, IndexWriter luceneWriter)
    {
        super(metadata, writeLimit);
        m_fieldConfig = fieldConfig;
        m_luceneWriter = luceneWriter;
    }



    @Override
    public void processErrorData(Metadata metadata)
    {
        // NOP

    }




    @Override
    public void processModifiedData(Metadata metadata, String strFulltext)
    {


        // TODO da kann man ja mit den inkremental-Ids spielen, die stehen ja evtl. noch in den Metadaten drin :) :)
    }





    @Override
    public void processNewData(Metadata metadata, String strFulltext)
    {

        try
        {

            // TODO wir wollen noch die parent-child dokumente als solche mit writer.addDocuments(.., analyzer) indexieren. Die sind ja in den
            // Metadaten markiert, erst kommen die childs, dann die parents (vielleicht muß das aber auch nicht zwingend sein^^)
            Document doc = new Document();


            // Das man mein Field aus einem reader machen kann ist der Grund, warum processNewMetaData den Fulltext als String und nicht als reader
            // übergibt

            doc.add(FieldFactory.createField("fulltext", strFulltext, m_fieldConfig));

            // die restlichen metadaten

            for (String strFieldName : metadata.names())
            {
                for (String strValue : metadata.getValues(strFieldName))
                {
                    doc.add(FieldFactory.createField(strFieldName, strValue, m_fieldConfig));
                }
            }


            String strDefaultAnalyzerName = m_fieldConfig.defaultFieldMapping.analyzer;
            Analyzer defaultAnalyzer = LuceneAnalyzerFactory.createAnalyzer(strDefaultAnalyzerName, null);

            HashMap<String, Analyzer> hsFieldName2Analyzer = new HashMap<String, Analyzer>();
            for (Entry<String, FieldMapping> fieldname2FieldMapping : m_fieldConfig.hsFieldName2FieldMapping.entrySet())
            {
                String strFieldName = fieldname2FieldMapping.getKey();
                String strAnalyzer4Field = fieldname2FieldMapping.getValue().analyzer;
                hsFieldName2Analyzer.put(strFieldName, LuceneAnalyzerFactory.createAnalyzer(strAnalyzer4Field, null));
            }

            PerFieldAnalyzerWrapper analyzer = new PerFieldAnalyzerWrapper(defaultAnalyzer, hsFieldName2Analyzer);


            m_luceneWriter.addDocument(doc, analyzer);


        }
        catch (Exception e)
        {
            Logger.getLogger(ToLuceneContentHandler.class.getName()).log(Level.SEVERE, "Error", e);
        }

    }





    @Override
    public void processRemovedData(Metadata metadata)
    {
        // TODO da kann man ja mit den inkremental-Ids spielen, die stehen ja evtl. noch in den Metadaten drin :) :)

    }







    public ToLuceneContentHandler setFieldConfiguration(FieldConfig fieldConfig)
    {
        m_fieldConfig = fieldConfig;

        return this;
    }







}
