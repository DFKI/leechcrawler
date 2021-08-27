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

package de.dfki.km.leech.sax;



import de.dfki.inquisitor.collections.CollectionUtilz;
import de.dfki.inquisitor.collections.MultiValueTreeMap;
import de.dfki.inquisitor.processes.StopWatch;
import de.dfki.inquisitor.text.StringUtils;
import org.apache.tika.metadata.Metadata;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;



/**
 * A ContentHandler wrapper/decorator that counts the new, modified removed and error entities during a crawl. For new, modified and error entities she also counts the
 * according content types as detail information.<br>
 * <br>
 * Usage:<br>
 * <code>
 * CrawlerContext crawlerContext = new CrawlerContext().setIncrementalCrawlingHistoryPath("./history/forResourceDir");<br><br>
 * CrawlReportContentHandler reportContentHandler = new CrawlReportContentHandler(new PrintlnContentHandler(Granularity.titlePlusFulltext));<br>
 * leech.parse(new File("resource/testData_short"), reportContentHandler, crawlerContext.createParseContext());<br><br>
 * System.out.println(reportContentHandler.getReport());<br>
 * </code>
 * 
 * @author Christian Reuschling, Dipl.Ing.(BA)
 * 
 */
public class CrawlReportContentHandler extends DataSinkContentHandlerDecorator
{

    public static class CrawlReport
    {
        public HashMap<String, Integer> hsErrorType2EntityCount = new HashMap<String, Integer>();

        public HashMap<String, Integer> hsModifiedType2EntityCount = new HashMap<String, Integer>();

        public HashMap<String, Integer> hsNewType2EntityCount = new HashMap<String, Integer>();

        public int iErrorEntities = 0;

        public int iModifiedEntities = 0;

        public int iNewEntities = 0;

        public int iProcessedEntities = 0;

        public int iRemovedEntities = 0;

        public int iUnModifiedEntities = 0;

        public long lastModifiedEntityProcessingTime;

        public long lastNewEntityProcessingTime;

        public long lastRemovedEntityProcessingTime;

        public long lfirstEntityStartTime = -1;

        public long lLastEntityEndTime = -1;

        public long lModifiedEntitiesProcessingTime = 0;

        public long lNewEntitiesProcessingTime = 0;

        public long lRemovedEntitiesProcessingTime = 0;

        boolean bSomeHandled = false;



