package de.dfki.km.leech.lucene;



import de.dfki.inquisitor.file.FileUtilz;
import de.dfki.inquisitor.processes.StopWatch;
import de.dfki.inquisitor.text.StringUtils;
import de.dfki.km.leech.lucene.basic.*;
import de.dfki.km.leech.metadata.LeechMetadata;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.*;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.automaton.CompiledAutomaton;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.PagedText;
import org.apache.tika.metadata.TikaCoreProperties;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;



public class IndexPostprocessor
{

    static protected List<String> terms(String strFieldName, String strPrefix, int iMaxTerms2Return, IndexReader reader) throws IOException, URISyntaxException
    {

        LinkedList<String> llFieldTerms = new LinkedList<String>();



        Terms terms = MultiFields.getTerms(reader, strFieldName);

        if(terms == null) return llFieldTerms;

        TermsEnum termsEnum = terms.iterator();

        if(!StringUtils.nullOrWhitespace(strPrefix)) {
            termsEnum = new AutomatonTermsEnum(termsEnum, new CompiledAutomaton(PrefixQuery.toAutomaton(new Term(strFieldName, strPrefix).bytes())));
        }

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

    protected String m_strNewField4FrqClass;

    protected Metadata m_staticAttributes2values = new Metadata();



    /**
     * Enables the Buzzword creation by setting the related configuration parameters.
     */
    public void enableBuzzwordGeneration(String strNewField4Buzzwords, int iMaxNumberOfBuzzwords, boolean bSkipSimilarTerms)
    {
        this.m_strNewField4Buzzwords = strNewField4Buzzwords;
        this.m_iMaxNumberOfBuzzwords = iMaxNumberOfBuzzwords;
        this.m_bSkipSimilarTerms = bSkipSimilarTerms;
    }




    /**
     * Enables to add a frequency class attribute to the documents. This is a measure how 'generalized' a document is in its topics.
     * 
     * @param strNewField4FrqClass
     */
    public void enableFrequencyClassCalculation(String strNewField4FrqClass)
    {
        m_strNewField4FrqClass = strNewField4FrqClass;
    }



    /**
     * Enables to add a page count attribute to a document in the case no one is there. The method estimates the page cont (i.e. 400 terms => 1 page).
     */
    public void enablePageCountEstimation()
    {
        m_bEstimatePageCounts = true;
    }



    /**
     * Enables to add static attribute value pairs to each document. Thus, you can e.g. mark a specific crawl with a category attribute, etc.
     * 
     * @param attributes2values the attribute value pairs that should be simply added to each document
     */
    public void enableStaticAttributeValuePairs(Metadata attributes2values)
    {
        m_staticAttributes2values = attributes2values;
    }





    public void postprocessIndex(String strLuceneIndexPath, FieldConfig fieldConfig, String... straLuceneReadOnlyLookupPaths) throws Exception
    {

        // wir öffnen den einen Index lediglich lesend, erstellen alle n Einträge einen neuen Index, mergen die am Schluß zusammen und tauschen den
        // gegebenen aus

        if(StringUtils.nullOrWhitespace(m_strNewField4Buzzwords) && !m_bEstimatePageCounts)
            LoggerFactory.getLogger(IndexPostprocessor.class.getName()).warn("Will do nothing - nothing is enabled.");

        if(!StringUtils.nullOrWhitespace(m_strNewField4Buzzwords))
            LoggerFactory.getLogger(LuceneIndexCreator.class.getName()).info("Index postprocessing: Will create buzzwords");
        if(m_bEstimatePageCounts) LoggerFactory.getLogger(LuceneIndexCreator.class.getName()).info("Index postprocessing: Will calculate heuristic page counts");

        if(!StringUtils.nullOrWhitespace(m_strNewField4FrqClass))
            LoggerFactory.getLogger(LuceneIndexCreator.class.getName()).info("Index postprocessing: Will calculate document frequency classes");

        long lStart = System.currentTimeMillis();


        LinkedList<IndexReader> llsubReaders = new LinkedList<IndexReader>();
        IndexReader reader4SourceIndex = DirectoryReader.open(new SimpleFSDirectory(Paths.get(strLuceneIndexPath)));
        IndexSearcher searcher4SourceIndex = new IndexSearcher(reader4SourceIndex);
        llsubReaders.add(reader4SourceIndex);
        for (String strLuceneReadOnlyLookupPath : straLuceneReadOnlyLookupPaths)
            llsubReaders.add(DirectoryReader.open(new SimpleFSDirectory(Paths.get(strLuceneReadOnlyLookupPath))));


        IndexReader lookupReader;
        if(llsubReaders.size() > 1)
            lookupReader = new MultiReader(llsubReaders.toArray(new IndexReader[0]), true);
        else
            lookupReader = reader4SourceIndex;



        // wir machen uns einen leeren initialen Writer zum schreiben - den Rest macht der ToLuceneContentHandler
        File fLuceneIndex = new File(strLuceneIndexPath);
        Path fOurTmpDir = Paths.get(fLuceneIndex.getAbsolutePath() + "_4PostProcessing");


        IndexWriterConfig config = new IndexWriterConfig(fieldConfig.createAnalyzer());
        config.setOpenMode(OpenMode.CREATE);

        IndexWriter firstTmpWriter = new IndexWriter(new SimpleFSDirectory(fOurTmpDir), config);

        ToLuceneContentHandler toLuceneContentHandler = new ToLuceneContentHandler(fieldConfig, firstTmpWriter);


        LoggerFactory.getLogger(LuceneIndexCreator.class.getName()).info("Will get the doc ids...");
        List<String> llAllIds = terms(LeechMetadata.id, "", Integer.MAX_VALUE, reader4SourceIndex);
        LoggerFactory.getLogger(LuceneIndexCreator.class.getName()).info("...finished");


        Set<String> sAttNames4BuzzwordCalculation = new HashSet<String>();

        sAttNames4BuzzwordCalculation.add(LeechMetadata.body);
        sAttNames4BuzzwordCalculation.add(TikaCoreProperties.TITLE.getName());

        DocumentFrqClass documentFrqClass = null;
        if(!StringUtils.nullOrWhitespace(m_strNewField4FrqClass)) documentFrqClass = new DocumentFrqClass(lookupReader, LeechMetadata.body);



        int i = 0;
        for (String strId : llAllIds)
        {

            TopDocs topDocs = searcher4SourceIndex.search(new TermQuery(new Term(LeechMetadata.id, strId)), 1);

            int iDocNo = topDocs.scoreDocs[0].doc;
            Document doc2modify = reader4SourceIndex.document(iDocNo);

            // es gibt einen bug, das bei vorhandenen numerischen Attributen z.B. das indexed-Attribut verloren geht, wenn man es hier nochmal ausliest und neu einspielt
            // - beim ersten einstellen gehts. Deshalb füge ich hier fields, die stored sind, nochmal neu ein.
            LuceneUtilz.reInsertStoredFieldTypes(doc2modify, fieldConfig);


            if(!StringUtils.nullOrWhitespace(m_strNewField4Buzzwords))
                Buzzwords.addBuzzwords(iDocNo, doc2modify, m_strNewField4Buzzwords, sAttNames4BuzzwordCalculation, m_iMaxNumberOfBuzzwords, m_bSkipSimilarTerms,
                        lookupReader);

            if(m_bEstimatePageCounts)
                PageCountEstimator.addHeuristicDocPageCounts(iDocNo, doc2modify, PagedText.N_PAGES.getName(), LeechMetadata.isHeuristicPageCount, LeechMetadata.body,
                        reader4SourceIndex);

            if(!StringUtils.nullOrWhitespace(m_strNewField4FrqClass)) documentFrqClass.addDocumentFrequencyClass(iDocNo, doc2modify, m_strNewField4FrqClass);


            for (String strAttName : m_staticAttributes2values.names())
            {
                String strAttValue = m_staticAttributes2values.get(strAttName);
                Field field = fieldConfig.createField(strAttName, strAttValue);

                doc2modify.add(field);
            }


            toLuceneContentHandler.processNewDocument(doc2modify);

            if(++i % 100000 == 0) LoggerFactory.getLogger(LuceneIndexCreator.class.getName()).info(StringUtils.beautifyNumber(i) + " docs postprocessed");

        }
        LoggerFactory.getLogger(LuceneIndexCreator.class.getName()).info(StringUtils.beautifyNumber(i) + " docs postprocessed");


        toLuceneContentHandler.crawlFinished();
        firstTmpWriter.forceMerge(1, true);
        firstTmpWriter.close();
        if(lookupReader instanceof MultiReader)
            lookupReader.close();
        else
            reader4SourceIndex.close();


        // jetzt müssen wir den alten Index durch den neuen ersetzen
        // es nervt, wenn es ein neues Verzeichnis ist (Kommandozeile) - besser die Inhalte verschieben

        // wir verschieben alle Dateien vom alten Index in ein neues, temporäres
        // File fBackup = new File(fLuceneIndex.getAbsolutePath() + "_bak");
        // fLuceneIndex.renameTo(fBackup);
        Path pUnpostProcessed = Paths.get(fLuceneIndex.getAbsolutePath(), "/unpostprocessed");
        Files.createDirectory(pUnpostProcessed);

        for (File fFileInOriginIndex : fLuceneIndex.listFiles())
        {
            if(!fFileInOriginIndex.isDirectory())
            {
                Path pFileInOriginIndex = Paths.get(fFileInOriginIndex.getAbsolutePath());
                Files.move(pFileInOriginIndex, pUnpostProcessed.resolve(pFileInOriginIndex.getFileName()));
            }
        }


        // nun verschieben wir die neuen Dateien alle in das alte, nun leere Indexverzeichnis
        Path pLuceneIndex = Paths.get(fLuceneIndex.getAbsolutePath());
        for (File fFileInTmpDir : fOurTmpDir.toFile().listFiles())
        {
            Path pFileInTmpDir = Paths.get(fFileInTmpDir.getAbsolutePath());
            Files.move(pFileInTmpDir, pLuceneIndex.resolve(pFileInTmpDir.getFileName()));
        }
        // fOurTmpDir.renameTo(fLuceneIndex);

        FileUtilz.deleteDirectory(new File(pUnpostProcessed.toString()));
        FileUtilz.deleteDirectory(fOurTmpDir.toFile());



        LoggerFactory.getLogger(LuceneIndexCreator.class.getName()).info(
                "...postprocessing finished. Needed " + StopWatch.formatTimeDistance(System.currentTimeMillis() - lStart));



    }


}
