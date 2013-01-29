package de.dfki.km.leech.util;



import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;
import org.apache.tika.metadata.Metadata;

import de.dfki.km.leech.lucene.Buzzwords;
import de.dfki.km.leech.lucene.FieldConfig;
import de.dfki.km.leech.lucene.PageCountEstimator;
import de.dfki.km.leech.lucene.ToLuceneContentHandler;
import de.dfki.km.leech.metadata.LeechMetadata;



public class IndexPostprocessor
{

    static protected List<String> terms(String strFieldName, String strPrefix, int iMaxTerms2Return, IndexReader reader) throws IOException,
            URISyntaxException
    {

        LinkedList<String> llFieldTerms = new LinkedList<String>();

        TermEnum termEnum = null;
        try
        {

            termEnum = reader.terms(new Term(strFieldName, strPrefix));

            for (int i = 0; i < iMaxTerms2Return; i++)
            {
                if(termEnum.term() == null) break;
                if(!termEnum.term().field().equals(strFieldName)) break;

                String strFieldTerm = termEnum.term().text();
                if(!strFieldTerm.startsWith(strPrefix)) break;


                llFieldTerms.add(strFieldTerm);

                if(termEnum.next() == false) break;
            }


        }
        finally
        {
            if(termEnum != null) termEnum.close();
        }


        return llFieldTerms;
    }


    protected boolean m_bEstimatePageCounts = false;

    protected boolean m_bSkipSimilarTerms;

    protected int m_iMaxNumberOfBuzzwords;

    protected String m_strNewField4Buzzwords;



    /**
     * Enables the Buzzword creation by setting the related configuration parameters.
     * 
     * @param strNewField4Buzzwords
     * @param sAttNames4BuzzwordCalculation
     * @param iMaxNumberOfBuzzwords
     * @param bSkipSimilarTerms
     */
    public void enableBuzzwordGeneration(String strNewField4Buzzwords, int iMaxNumberOfBuzzwords, boolean bSkipSimilarTerms)
    {
        this.m_strNewField4Buzzwords = strNewField4Buzzwords;
        this.m_iMaxNumberOfBuzzwords = iMaxNumberOfBuzzwords;
        this.m_bSkipSimilarTerms = bSkipSimilarTerms;
    }





    /**
     * Enables to add a page count attribute to a document in the case no one is there. The method estimates the page cont (i.e. 400 terms => 1 page).
     */
    public void enablePageCountEstimation()
    {
        m_bEstimatePageCounts = true;
    }




    public void postprocessIndex(String strLuceneIndexPath, FieldConfig fieldConfig) throws Exception
    {

        // wir öffnen den einen Index lediglich lesend, erstellen alle n Einträge einen neuen Index, mergen die am Schluß zusammen und tauschen den
        // gegebenen aus

        if(StringUtils.nullOrWhitespace(m_strNewField4Buzzwords) && !m_bEstimatePageCounts)
            Logger.getLogger(IndexPostprocessor.class.getName()).warning("Will do nothing - nothing is enabled.");

        if(!StringUtils.nullOrWhitespace(m_strNewField4Buzzwords))
            Logger.getLogger(LuceneIndexCreator.class.getName()).info("Index postprocessing: Will create buzzwords");
        if(m_bEstimatePageCounts)
            Logger.getLogger(LuceneIndexCreator.class.getName()).info("Index postprocessing: Will calculate heuristic page counts");

        long lStart = System.currentTimeMillis();


        IndexReader reader4SourceIndex = IndexReader.open(new SimpleFSDirectory(new File(strLuceneIndexPath)));


        // wir machen uns einen leeren initialen Writer zum schreiben - den Rest macht der ToLuceneContentHandler
        File fLuceneIndex = new File(strLuceneIndexPath);
        String strTmpPath = fLuceneIndex.getAbsolutePath() + "_4PostProcessing";
        File fOurTmpDir = new File(strTmpPath);


        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_CURRENT, fieldConfig.createAnalyzer());
        config.setOpenMode(OpenMode.CREATE);

        IndexWriter firstTmpWriter = new IndexWriter(new SimpleFSDirectory(fOurTmpDir), config);

        ToLuceneContentHandler toLuceneContentHandler = new ToLuceneContentHandler(fieldConfig, firstTmpWriter);


        Logger.getLogger(LuceneIndexCreator.class.getName()).info("Will get the doc ids...");
        List<String> llAllIds = terms(LeechMetadata.id, "", Integer.MAX_VALUE, reader4SourceIndex);
        Logger.getLogger(LuceneIndexCreator.class.getName()).info("...finished");


        Set<String> sAttNames4BuzzwordCalculation = new HashSet<String>();

        sAttNames4BuzzwordCalculation.add(LeechMetadata.body);
        sAttNames4BuzzwordCalculation.add(Metadata.TITLE);


        int i = 0;
        for (String strId : llAllIds)
        {
            TermDocs termDocs = reader4SourceIndex.termDocs(new Term(LeechMetadata.id, strId));
            termDocs.next();
            int iDocNo = termDocs.doc();
            Document doc2modify = reader4SourceIndex.document(iDocNo);

            // long lStartLoop = StopWatch.stopAndPrintTime();
            if(!StringUtils.nullOrWhitespace(m_strNewField4Buzzwords))
                Buzzwords.addBuzzwords(iDocNo, doc2modify, m_strNewField4Buzzwords, sAttNames4BuzzwordCalculation, m_iMaxNumberOfBuzzwords,
                        m_bSkipSimilarTerms, reader4SourceIndex);
            // lStartLoop = StopWatch.stopAndPrintDistance(lStartLoop);
            if(m_bEstimatePageCounts) PageCountEstimator.addHeuristicDocPageCounts(iDocNo, doc2modify, reader4SourceIndex);

            toLuceneContentHandler.processNewDocument(doc2modify);

            if(i++ % 100000 == 0) Logger.getLogger(LuceneIndexCreator.class.getName()).info(i + " docs postprocessed");

        }


        toLuceneContentHandler.crawlFinished();
        firstTmpWriter.forceMerge(1, true);
        firstTmpWriter.close(true);
        reader4SourceIndex.close();


        // jetzt müssen wir den alten Index durch den neuen ersetzen
        File fBackup = new File(fLuceneIndex.getAbsolutePath() + "_bak");
        fLuceneIndex.renameTo(fBackup);
        fOurTmpDir.renameTo(fLuceneIndex);
        // TODO einschalten wenn alles funzt
        // FileUtils.deleteDirectory(fBackup);



        Logger.getLogger(LuceneIndexCreator.class.getName()).info(
                "...postprocessing finished. Needed " + StopWatch.formatTimeDistance(System.currentTimeMillis() - lStart));



    }



}