        @Override
        public String toString()
        {
            StringBuilder strbReport = new StringBuilder();

            strbReport.append("Report: ");
            if(lfirstEntityStartTime != -1)
                strbReport.append("First handled data entity at ").append(new SimpleDateFormat().format(new Date(lfirstEntityStartTime))).append(", ");
            int iEntities = iModifiedEntities + iNewEntities + iRemovedEntities + iErrorEntities;
            strbReport.append(StringUtils.beautifyNumber(iEntities)).append(" processed entities");
            if(lfirstEntityStartTime != -1)
            {
                long lDuration = lLastEntityEndTime - lfirstEntityStartTime;
                strbReport.append(", duration ").append(StopWatch.formatTimeDistance(lDuration));

                if(lDuration > 0)
                {
                    long lMilliSecondsPerEntity = Math.round((double) lDuration / (double) iEntities);
                    strbReport.append(", ").append(StopWatch.formatTimeDistance(lMilliSecondsPerEntity)).append("/entity");

                    double dEntitiesPerMilliSecond = (double) iEntities / (double) lDuration;
                    strbReport.append(", ").append(StringUtils.beautifyNumber(Math.round(dEntitiesPerMilliSecond * 1000))).append("/s");
                    strbReport.append(", ").append(StringUtils.beautifyNumber(Math.round(dEntitiesPerMilliSecond * 1000 * 60))).append("/m");
                    strbReport.append(", ").append(StringUtils.beautifyNumber(Math.round(dEntitiesPerMilliSecond * 1000 * 60 * 60))).append("/h");
                    strbReport.append(", ").append(StringUtils.beautifyNumber(Math.round(dEntitiesPerMilliSecond * 1000 * 60 * 60 * 24))).append("/d");
                }

            }



            strbReport.append("\n");


            strbReport.append("New data entities: ").append(StringUtils.beautifyNumber(iNewEntities));
            if(iNewEntities > 0)
                strbReport.append(" (in average ").append(StopWatch.formatTimeDistance(lNewEntitiesProcessingTime / iNewEntities))
                        .append(" to handle. Last entity took " + StopWatch.formatTimeDistance(lastNewEntityProcessingTime) + ")");
            strbReport.append("\n");

            MultiValueTreeMap<Integer, String> tmEntityCount2Type = new MultiValueTreeMap<>(Collections.reverseOrder(), LinkedList.class);
            for (Entry<String, Integer> newType2EntityCount : hsNewType2EntityCount.entrySet())
                tmEntityCount2Type.add(newType2EntityCount.getValue(), newType2EntityCount.getKey());

            StringBuilder strbTmp = new StringBuilder();
            for (Entry<Integer, String> entityCount2Type : tmEntityCount2Type.entryList())
                strbTmp.append(", ").append(entityCount2Type.getValue()).append(":").append(StringUtils.beautifyNumber(entityCount2Type.getKey()));
            strbTmp.replace(0, 1, "");
            strbReport.append(strbTmp);
            if(strbTmp.length() > 0) strbReport.append("\n");

            strbReport.append("Modified data entities: ").append(StringUtils.beautifyNumber(iModifiedEntities));
            if(iModifiedEntities > 0)
                strbReport.append(" (in average ").append(StopWatch.formatTimeDistance(lModifiedEntitiesProcessingTime / iModifiedEntities))
                        .append(" to handle. Last entity took " + StopWatch.formatTimeDistance(lastModifiedEntityProcessingTime) + ")");
            strbReport.append("\n");

            tmEntityCount2Type = new MultiValueTreeMap<>(Collections.reverseOrder(), LinkedList.class);
            for (Entry<String, Integer> modifiedType2EntityCount : hsModifiedType2EntityCount.entrySet())
                tmEntityCount2Type.add(modifiedType2EntityCount.getValue(), modifiedType2EntityCount.getKey());

            strbTmp = new StringBuilder();
            for (Entry<Integer, String> entityCount2Type : tmEntityCount2Type.entryList())
                strbTmp.append(", ").append(entityCount2Type.getValue()).append(":").append(StringUtils.beautifyNumber(entityCount2Type.getKey()));
            strbTmp.replace(0, 1, "");
            strbReport.append(strbTmp);
            if(strbTmp.length() > 0) strbReport.append("\n");

            strbReport.append("Removed data entities: ").append(StringUtils.beautifyNumber(iRemovedEntities));
            if(iRemovedEntities > 0)
                strbReport.append(" (in average ").append(StopWatch.formatTimeDistance(lRemovedEntitiesProcessingTime / iRemovedEntities))
                        .append(" to handle. Last entity took " + StopWatch.formatTimeDistance(lastRemovedEntityProcessingTime) + ")");
            strbReport.append("\n");

            strbReport.append("Unmodified data entities: ").append(StringUtils.beautifyNumber(iUnModifiedEntities));
            strbReport.append("\n");

            strbReport.append("Double data entities: ").append(StringUtils.beautifyNumber(iProcessedEntities));
            strbReport.append("\n");

            strbReport.append("Error data entities: ").append(StringUtils.beautifyNumber(iErrorEntities)).append("\n");

            tmEntityCount2Type = new MultiValueTreeMap<>(Collections.reverseOrder(), LinkedList.class);
            for (Entry<String, Integer> errorType2EntityCount : hsErrorType2EntityCount.entrySet())
                tmEntityCount2Type.add(errorType2EntityCount.getValue(), errorType2EntityCount.getKey());

            strbTmp = new StringBuilder();
            for (Entry<Integer, String> entityCount2Type : tmEntityCount2Type.entryList())
                strbTmp.append(", ").append(entityCount2Type.getValue()).append(":").append(StringUtils.beautifyNumber(entityCount2Type.getKey()));
            strbTmp.replace(0, 1, "");
            strbReport.append(strbTmp);
            if(strbTmp.length() > 0) strbReport.append("\n");

            return strbReport.toString();
        }
    }


