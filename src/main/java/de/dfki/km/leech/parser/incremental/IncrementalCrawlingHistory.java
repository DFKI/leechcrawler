/*
 * Leech - crawling capabilities for Apache Tika
 * 
 * Copyright (C) 2012 DFKI GmbH, Author: Christian Reuschling
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Contact us by mail: christian.reuschling@dfki.de
 */

package de.dfki.km.leech.parser.incremental;



import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Bits;

import de.dfki.inquisition.text.StringUtils;
import de.dfki.km.leech.config.CrawlerContext;



/**
 * A persistent history database, remarking everything that was processed during a crawl. This history makes it possible to fulfill incremental crawling, where you can
 * quickly check whether a data entity found during the crawl is new or modified with respect to the last crawl. Further, all data entities that was removed since the
 * last crawl can be determined for final synchronization.<br>
 * To check whether a file is new or modified, IncrementalCrawlingHistory needs two informations: a 'data entity exists ID', which is an identifier for a data entity that
 * is independent from the content of this entity. It is only for identifying the existence, not to check whether it has changed. A 'data entity content fingerprint'
 * gives the hint whether the content of the data entity has changed. This e.g. can be the modifed date of a file, or a mail header hash.<br>
 * To determine the data entities that were removed since the last crawl, IncrementalCrawlingHistory remarks the crawl starting time, and updates a 'last crawled/checked
 * time' entry for every data entity. When the crawl is finished, every data entity which 'last crawled/checked time' is before the remarked crawl starting time is
 * considered as outdated and thus as removed.<br>
 * This is an easy, intuitive, general approach that should work for almost all possible data entities. Other approaches stores e.g. parent/child relationships of data
 * entities, maintain resulting relationship lists, and infer whether an entity was deleted or not. These approaches have the advantage that you can determine, in some
 * cases, immediately by crawling a container data source whether a data entity was deleted or not, before the recursive call. Nevertheless, where this is easy in e.g.
 * file system data sources, in other scenarios as web crawlers this is much more complicated, where a link can be potentially part of several 'container websites'.<br>
 * The timestamp-approach we choose is much easier and works in all scenarios with the same conditions, but has 2 disadvantages against the other approaches:<br>
 * <li>You have to update every data entity history entry on every crawl with the new 'last crawled/checked time', even if the entity has not changed at all.<br> <li>The
 * information which data entities were removed can be determined only at the end of a crawl, for the whole history. <br>
 * <br>
 * We realized this crawling history with an underlying Lucene index.
 * 
 * <br>
 * To enable incremental indexing during a crawl, pass a CrawlerConfig instance with a path to the history into the ParseContext parameter of the Leech.parse(..) method:<br>
 * <code>
 * Leech leech = new Leech();<br>
 * Metadata metadata = new Metadata();<br>
 * {@link CrawlerContext} crawlerContext = new {@link CrawlerContext}().setIncrementalCrawlingHistoryPath("./history/forResourceDir");<br>
 * leech.parse(new File("resource"), new PrintlnContentHandler(metadata), crawlerContext.createParseContext());<br>
 * </code> <br>
 * Make sure that you always use the according history for a specific crawling source - this is a 1:1 relationship, you can't mix. Otherwise, all new stuff will be
 * considered as new, and all old stuff as deleted.
 * 
 * @author Christian Reuschling, Dipl.Ing.(BA)
 */
public class IncrementalCrawlingHistory
{


    protected class CrawlFinishedIterator implements Iterator<String>
    {

        protected LinkedList<String> m_llQueuedOutdatedIDs = new LinkedList<String>();

        protected Query m_query = null;



        protected CrawlFinishedIterator() throws IOException
        {

            if(m_lCrawlStartingTime == null) throw new IllegalStateException("No crawl starting time found. Did you invoke crawlStarted?");


            m_query = LongPoint.newRangeQuery(lastCrawledTime, 0l, m_lCrawlStartingTime - 1);
        }



