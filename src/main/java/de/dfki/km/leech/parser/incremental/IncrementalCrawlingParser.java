/*
    Leech - crawling capabilities for Apache Tika
    
    Copyright (C) 2012 DFKI GmbH, Author: Christian Reuschling

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

    Contact us by mail: christian.reuschling@dfki.de
*/

package de.dfki.km.leech.parser.incremental;



import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.UUID;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.DublinCore;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.CompositeParser;
import org.apache.tika.parser.EmptyParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.ParserDecorator;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import de.dfki.km.leech.Leech;
import de.dfki.km.leech.config.CrawlerContext;
import de.dfki.km.leech.parser.CrawlerParser;
import de.dfki.km.leech.parser.incremental.IncrementalCrawlingHistory.Exist;
import de.dfki.km.leech.util.TikaUtils;



/**
 * A Parser decorator which enables incremental indexing during the crawl. For this, {@link IncrementalCrawlingParser} needs two entries inside the
 * metadata given from the parse method:<br>
 * <br>
 * <li>{@link IncrementalCrawlingHistory}.dataEntityExistsID: an identifier for a data entity that is independent from the content of this entity. It
 * is only for identifying the occurence, not to check whether it has changed (e.g. a filename) <li>
 * <br>
 * {@link IncrementalCrawlingHistory}.dataEntityContentFingerprint: some fingerprint/identifier that gives the hint whether the content of the data
 * entity has changed, e.g. the modifed date of a file These entries depends on the type of the datasource, which will considered by creating the
 * InputStream for the parse method. Thus, both metadata entries will be performed in {@link TikaUtils} during stream creation.<br>
 * <br>
 * Dependent on these entries this decorator writes a data entity modification state (new, modified, unmodified, removed) into the metadata before
 * delegating to the wrapped parser. In the case of a cycle during a crawl (when a data entity comes a second time during a crawl), nothing will be
 * delegated.
 * 
 * 
 * 
 * @author Christian Reuschling, Dipl.Ing.(BA)
 */
public class IncrementalCrawlingParser extends ParserDecorator
{

    static public final String DATA_ENTITY_MODIFICATION_STATE = "dataEntitiyModificationState";

    static public final String MODIFIED = "modified";

    static public final String NEW = "new";

    static public final String REMOVED = "removed";

    static public final String ERROR = "error";

    private static final long serialVersionUID = 3823147926764040243L;

    static public final String UNMODIFIED = "unmodified";



    protected Leech m_leech = new Leech();



    public IncrementalCrawlingParser(Parser parser)
    {
        super(parser);
    };



    @Override
    public void parse(InputStream stream, ContentHandler handler, Metadata metadata, ParseContext context) throws IOException, SAXException,
            TikaException
    {

        // wir machen hier das ganze inkrementelle history-Zeugs, dann parsen wir des Teil wie gewohnt, die entsprechenden crawlerParser werten u.U.
        // auch noch die Einträge in den Metadaten aus. Am Schluß machen wir noch die history zu.


        IncrementalCrawlingHistory crawlingHistory = null;
        boolean bIsTmpHistory = false;
        int iCurrentCrawlingDepth = 0;

        try
        {
            CrawlerContext crawlerContext = context.get(CrawlerContext.class);
            if(crawlerContext == null) crawlerContext = new CrawlerContext();


            // die momentane crawlingdepth brauchen wir um festzustellen, wann ein kompletter crawlingVorgang abgeschlossen ist. Ein CrawlerParser
            // aktualisiert diese Info in der metadata
            String strDepth = metadata.get(CrawlerParser.CURRENT_CRAWLING_DEPTH);
            if(strDepth != null) iCurrentCrawlingDepth = Integer.valueOf(strDepth);



            // ## die crawling history
            crawlingHistory = crawlerContext.getIncrementalCrawlingHistory();
            if(crawlingHistory == null && crawlerContext.getDetectCycles() && iCurrentCrawlingDepth == 0)
            {
                // wir erstellen eine temporäre crawlerhistory, die am Schluß des Crawls auch wieder gelöscht wird
                File parentDir = File.createTempFile("Hitzli", "Putzli").getParentFile();
                File fTmpHistory = new File(parentDir.getAbsolutePath() + "/leechTmp/" + UUID.randomUUID().toString().replaceAll("\\W", "_"));
                fTmpHistory.mkdirs();

                crawlerContext.setIncrementalCrawlingHistoryPath(fTmpHistory.getAbsolutePath());
                crawlingHistory = crawlerContext.getIncrementalCrawlingHistory();

                bIsTmpHistory = true;
            }


            if(iCurrentCrawlingDepth == 0 && crawlingHistory != null) crawlingHistory.crawlStarted();




            // ## content and history


            boolean bProcessEntity = performHistoryStuff(crawlingHistory, metadata);

            if(bProcessEntity)
            {
                // wenn wir unmodified sind, dann wollen wir nur weitermachen, wenn es ein crawlerParser ist - evtl. sind verlinkte Inhalte ja auch
                // modified
                String strDataEntityModState = metadata.get(IncrementalCrawlingParser.DATA_ENTITY_MODIFICATION_STATE);
                MediaType type = m_leech.getDetector().detect(stream, metadata);
                Parser finalParser4Type = TikaUtils.getParser4Type((CompositeParser) getWrappedParser(), type, context);

                if(!IncrementalCrawlingParser.UNMODIFIED.equals(strDataEntityModState))
                {
                    getWrappedParser().parse(stream, handler, metadata, context);
                }
                else if(finalParser4Type instanceof CrawlerParser)
                {
                    getWrappedParser().parse(stream, handler, metadata, context);
                }
            }


            // removed entites and finishing stuff



            if(iCurrentCrawlingDepth != 0 || crawlingHistory == null) return;



            Iterator<String> itRemovedDataEntitiesIDs = crawlingHistory.crawlFinished();
            while (itRemovedDataEntitiesIDs.hasNext() && !crawlerContext.stopRequested() && crawlerContext.getCheckForRemovedEntities())
            {


                // wenn der Handler gleich geblieben ist, dann muß dieses Metadata Object das selbe wie u.U. beim übergebenen handler bleiben. Das ist
                // bei unserem DataSinkcontentHandler der Fall
                ContentHandler handler4RemovedData = TikaUtils.createContentHandler4SubCrawl(crawlerContext);
                // wir löschen die Inhalte im Metadata-Objekt, da wir zwar die Referenz behalten wollen (falls ein Handler das auch hat), aber die
                // Inhalte für das subObject neu gefüllt werden sollen.
                TikaUtils.clearMetadata(metadata);


                metadata.set(DATA_ENTITY_MODIFICATION_STATE, REMOVED);

                String strDataEntityId2Remove = itRemovedDataEntitiesIDs.next();
                metadata.set(IncrementalCrawlingHistory.dataEntityExistsID, strDataEntityId2Remove);

                InputStream dummyStream = new ByteArrayInputStream("leech sucks - hopefully :)".getBytes());

                EmptyParser.INSTANCE.parse(dummyStream, handler4RemovedData, metadata, context);
            }





        }
        catch (Exception e)
        {
            String strUrlOrSource = metadata.get(DublinCore.SOURCE);
            if(strUrlOrSource == null) strUrlOrSource = metadata.get(Metadata.RESOURCE_NAME_KEY);
            if(strUrlOrSource == null) strUrlOrSource = metadata.get(IncrementalCrawlingHistory.dataEntityExistsID);
            if(strUrlOrSource == null) strUrlOrSource = "no entity id known in metadata";

            if(e instanceof TikaException) throw (TikaException) e;
            throw new TikaException("Error while crawling " + strUrlOrSource, e);
        }
        finally
        {

            if(crawlingHistory != null && iCurrentCrawlingDepth == 0) crawlingHistory.closeLuceneStuff();

            if(crawlingHistory != null && iCurrentCrawlingDepth == 0 && bIsTmpHistory)
            {
                File fTmpHistory = new File(crawlingHistory.getHistoryPath());
                for (File fSubFile : fTmpHistory.listFiles())
                    fSubFile.delete();
                fTmpHistory.delete();
            }
        }



    }







