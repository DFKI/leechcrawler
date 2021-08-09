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


import de.dfki.inquisitor.collections.MultiValueBalancedTreeMap;
import de.dfki.inquisitor.text.StringUtils;
import de.dfki.km.leech.config.CrawlerContext;

import java.io.IOException;
import java.util.*;


/**
 * A persistent history database, remarking everything that was processed during a crawl. This history makes it possible to fulfill incremental crawling, where you can
 * quickly check whether a data entity found during the crawl is new or modified with respect to the last crawl. Further, all data entities that were removed since the
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
@SuppressWarnings("JavaDoc")
public class IncrementalCrawlingHistory
{


    static public final String dataEntityContentFingerprint = "dataEntityContentFingerprint";
    static public final String dataEntityId = "dataEntityId";
    static public final String masterDataEntityId = "masterDataEntityId";
    protected final String m_strHistoryPath;
    protected Long m_lCrawlStartingTime = null;
    Map<String, DataEntityHistoryEntry> m_hsDataEntityId2HistoryEntry;
    MultiValueBalancedTreeMap<String, String> m_hsMasterDataEntityId2DataEntityIds;
    Set<String> m_sDataEntityIdsNotProcessed;

    public IncrementalCrawlingHistory(String strHistoryPath)
    {
        m_strHistoryPath = strHistoryPath + "/mapDB";


        Runtime.getRuntime().addShutdownHook(new Thread("IncrementalCrawlingHistory shutdown hook for " + strHistoryPath)
        {
            @Override
            public void run()
            {
                closeDBStuff();
            }
        });
    }

    /**
     * Remarks a new data entity, together with the current time as 'last crawled/checked time'.
     *
     * @param strDataEntityId                 an identifier for a data entity that is independent from the content of this entity. It is only for identifying the occurence, not to
     *                                        check whether it has changed (e.g. a filename)
     * @param strDataEntityContentFingerprint some fingerprint/identifier that gives the hint whether the content of the data entity has changed, e.g. the modifed date of
     *                                        a file
     */
    public void addDataEntity(String strDataEntityId, String strDataEntityContentFingerprint)
    {
        addDataEntity(strDataEntityId, strDataEntityContentFingerprint, null);
    }

    /**
     * Remarks a new data entity, together with the current time as 'last crawled/checked time'.
     *
     * @param strDataEntityId                 an identifier for a data entity that is independent from the content of this entity. It is only for identifying the occurence, not to
     *                                        check whether it has changed (e.g. a filename)
     * @param strDataEntityContentFingerprint some fingerprint/identifier that gives the hint whether the content of the data entity has changed, e.g. the modifed date of
     *                                        a file
     * @param strMasterDataEntityId           optional: an EntityId of another data entity that is our 'master' which means that when the master is updated with
     *                                        {@link #updateDataEntityLastCrawledTime(String)}, all associated slaves will be also updated. This is e.g. for the case when you
     *                                        are in a second run for RSS-File indexing, and leech recognizes that this file didn't changed. Now we don't want to go unnecessarily into the file and mark
     *                                        each entry on it's own. We know no subentry has changed, and can immediately mark them as processed with {@link #updateDataEntityLastCrawledTime(String)} on the master
     *                                        dataEntityId, which is the one from the RSS file. Leave it null or empty in the case you don't need to use it.
     */
    public void addDataEntity(String strDataEntityId, String strDataEntityContentFingerprint, String strMasterDataEntityId)
    {

        DataEntityHistoryEntry historyEntry = new DataEntityHistoryEntry(strDataEntityId, strDataEntityContentFingerprint, strMasterDataEntityId, System.currentTimeMillis());

        m_hsDataEntityId2HistoryEntry.put(strDataEntityId, historyEntry);

        if (StringUtils.notNullOrWhitespace(strMasterDataEntityId))
            m_hsMasterDataEntityId2DataEntityIds.add(strMasterDataEntityId, strDataEntityId);

        m_sDataEntityIdsNotProcessed.remove(strDataEntityId);
    }

    public void closeDBStuff()
    {

        if (m_sDataEntityIdsNotProcessed != null)
            m_sDataEntityIdsNotProcessed.clear();

        if (m_hsMasterDataEntityId2DataEntityIds != null)
        {
            m_hsMasterDataEntityId2DataEntityIds.getInternalMapDB().close();

            m_hsMasterDataEntityId2DataEntityIds = null;
            m_hsDataEntityId2HistoryEntry = null;
            m_sDataEntityIdsNotProcessed = null;
        }
    }

    /**
     * Returns all DataEntityIds with a 'last crawled/checked time' before the 'crawl starting time' as outdated data entities. These are all entities that didn't
     * exist in this crawl anymore, and thus can be considered as removed.<br>
     * You can only invoke and walk to the iterator once - the outdated entries inside the history will be deleted. In the case you invoke this method
     * twice, the second invocation will result into an empty list. This is to ensure that also huge deleted entity lists can be handled without problematic memory
     * consumption.<br>
     * Remark: The database instance for the underlying MapDB database will be closed when you walk the iterator to the end. All data will be committed before.
     *
     * @return all DataEntityIds with a 'last crawled/checked time' before the 'crawl starting time', thus all entities that can be considered as removed.
     */
    public Iterator<String> crawlFinished()
    {
        //ich lösche schon mal die nicht mehr benötigten aus der History, damit wir ein sauberes System haben
        for (String strRemovedId : m_sDataEntityIdsNotProcessed)
            m_hsDataEntityId2HistoryEntry.remove(strRemovedId);

        return new CrawlFinishedIterator();
    }

    /**
     * Informs the history that a new crawl has started. The history will save the current time as 'crawl starting time'. <br>
     * Remark: The database instance for the underlying MapDB database will be opened if necessary
     */
    public void crawlStarted()
    {

        openDBStuff();


        // wir merken uns die aktuelle crawlStartingTime - diese wird gebraucht um zu ermitteln ob ein item schon in diesem Lauf processed wurde (Zykel)
        m_lCrawlStartingTime = System.currentTimeMillis();

        // wir schreiben initial alle dataEntityIds aus der History in unser set - die werden dann im Laufe des crawls wieder ausgetragen, übrig bleiben die gelöschten
        m_sDataEntityIdsNotProcessed.addAll(m_hsDataEntityId2HistoryEntry.keySet());
    }

    /**
     * Checks whether an ID exists inside the incremental crawling history or not. During the crawl, this is to identify quickly whether a data entity is completely new
     * or not.
     *
     * @param strDataEntityId an identifier for a data entity that is independent from the content of this entity. It is only for identifying the occurence, not to
     *                        check whether it has changed (e.g. a filename)
     * @return There are three states: Exist.NOT says that the data entity has no entry inside the history at all. Exist.YES_UNPROCESSED means that the entity has an
     * entry inside the history, and that it still wasn't processed during the current crawl. Exist.YES_PROCESSED means that there is an entry but the data entity
     * was processed in this run yet, so normally another processing is unnecessary. This is to detect cycles.
     * @throws IOException
     */
    public Exist exists(String strDataEntityId) throws IOException
    {

        Long lDataEntityLastCrawledTime = getDataEntityLastCrawledTime(strDataEntityId);


        if (lDataEntityLastCrawledTime == null)
            return Exist.NOT;

        if (lDataEntityLastCrawledTime >= m_lCrawlStartingTime)
            return Exist.YES_PROCESSED;

        return Exist.YES_UNPROCESSED;
    }

    /**
     * Checks whether an ID with a specific content fingerprint exists in the crawling history or not. During the crawl, this is to identify quickly whether a data entity
     * has changed its content or not. Of course, this makes only sense in the case the content fingerprint that gives the hint whether the entity has changed can be
     * created quickly, at best without extracting the content. Such a fingerprint can be e.g. a modified date of a file, or the time attribute of an email.
     *
     * @param strDataEntityId                 an identifier for a data entity that is independent from the content of this entity. It is only for identifying the occurence, not to
     *                                        check whether it has changed (e.g. a filename)
     * @param strDataEntityContentFingerprint some fingerprint/identifier that gives the hint whether the content of the data entity has changed, e.g. the modifed date of
     *                                        a file
     * @return true in the case this identifier exists with exact this content fingerprint inside the crawling history
     */
    @SuppressWarnings("RedundantIfStatement")
    public boolean existsWithContent(String strDataEntityId, String strDataEntityContentFingerprint)
    {
        if (StringUtils.nullOrWhitespace(strDataEntityId))
            return false;


        DataEntityHistoryEntry historyEntry = m_hsDataEntityId2HistoryEntry.get(strDataEntityId);
        if (historyEntry == null)
            return false;

        if (historyEntry.dataEntityContentFingerprint.equals(strDataEntityContentFingerprint))
            return true;

        return false;
    }

    /**
     * Gets the stored content fingerprint for a given data entity entry.
     *
     * @param strDataEntityId an identifier for a data entity that is independent from the content of this entity. It is only for identifying the occurence, not to
     *                        check whether it has changed (e.g. a filename)
     * @return the according content fingerprint stored for this data entity, null in the case this data entity was not found
     */
    public String getDataEntityContentFingerprint(String strDataEntityId)
    {
        if (StringUtils.nullOrWhitespace(strDataEntityId))
            return null;

        DataEntityHistoryEntry historyEntry = m_hsDataEntityId2HistoryEntry.get(strDataEntityId);
        if (historyEntry == null)
            return null;

        return historyEntry.dataEntityContentFingerprint;
    }

    /**
     * Gets the stored last crawled time for a given data entity entry. This can be used to e.g. determine whether a data entity was already processed during the current
     * crawl or not. If it was processed already, this is a hint for a cycle.
     *
     * @param strDataEntityId an identifier for a data entity that is independent from the content of this entity. It is only for identifying the occurence, not to
     *                        check whether it has changed (e.g. a filename)
     * @return the according last crawled time stored for this data entity, null in the case this data entity was not found
     */
    public Long getDataEntityLastCrawledTime(String strDataEntityId)
    {
        if (StringUtils.nullOrWhitespace(strDataEntityId))
            return null;


        DataEntityHistoryEntry historyEntry = m_hsDataEntityId2HistoryEntry.get(strDataEntityId);
        if (historyEntry == null)
            return null;

        return historyEntry.lastCrawledTime;
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
     */
    @SuppressWarnings("unchecked")
    public void openDBStuff()
    {

        if (m_hsMasterDataEntityId2DataEntityIds == null)
        {
            m_hsMasterDataEntityId2DataEntityIds = new MultiValueBalancedTreeMap<>(m_strHistoryPath, HashSet.class);

            m_hsDataEntityId2HistoryEntry =
                    (Map<String, DataEntityHistoryEntry>) m_hsMasterDataEntityId2DataEntityIds.getInternalMapDB().hashMap("dataEntityId2HistoryEntry").createOrOpen();

            m_sDataEntityIdsNotProcessed = (Set<String>) m_hsMasterDataEntityId2DataEntityIds.getInternalMapDB().hashSet("dataEntityIdsNotProcessed").createOrOpen();
            m_sDataEntityIdsNotProcessed.clear();
        }
    }

    /**
     * Updates a whole data entity - same as addDataEntity, but removes a former entry before storing the new one
     *
     * @param strDataEntityId                 an identifier for a data entity that is independent from the content of this entity. It is only for identifying the occurence, not to
     *                                        check whether it has changed (e.g. a filename)
     * @param strDataEntityContentFingerprint some fingerprint/identifier that gives the hint whether the content of the data entity has changed, e.g. the modifed date of
     *                                        a file
     */
    public void updateDataEntity(String strDataEntityId, String strDataEntityContentFingerprint)
    {
        updateDataEntity(strDataEntityId, strDataEntityContentFingerprint, null);
    }

    /**
     * Updates a whole data entity - same as addDataEntity, but removes a former entry before storing the new one
     *
     * @param strDataEntityId                 an identifier for a data entity that is independent from the content of this entity. It is only for identifying the occurence, not to
     *                                        check whether it has changed (e.g. a filename)
     * @param strDataEntityContentFingerprint some fingerprint/identifier that gives the hint whether the content of the data entity has changed, e.g. the modifed date of
     *                                        a file
     * @param strMasterDataEntityId           optional: an EntityId of another data entity that is our 'master' which means that when the master is updated with
     *                                        {@link #updateDataEntityLastCrawledTime(String)}, all associated slaves will be also updated. This is e.g. for the case when you
     *                                        are in a second run for
     *                                        RSS-File indexing, and leech recognizes that this file didn't changed. Now we don't want to go unnecessarily into the fil and mark
     *                                        each entry on it's
     *                                        own. We know no subentry has changed, and can immediately mark them as processed with
     *                                        {@link #updateDataEntityLastCrawledTime(String)} on the master
     *                                        dataEntityId, which is the one from the RSS file. Leave it null or empty in the case you don't need to use it.
     */
    public void updateDataEntity(String strDataEntityId, String strDataEntityContentFingerprint, String strMasterDataEntityId)
    {

        DataEntityHistoryEntry historyEntry = new DataEntityHistoryEntry(strDataEntityId, strDataEntityContentFingerprint, strMasterDataEntityId, System.currentTimeMillis());

        m_hsDataEntityId2HistoryEntry.put(strDataEntityId, historyEntry);

        if (StringUtils.notNullOrWhitespace(strMasterDataEntityId))
            m_hsMasterDataEntityId2DataEntityIds.add(strMasterDataEntityId, strDataEntityId);

        m_sDataEntityIdsNotProcessed.remove(strDataEntityId);
    }

    /**
     * Sets a data entities 'last crawled/checked time' entry to the current time. In the case this data entity is a master entity, all slave documents will be updated
     * also. You can set an entity as a master entity with {@link #addDataEntity(String, String, String)} or {@link #updateDataEntity(String, String, String)}
     *
     * @param strDataEntityId the data entity which is finally checked/crawled
     */
    public void updateDataEntityLastCrawledTime(String strDataEntityId)
    {

        long lCurrentTime = System.currentTimeMillis();

        DataEntityHistoryEntry historyEntryOld = m_hsDataEntityId2HistoryEntry.get(strDataEntityId);

        if (historyEntryOld == null)
            throw new IllegalStateException("there has to be an data entry with Id " + strDataEntityId + " for updating. Nothing was found.");

        DataEntityHistoryEntry historyEntryNew =
                new DataEntityHistoryEntry(strDataEntityId, historyEntryOld.dataEntityContentFingerprint, historyEntryOld.masterDataEntityId, lCurrentTime);

        m_hsDataEntityId2HistoryEntry.put(strDataEntityId, historyEntryNew);

        m_sDataEntityIdsNotProcessed.remove(strDataEntityId);


        // wenn das Teil eine MasterDataEntity ist, dann müssen alle assoziierten Sklaven auch noch aktualisiert werden


        Collection<String> sSlaveIds = m_hsMasterDataEntityId2DataEntityIds.get(strDataEntityId);
        for (String strSlaveId : sSlaveIds)
        {
            DataEntityHistoryEntry historyEntryOldSlave = m_hsDataEntityId2HistoryEntry.get(strSlaveId);

            if (historyEntryOldSlave == null)
                continue;

            DataEntityHistoryEntry historyEntryNewSlave =
                    new DataEntityHistoryEntry(strSlaveId, historyEntryOldSlave.dataEntityContentFingerprint, historyEntryOldSlave.masterDataEntityId, lCurrentTime);

            m_hsDataEntityId2HistoryEntry.put(strSlaveId, historyEntryNewSlave);

            m_sDataEntityIdsNotProcessed.remove(strSlaveId);
        }
    }


    /**
     * Defines the states whether a data entity is in the history or not. There are three states: Exist.NOT says that the data entity has no entry inside the history at
     * all. Exist.YES_UNPROCESSED means that the entity has an entry inside the history, and that it still wasn't processed during the current crawl. Exist.YES_PROCESSED
     * means that there is an entry but the data entity was processed in this run yet, so normally another processing is unnecessary. This is to detect cycles.
     *
     * @author Christian Reuschling, Dipl.Ing.(BA)
     */
    public enum Exist
    {
        NOT, YES_PROCESSED, YES_UNPROCESSED
    }

    protected class CrawlFinishedIterator implements Iterator<String>
    {

        Iterator<String> m_itDataEntityIdsNotProcessed;


        protected CrawlFinishedIterator()
        {

            if (m_sDataEntityIdsNotProcessed == null)
                m_itDataEntityIdsNotProcessed = Collections.emptyIterator();
            else
                m_itDataEntityIdsNotProcessed = m_sDataEntityIdsNotProcessed.iterator();
        }


        @Override
        public boolean hasNext()
        {


            boolean bHasNext = m_itDataEntityIdsNotProcessed.hasNext();

            // wenn wir nichts mehr haben, machen wir die unterliegene DB zu - der crawl ist final beendet
            if (!bHasNext)
                closeDBStuff();

            return bHasNext;
        }


        @Override
        public String next()
        {
            return m_itDataEntityIdsNotProcessed.next();
        }


        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }
}