    protected CrawlReport m_crawlReport = new CrawlReport();



    protected long m_lastReportTime = -1;



    protected long m_lCyclicReportMilliseconds = -1;



    public CrawlReportContentHandler(DataSinkContentHandler wrappedDataSinkContentHandler)
    {
        m_wrappedDataSinkContentHandler = wrappedDataSinkContentHandler;
    }



    @Override
    public void crawlFinished()
    {
        if(m_wrappedDataSinkContentHandler != null) m_wrappedDataSinkContentHandler.crawlFinished();

        LoggerFactory.getLogger(CrawlReportContentHandler.class.getName()).info("Crawl finished:\n" + getReport().toString());
    }



    public CrawlReport getReport()
    {
        return m_crawlReport;
    }





    @Override
    public void processErrorData(Metadata metadata)
    {
        if(m_crawlReport.lfirstEntityStartTime == -1 || m_crawlReport.bSomeHandled == false)
        {
            m_crawlReport.lfirstEntityStartTime = System.currentTimeMillis();
            m_crawlReport.bSomeHandled = true;
        }

        m_crawlReport.iErrorEntities++;

        String[] strTypes = metadata.getValues("Content-Type");
        if(strTypes == null || strTypes.length == 0) strTypes = CollectionUtilz.createArray("unknown");

        for (String strType : strTypes)
        {
            int iIndex = strType.indexOf(";");
            if(iIndex != -1) strType = strType.substring(0, iIndex);
            Integer iCount4Type = m_crawlReport.hsErrorType2EntityCount.get(strType);
            if(iCount4Type == null) iCount4Type = 0;
            iCount4Type++;

            m_crawlReport.hsErrorType2EntityCount.put(strType, iCount4Type);
        }


        if(m_wrappedDataSinkContentHandler != null) m_wrappedDataSinkContentHandler.processErrorData(metadata);


        m_crawlReport.lLastEntityEndTime = System.currentTimeMillis();

        printReportIfItsTime();
    }



    @Override
    public void processModifiedData(Metadata metadata, String strFulltext)
    {
        if(m_crawlReport.lfirstEntityStartTime == -1 || m_crawlReport.bSomeHandled == false)
        {
            m_crawlReport.lfirstEntityStartTime = System.currentTimeMillis();
            m_crawlReport.bSomeHandled = true;
        }

        m_crawlReport.iModifiedEntities++;

        String[] strTypes = metadata.getValues("Content-Type");
        if(strTypes == null || strTypes.length == 0) strTypes = CollectionUtilz.createArray("unknown");

        for (String strType : strTypes)
        {
            int iIndex = strType.indexOf(";");
            if(iIndex != -1) strType = strType.substring(0, iIndex);
            Integer iCount4Type = m_crawlReport.hsModifiedType2EntityCount.get(strType);
            if(iCount4Type == null) iCount4Type = 0;
            iCount4Type++;

            m_crawlReport.hsModifiedType2EntityCount.put(strType, iCount4Type);
        }


        long lStart = System.currentTimeMillis();

        if(m_wrappedDataSinkContentHandler != null) m_wrappedDataSinkContentHandler.processModifiedData(metadata, strFulltext);

        long lDuration = System.currentTimeMillis() - lStart;
        m_crawlReport.lModifiedEntitiesProcessingTime += lDuration;
        m_crawlReport.lastModifiedEntityProcessingTime = lDuration;
        m_crawlReport.lLastEntityEndTime = System.currentTimeMillis();

        printReportIfItsTime();
    }