        @Override
        public boolean hasNext()
        {
            try
            {

                // soo - hier stellen wir die Suchfrage, ob noch outdated entities vorhanden sind - wenn wir nicht noch welche in der queue von der
                // letzten Anfrage haben. Wenn wir false zurück geben, dann machen wir die ganzen Lucene-Teile zu.

                if(m_query == null) return false;

                // wenn wir nix mehr haben, dann stellen wir eine Suchanfrage
                if(m_llQueuedOutdatedIDs.size() == 0)
                {

                    refreshIndexReaderz();
                    TopDocs topDocs = m_indexSearcher.search(m_query, 5000);

                    Bits liveDocs = MultiFields.getLiveDocs(m_indexReader);

                    for (ScoreDoc scoreDoc : topDocs.scoreDocs)
                    {
                        // skip deleted documents
                        if(liveDocs != null && !liveDocs.get(scoreDoc.doc)) continue;

                        Document doc4Queue = m_indexReader.document(scoreDoc.doc, Collections.singleton(dataEntityId));


                        m_llQueuedOutdatedIDs.add(doc4Queue.get(dataEntityId));
                    }


                    // wenn die queue immer noch leer ist, dann gibts nix mehr
                    if(m_llQueuedOutdatedIDs.size() == 0)
                    {
                        // alles zu - wir sind fertig
                        closeLuceneStuff();

                        return false;
                    }


                }


                return true;


            }
            catch (IOException e)
            {
                Logger.getLogger(IncrementalCrawlingHistory.CrawlFinishedIterator.class.getName()).log(Level.SEVERE, "Error", e);

                return false;
            }
        }



