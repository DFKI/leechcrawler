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
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixTermsEnum;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
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

    static protected List<String> terms(String strFieldName, String strPrefix, int iMaxTerms2Return, IndexReader reader) throws IOException, URISyntaxException
    {

        LinkedList<String> llFieldTerms = new LinkedList<String>();



        Terms terms = MultiFields.getTerms(reader, strFieldName);
        
        if(terms == null) return llFieldTerms;

        TermsEnum termsEnum = terms.iterator(null);

        if(!StringUtils.nullOrWhitespace(strPrefix)) termsEnum = new PrefixTermsEnum(termsEnum, new Term(strFieldName, strPrefix).bytes());

        while (termsEnum.next() != null)
        {
            String strTerm = termsEnum.term().utf8ToString();

            llFieldTerms.add(strTerm);

            if(llFieldTerms.size() >= iMaxTerms2Return) break;
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




    public void postprocessIndex(String strLuceneIndexPath, FieldConfig fieldConfig, String... straLuceneReadOnlyLookupPaths) throws Exception
    {

        // wir öffnen den einen Index lediglich lesend, erstellen alle n Einträge einen neuen Index, mergen die am Schluß zusammen und tauschen den
        // gegebenen aus

        if(StringUtils.nullOrWhitespace(m_strNewField4Buzzwords) && !m_bEstimatePageCounts)
            Logger.getLogger(IndexPostprocessor.class.getName()).warning("Will do nothing - nothing is enabled.");

        if(!StringUtils.nullOrWhitespace(m_strNewField4Buzzwords))
            Logger.getLogger(LuceneIndexCreator.class.getName()).info("Index postprocessing: Will create buzzwords");
        if(m_bEstimatePageCounts) Logger.getLogger(LuceneIndexCreator.class.getName()).info("Index postprocessing: Will calculate heuristic page counts");

        long lStart = System.currentTimeMillis();


        LinkedList<IndexReader> llsubReaders = new LinkedList<IndexReader>();
        IndexReader reader4SourceIndex = DirectoryReader.open(new SimpleFSDirectory(new File(strLuceneIndexPath)));
        IndexSearcher searcher4SourceIndex = new IndexSearcher(reader4SourceIndex);
        llsubReaders.add(reader4SourceIndex);
        for (String strLuceneReadOnlyLookupPath : straLuceneReadOnlyLookupPaths)
            llsubReaders.add(DirectoryReader.open(new SimpleFSDirectory(new File(strLuceneReadOnlyLookupPath))));


        IndexReader lookupReader;
        if(llsubReaders.size() > 1)
            lookupReader = new MultiReader(llsubReaders.toArray(new IndexReader[0]), true);
        else
            lookupReader = reader4SourceIndex;



        // wir machen uns einen leeren initialen Writer zum schreiben - den Rest macht der ToLuceneContentHandler
        File fLuceneIndex = new File(strLuceneIndexPath);
        String strTmpPath = fLuceneIndex.getAbsolutePath() + "_4PostProcessing";
        File fOurTmpDir = new File(strTmpPath);


        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_4_9, fieldConfig.createAnalyzer());
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
            
            TopDocs topDocs = searcher4SourceIndex.search(new TermQuery(new Term(LeechMetadata.id, strId)), 1);
            
            int iDocNo = topDocs.scoreDocs[0].doc;
            Document doc2modify = reader4SourceIndex.document(iDocNo);

            // long lStartLoop = StopWatch.stopAndPrintTime();
            if(!StringUtils.nullOrWhitespace(m_strNewField4Buzzwords))
                Buzzwords.addBuzzwords(iDocNo, doc2modify, m_strNewField4Buzzwords, sAttNames4BuzzwordCalculation, m_iMaxNumberOfBuzzwords, m_bSkipSimilarTerms,
                        lookupReader);
            // lStartLoop = StopWatch.stopAndPrintDistance(lStartLoop);
            if(m_bEstimatePageCounts) PageCountEstimator.addHeuristicDocPageCounts(iDocNo, doc2modify, reader4SourceIndex);

            toLuceneContentHandler.processNewDocument(doc2modify);

            if(i++ % 100000 == 0) Logger.getLogger(LuceneIndexCreator.class.getName()).info(i + " docs postprocessed");

        }


        toLuceneContentHandler.crawlFinished();
        firstTmpWriter.forceMerge(1, true);
        firstTmpWriter.close(true);
        if(lookupReader instanceof MultiReader)
            lookupReader.close();
        else
            reader4SourceIndex.close();


        // jetzt müssen wir den alten Index durch den neuen ersetzen
        File fBackup = new File(fLuceneIndex.getAbsolutePath() + "_bak");
        fLuceneIndex.renameTo(fBackup);
        fOurTmpDir.renameTo(fLuceneIndex);
        FileUtils.deleteDirectory(fBackup);



        Logger.getLogger(LuceneIndexCreator.class.getName()).info(
                "...postprocessing finished. Needed " + StopWatch.formatTimeDistance(System.currentTimeMillis() - lStart));



    }



}