    @Override
    public void processNewData(Metadata metadata, String strFulltext)
    {
        if(m_crawlReport.lfirstEntityStartTime == -1 || m_crawlReport.bSomeHandled == false)
        {
            m_crawlReport.lfirstEntityStartTime = System.currentTimeMillis();
            m_crawlReport.bSomeHandled = true;
        }

        m_crawlReport.iNewEntities++;

        String[] strTypes = metadata.getValues("Content-Type");
        if(strTypes == null || strTypes.length == 0) strTypes = CollectionUtilz.createArray("unknown");

        for (String strType : strTypes)
        {
            int iIndex = strType.indexOf(";");
            if(iIndex != -1) strType = strType.substring(0, iIndex);
            Integer iCount4Type = m_crawlReport.hsNewType2EntityCount.get(strType);
            if(iCount4Type == null) iCount4Type = 0;
            iCount4Type++;

            m_crawlReport.hsNewType2EntityCount.put(strType, iCount4Type);
        }


        long lStart = System.currentTimeMillis();

        if(m_wrappedDataSinkContentHandler != null) m_wrappedDataSinkContentHandler.processNewData(metadata, strFulltext);

        long lDuration = System.currentTimeMillis() - lStart;
        m_crawlReport.lNewEntitiesProcessingTime += lDuration;
        m_crawlReport.lastNewEntityProcessingTime = lDuration;
        m_crawlReport.lLastEntityEndTime = System.currentTimeMillis();

        printReportIfItsTime();
    }



    @Override
    public void processProcessedData(Metadata metadata)
    {
        m_crawlReport.iProcessedEntities++;

        if(m_wrappedDataSinkContentHandler != null) m_wrappedDataSinkContentHandler.processProcessedData(metadata);

        printReportIfItsTime();
    }



    @Override
    public void processRemovedData(Metadata metadata)
    {
        if(m_crawlReport.lfirstEntityStartTime == -1 || m_crawlReport.bSomeHandled == false)
        {
            m_crawlReport.lfirstEntityStartTime = System.currentTimeMillis();
            m_crawlReport.bSomeHandled = true;
        }

        m_crawlReport.iRemovedEntities++;

        long lStart = System.currentTimeMillis();

        if(m_wrappedDataSinkContentHandler != null) m_wrappedDataSinkContentHandler.processRemovedData(metadata);

        long lDuration = System.currentTimeMillis() - lStart;
        m_crawlReport.lRemovedEntitiesProcessingTime += lDuration;
        m_crawlReport.lastRemovedEntityProcessingTime = lDuration;
        m_crawlReport.lLastEntityEndTime = System.currentTimeMillis();

        printReportIfItsTime();
    }



    @Override
    public void processUnmodifiedData(Metadata metadata)
    {
        m_crawlReport.iUnModifiedEntities++;

        if(m_wrappedDataSinkContentHandler != null) m_wrappedDataSinkContentHandler.processUnmodifiedData(metadata);

        printReportIfItsTime();
    }



    /**
     * Sets everything counted yet to zero. Then you can use this object for another crawl
     */
    public void reset()
    {
        m_crawlReport = new CrawlReport();
    }



    /**
     * Sets whether or not a time-based cyclic report will be generated. The time is not a hard criteria, the method will print the report when new content arrives and
     * the last report was longer ago than everMilliseconds
     * 
     * @param everyMilliseconds in the case this value is <0, cyclic report printing will be disabled. Otherwise, every <everyMilliseconds> milliseconds a report will be
     *            println'd
     * 
     * @return this
     */
    public CrawlReportContentHandler setCyclicReportPrintln(long everyMilliseconds)
    {
        m_lCyclicReportMilliseconds = everyMilliseconds;

        return this;
    }



    protected void printReportIfItsTime()
    {
        if(m_lCyclicReportMilliseconds < 0) return;

        if(m_lastReportTime < 0)
        {
            m_lastReportTime = System.currentTimeMillis();
            return;
        }

        if(System.currentTimeMillis() >= m_lastReportTime + m_lCyclicReportMilliseconds)
        {
            LoggerFactory.getLogger(CrawlReportContentHandler.class.getName()).info(m_crawlReport.toString());

            m_lastReportTime = System.currentTimeMillis();
        }
    }




}