        @Override
        public String next()
        {
            // hier geben wir die id zurück und löschen sie vorher aus der queue und dem index
            try
            {
                if(m_llQueuedOutdatedIDs.isEmpty()) return null;


                m_indexWriter.deleteDocuments(new Term(dataEntityId, m_llQueuedOutdatedIDs.getFirst()));

                return m_llQueuedOutdatedIDs.poll();

            }
            catch (Exception e)
            {
                Logger.getLogger(IncrementalCrawlingHistory.CrawlFinishedIterator.class.getName()).log(Level.SEVERE, "Error", e);
            }

            return null;
        }



        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }


    }




    /**
     * Defines the states whether a data entity is in the history or not. There are three states: Exist.NOT says that the data entity has no entry inside the history at
     * all. Exist.YES_UNPROCESSED means that the entity has an entry inside the history, and that it still wasn't processed during the current crawl. Exist.YES_PROCESSED
     * means that there is an entry but the data entity was processed in this run yet, so normally another processing is unnecessary. This is to detect cycles.
     * 
     * @author Christian Reuschling, Dipl.Ing.(BA)
     */
    public enum Exist {
        NOT, YES_PROCESSED, YES_UNPROCESSED
    }


    static public final String dataEntityContentFingerprint = "dataEntityContentFingerprint";

    static public final String dataEntityId = "dataEntityId";

    static public final String masterDataEntityId = "masterDataEntityId";

    static public final String lastCrawledTime = "lastCrawledTime";

    protected DirectoryReader m_indexReader = null;

    protected IndexSearcher m_indexSearcher = null;

    protected IndexWriter m_indexWriter = null;



    protected Long m_lCrawlStartingTime = null;



    protected final String m_strHistoryPath;



    public IncrementalCrawlingHistory(String strHistoryPath)
    {
        m_strHistoryPath = strHistoryPath;


        Runtime.getRuntime().addShutdownHook(new Thread("IncrementalCrawlingHistory shutdown hook for " + strHistoryPath)
        {
            @Override
            public void run()
            {
                try
                {
                    closeLuceneStuff();
                }
                catch (IOException e)
                {
                    Logger.getLogger(IncrementalCrawlingHistory.class.getName()).log(Level.SEVERE, "Error", e);
                }
            }
        });
    }



    /**
     * Remarks a new data entity, together with the current time as 'last crawled/checked time'.
     * 
     * @param strDataEntityId an identifier for a data entity that is independent from the content of this entity. It is only for identifying the occurence, not to
     *            check whether it has changed (e.g. a filename)
     * @param strDataEntityContentFingerprint some fingerprint/identifier that gives the hint whether the content of the data entity has changed, e.g. the modifed date of
     *            a file
     * 
     * @throws IOException
     * @throws CorruptIndexException
     */
    public void addDataEntity(String strDataEntityId, String strDataEntityContentFingerprint) throws CorruptIndexException, IOException
    {
        addDataEntity(strDataEntityId, strDataEntityContentFingerprint, null);
    }



    /**
     * Remarks a new data entity, together with the current time as 'last crawled/checked time'.
     * 
     * @param strDataEntityId an identifier for a data entity that is independent from the content of this entity. It is only for identifying the occurence, not to
     *            check whether it has changed (e.g. a filename)
     * @param strDataEntityContentFingerprint some fingerprint/identifier that gives the hint whether the content of the data entity has changed, e.g. the modifed date of
     *            a file
     * @param strMasterDataEntityId optional: an EntityId of another data entity that is our 'master' which means that when the master is updated with
     *            {@link #updateDataEntityLastCrawledTime(String)}, all associated slaves will be also updated. This is e.g. for the case when you are in a second run for
     *            RSS-File indexing, and leech recognizes that this file didn't changed. Now we don't want to go unnecessarily into the fil and mark each entry on it's
     *            own. We know no subentry has changed, and can immediately mark them as processed with {@link #updateDataEntityLastCrawledTime(String)} on the master
     *            dataEntityId, which is the one from the RSS file. Leave it null or empty in the case you don't need to use it.
     * 
     * @throws IOException
     * @throws CorruptIndexException
     */
    public void addDataEntity(String strDataEntityId, String strDataEntityContentFingerprint, String strMasterDataEntityId) throws CorruptIndexException,
            IOException
    {

        Document doc = new Document();

        doc.add(new StringField(dataEntityId, strDataEntityId, Store.YES));
        doc.add(new StringField(dataEntityContentFingerprint, strDataEntityContentFingerprint, Store.YES));
        doc.add(new LongPoint(lastCrawledTime, System.currentTimeMillis()));
        doc.add(new StoredField(lastCrawledTime, System.currentTimeMillis()));
        if(!StringUtils.nullOrWhitespace(strMasterDataEntityId)) doc.add(new StringField(masterDataEntityId, strMasterDataEntityId, Store.YES));

        m_indexWriter.addDocument(doc);

    }



    public void closeLuceneStuff() throws IOException
    {


        if(m_indexSearcher != null)
        {
            m_indexSearcher = null;
        }

        if(m_indexReader != null)
        {
            m_indexReader.close();
            m_indexReader = null;
        }

        if(m_indexWriter != null)
        {
            m_indexWriter.commit();
            m_indexWriter.close();
            m_indexWriter = null;
        }

    }



    /**
     * Returns all DataEntityIds with a 'last crawled/checked time' before the 'crawl starting time' as outdated data entities. These are all entities that doesn't
     * exist in this crawl anymore, and thus can be considered as removed.<br>
     * You can only invoke and walk to the iterator once - while iterating, the outdated entries inside the history will be deleted. In the case you invoke this method
     * twice, the second invocation will result into an empty list. This is to ensure that also huge deleted entity lists can be handled without problematic memory
     * consumption.<br>
     * Remark: The writer and reader instance for the underlying lucene index will be closed when you walk the iterator to the end, all data will be committed before.
     * 
     * @return all DataEntityIds with a 'last crawled/checked time' before the 'crawl starting time', thus all entities that can be considered as removed.
     */
    public Iterator<String> crawlFinished()
    {

        try
        {

            return new CrawlFinishedIterator();

        }
        catch (IOException e)
        {
            Logger.getLogger(IncrementalCrawlingHistory.class.getName()).log(Level.SEVERE, "Error", e);
            return null;
        }

    }



    /**
     * Informs the history that a new crawl has started. The history will save the current time as 'crawl starting time'. <br>
     * Remark: The writer and reader instance for the underlying lucene index will be opened if necessary
     * 
     * @throws IOException
     * @throws LockObtainFailedException
     * @throws CorruptIndexException
     */
    public void crawlStarted() throws CorruptIndexException, LockObtainFailedException, IOException
    {

        openLuceneStuff();


        // wir merken uns die aktuelle crawlStartingTime - diese wird in CrawlFinished gebraucht, um die outdated entities zu ermitteln.
        m_lCrawlStartingTime = System.currentTimeMillis();

    }




    /**
     * Checks whether an ID exists inside the incremental crawling history or not. During the crawl, this is to identify quickly whether a data entity is completely new
     * or not.
     * 
     * @param strDataEntityId an identifier for a data entity that is independent from the content of this entity. It is only for identifying the occurence, not to
     *            check whether it has changed (e.g. a filename)
     * 
     * @return There are three states: Exist.NOT says that the data entity has no entry inside the history at all. Exist.YES_UNPROCESSED means that the entity has an
     *         entry inside the history, and that it still wasn't processed during the current crawl. Exist.YES_PROCESSED means that there is an entry but the data entity
     *         was processed in this run yet, so normally another processing is unnecessary. This is to detect cycles.
     * 
     * @throws IOException
     */
    public Exist exists(String strDataEntityId) throws IOException
    {

        Long lDataEntityLastCrawledTime = getDataEntityLastCrawledTime(strDataEntityId);


        if(lDataEntityLastCrawledTime == null) return Exist.NOT;

        if(lDataEntityLastCrawledTime >= m_lCrawlStartingTime) return Exist.YES_PROCESSED;

        return Exist.YES_UNPROCESSED;
    }



    /**
     * Checks whether an ID with a specific content fingerprint exists in the crawling history or not. During the crawl, this is to identify quickly whether a data entity
     * has changed its content or not. Of course, this makes only sense in the case the content fingerprint that gives the hint whether the entity has changed can be
     * created quickly, at best without extracting the content. Such a fingerprint can be e.g. a modified date of a file, or the time attribute of an email.
     * 
     * @param strDataEntityId an identifier for a data entity that is independent from the content of this entity. It is only for identifying the occurence, not to
     *            check whether it has changed (e.g. a filename)
     * @param strDataEntityContentFingerprint some fingerprint/identifier that gives the hint whether the content of the data entity has changed, e.g. the modifed date of
     *            a file
     * 
     * @return true in the case this identifier exists with exact this content fingerprint inside the crawling history
     * 
     * @throws IOException
     */
    public boolean existsWithContent(String strDataEntityId, String strDataEntityContentFingerprint) throws IOException
    {
        if(StringUtils.nullOrWhitespace(strDataEntityId)) return false;
        
        BooleanQuery query = (new BooleanQuery.Builder())
                                 .add(new TermQuery(new Term(dataEntityId, strDataEntityId)), Occur.MUST)
                                 .add(new TermQuery(new Term(dataEntityContentFingerprint, strDataEntityContentFingerprint)), Occur.MUST)
                                 .build();

        TotalHitCountCollector collector = new TotalHitCountCollector();

        refreshIndexReaderz();
        m_indexSearcher.search(query, collector);


        if(collector.getTotalHits() > 0) return true;

        return false;
    }



    /**
     * Gets the stored content fingerprint for a given data entity entry.
     * 
     * @param strDataEntityId an identifier for a data entity that is independent from the content of this entity. It is only for identifying the occurence, not to
     *            check whether it has changed (e.g. a filename)
     * 
     * @return the according content fingerprint stored for this data entity, null in the case this data entity was not found
     * 
     * @throws IOException
     */
    public String getDataEntityContentFingerprint(String strDataEntityId) throws IOException
    {
        if(StringUtils.nullOrWhitespace(strDataEntityId)) return null;
        
        Term termId = new Term(dataEntityId, strDataEntityId);

        refreshIndexReaderz();
        TopDocs topDocs = m_indexSearcher.search(new TermQuery(termId), 1);

        if(topDocs.totalHits == 0) return null;

        Document doc = m_indexReader.document(topDocs.scoreDocs[0].doc, Collections.singleton(dataEntityContentFingerprint));

        return doc.get(dataEntityContentFingerprint);
    }



    /**
     * Gets the stored last crawled time for a given data entity entry. This can be used to e.g. determine whether a data entity was already processed during the current
     * crawl or not. If it was processed already, this is a hint for a cycle.
     * 
     * @param strDataEntityId an identifier for a data entity that is independent from the content of this entity. It is only for identifying the occurence, not to
     *            check whether it has changed (e.g. a filename)
     * 
     * @return the according last crawled time stored for this data entity, null in the case this data entity was not found
     * 
     * @throws IOException
     */
    public Long getDataEntityLastCrawledTime(String strDataEntityId) throws IOException
    {
        if(StringUtils.nullOrWhitespace(strDataEntityId)) return null;
        
        Term termId = new Term(dataEntityId, strDataEntityId);

        refreshIndexReaderz();
        TopDocs topDocs = m_indexSearcher.search(new TermQuery(termId), 1);

        if(topDocs.totalHits == 0) return null;

        Document doc = m_indexReader.document(topDocs.scoreDocs[0].doc, Collections.singleton(lastCrawledTime));

        return Long.valueOf(doc.get(lastCrawledTime));
    }



    /**
     * Gets the path to this history
     * 
     * @return the path to this history
     */
    public String getHistoryPath()
    {
        return m_strHistoryPath;
    }




    /**
     * Creates all writer, reader, and searcher objects if necessary
     * 
     * @throws CorruptIndexException
     * @throws LockObtainFailedException
     * @throws IOException
     */
    public void openLuceneStuff() throws CorruptIndexException, LockObtainFailedException, IOException
    {
        if(m_indexWriter == null)
        {
            IndexWriterConfig config = new IndexWriterConfig(new KeywordAnalyzer());
            config.setOpenMode(OpenMode.CREATE_OR_APPEND);

            m_indexWriter = new IndexWriter(new SimpleFSDirectory(Paths.get(m_strHistoryPath)), config);
        }

        if(m_indexReader == null) m_indexReader = DirectoryReader.open(m_indexWriter, true, true);

        if(m_indexSearcher == null) m_indexSearcher = new IndexSearcher(m_indexReader);
    }



    protected void refreshIndexReaderz()
    {
        try
        {
            DirectoryReader newReader = DirectoryReader.openIfChanged(m_indexReader);

            if(newReader != null)
            {
                m_indexReader.close();
                m_indexReader = newReader;
                m_indexSearcher = new IndexSearcher(m_indexReader);
            }

        }
        catch (IOException e)
        {
            Logger.getLogger(IncrementalCrawlingHistory.class.getName()).log(Level.SEVERE, "Error", e);
        }
    }




    /**
     * Updates a whole data entity - same as addDataEntity, but removes a former entry before storing the new one
     * 
     * @param strDataEntityId an identifier for a data entity that is independent from the content of this entity. It is only for identifying the occurence, not to
     *            check whether it has changed (e.g. a filename)
     * @param strDataEntityContentFingerprint some fingerprint/identifier that gives the hint whether the content of the data entity has changed, e.g. the modifed date of
     *            a file
     * 
     * @throws IOException
     * @throws CorruptIndexException
     */
    public void updateDataEntity(String strDataEntityId, String strDataEntityContentFingerprint) throws CorruptIndexException, IOException
    {
        updateDataEntity(strDataEntityId, strDataEntityContentFingerprint, null);
    }



    /**
     * Updates a whole data entity - same as addDataEntity, but removes a former entry before storing the new one
     * 
     * @param strDataEntityId an identifier for a data entity that is independent from the content of this entity. It is only for identifying the occurence, not to
     *            check whether it has changed (e.g. a filename)
     * @param strDataEntityContentFingerprint some fingerprint/identifier that gives the hint whether the content of the data entity has changed, e.g. the modifed date of
     *            a file
     * @param strMasterDataEntityId optional: an EntityId of another data entity that is our 'master' which means that when the master is updated with
     *            {@link #updateDataEntityLastCrawledTime(String)}, all associated slaves will be also updated. This is e.g. for the case when you are in a second run for
     *            RSS-File indexing, and leech recognizes that this file didn't changed. Now we don't want to go unnecessarily into the fil and mark each entry on it's
     *            own. We know no subentry has changed, and can immediately mark them as processed with {@link #updateDataEntityLastCrawledTime(String)} on the master
     *            dataEntityId, which is the one from the RSS file. Leave it null or empty in the case you don't need to use it.
     * 
     * @throws IOException
     * @throws CorruptIndexException
     */
    public void updateDataEntity(String strDataEntityId, String strDataEntityContentFingerprint, String strMasterDataEntityId) throws CorruptIndexException,
            IOException
    {

        Term termId = new Term(dataEntityId, strDataEntityId);


        Document doc = new Document();

        doc.add(new StringField(dataEntityId, strDataEntityId, Store.YES));
        doc.add(new StringField(dataEntityContentFingerprint, strDataEntityContentFingerprint, Store.YES));
        doc.add(new LongPoint(lastCrawledTime, System.currentTimeMillis()));
        doc.add(new StoredField(lastCrawledTime, System.currentTimeMillis()));
        if(!StringUtils.nullOrWhitespace(strMasterDataEntityId))
            doc.add(new StringField(masterDataEntityId, strMasterDataEntityId, Store.YES));


        m_indexWriter.updateDocument(termId, doc);

    }



    /**
     * Sets a data entities 'last crawled/checked time' entry to the current time. In the case this data entity is a master entity, all slave documents will be updated
     * also. You can set an entity as a master entity with {@link #addDataEntity(String, String, String)} or {@link #updateDataEntity(String, String, String)}
     * 
     * @param strDataEntityId the data entity which is finally checked/crawled
     * 
     * @throws IOException
     * @throws CorruptIndexException
     */
    public void updateDataEntityLastCrawledTime(String strDataEntityId) throws CorruptIndexException, IOException
    {

        Term termId = new Term(dataEntityId, strDataEntityId);

        refreshIndexReaderz();
        TopDocs topDocs = m_indexSearcher.search(new TermQuery(termId), 1);

        if(topDocs.totalHits == 0) throw new IllegalStateException("there has to be an data entry with Id " + strDataEntityId + " for updating. Nothing was found.");


        long lCurrentTime = System.currentTimeMillis();

        Document doc = m_indexReader.document(topDocs.scoreDocs[0].doc);

        doc.removeFields(lastCrawledTime);
        doc.add(new LongPoint(lastCrawledTime, lCurrentTime));
        doc.add(new StoredField(lastCrawledTime, lCurrentTime));

        m_indexWriter.updateDocument(termId, doc);




        // wenn das Teil eine MasterDataEntity ist, dann müssen alle assoziierten Sklaven auch noch aktualisiert werden

        termId = new Term(masterDataEntityId, strDataEntityId);

        topDocs = m_indexSearcher.search(new TermQuery(termId), Integer.MAX_VALUE);

        for( int i=0; i<topDocs.scoreDocs.length; i++)
        {

            Document slaveDoc = m_indexReader.document(topDocs.scoreDocs[i].doc);

            slaveDoc.removeFields(lastCrawledTime);
            slaveDoc.add(new LongPoint(lastCrawledTime, lCurrentTime));
            slaveDoc.add(new StoredField(lastCrawledTime, lCurrentTime));

            m_indexWriter.updateDocument(termId, doc);
        }

    }






}