    /**
     * Performs the entries into the incremental crawling history and put the data entity modification state into the metadata object. In the case
     * this data entity was processed during this crawl yet (when we have a cycle), the method will return false which means that it don't have to
     * processed again.
     * 
     * @param crawlingHistory the crawling history. Can be null, in this case the data entity will be flagged as NEW in any case
     * @param metadata the metadata of the data entity. The method will put the data entity modification state into
     * 
     * @return true: process the data entity (it was not processed formerly in this crawl), false otherwise (it was processed during this call, we have a circle)
     * 
     * @throws Exception
     */
    public static boolean performHistoryStuff(IncrementalCrawlingHistory crawlingHistory, Metadata metadata) throws Exception
    {

        if(crawlingHistory == null)
        {
            metadata.set(DATA_ENTITY_MODIFICATION_STATE, NEW);

            return true;
        }
        else
        {
            // wir wollen inkrementelles indexieren - war das Teil schon mal da?

            String strDataEntityExistsID = metadata.get(IncrementalCrawlingHistory.dataEntityExistsID);


            Exist exist = crawlingHistory.exists(strDataEntityExistsID);

            // wenn wir es in diesem Crawl schon mal prozessiert haben, dann machen wir gar nix - und verfolgen auch keine Links mehr
            // weiter. Dann haben wir einen Zykel.
            if(exist.equals(Exist.YES_PROCESSED))
            {
                return false;
            }


            String strDataEntityContentFingerprint = metadata.get(IncrementalCrawlingHistory.dataEntityContentFingerprint);

            if(exist.equals(Exist.NOT))
            {
                metadata.set(DATA_ENTITY_MODIFICATION_STATE, NEW);

                crawlingHistory.addDataEntity(strDataEntityExistsID, strDataEntityContentFingerprint);

                return true;
            }


            // es war schon mal da - hat es sich verändert?
            boolean bExistsWithContent = crawlingHistory.existsWithContent(strDataEntityExistsID, strDataEntityContentFingerprint);
            if(bExistsWithContent)
            {
                // nicht verändert - wir merken uns, daß es bei diesem crawl immer noch dabei war
                metadata.set(DATA_ENTITY_MODIFICATION_STATE, UNMODIFIED);

                crawlingHistory.updateDataEntityLastCrawledTime(strDataEntityExistsID);

                return true;
            }


            // verändert
            metadata.set(DATA_ENTITY_MODIFICATION_STATE, MODIFIED);

            crawlingHistory.updateDataEntity(strDataEntityExistsID, strDataEntityContentFingerprint);

            return true;
        }

    }







}
