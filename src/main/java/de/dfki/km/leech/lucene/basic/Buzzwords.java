package de.dfki.km.leech.lucene.basic;



import de.dfki.inquisitor.collections.MultiValueTreeMap;
// import de.dfki.inquisitor.lucene.DynamicFieldType;
// import de.dfki.inquisitor.lucene.*;
import de.dfki.inquisitor.text.Levenshtein;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.*;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.ClassicSimilarity;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.Map.Entry;



/**
 * The class Buzzwords extracts keywords out of documents - these can be in the form of lucene-documents, which enables to calculate the buzzwords very fast because the
 * most information is still in the lucene index. But also strings can be processed, with an index as a base for calculation
 * 
 * @author Christian Reuschling, Elisabeth Wolf
 * 
 */
public class Buzzwords
{


    static protected ClassicSimilarity m_defaultSimilarity = new ClassicSimilarity();



    //
    // /**
    //  * Adds calculated buzzwords to the given document. The method makes use of the IndexAccessor default Analyzer.
    //  *
    //  * @param doc2modify the document that should enriched with a new buzzword field
    //  * @param strIdFieldName the attribute name that should be used to identify the documents according to their id String
    //  * @param strNewField4Buzzwords the attribute that should be created for the buzzword. Becomes part of the document object
    //  * @param sAttNames4BuzzwordCalculation the attributes that should be considered for buzzword generation
    //  * @param iMaxNumberOfBuzzwords the maximum number of buzzwords the method should generate
    //  * @param bSkipSimilarTerms true: similar terms (according to the Levenshtein-distance) will be skipped for better readability
    //  * @param hsIndexPaths the list of indices that should be used for buzzword calculation. The document must be stored in exactly one index, referenced by the document
    //  *            object value of strIdFieldName.
    //  *
    //  * @return true in the case the document object was modified, false otherwise. The method do not modify the index entry
    //  *
    //  * @throws Exception
    //  */
    // static public boolean addBuzzwords(Document doc2modify, String strIdFieldName, String strNewField4Buzzwords, Set<String> sAttNames4BuzzwordCalculation,
    //         int iMaxNumberOfBuzzwords, boolean bSkipSimilarTerms, LinkedHashSet<String> hsIndexPaths) throws Exception
    // {
    //
    //
    //     String strDocID = getAttributeValue(doc2modify, strIdFieldName);
    //     List<String> lBuzzwords = getBuzzwords(strDocID, sAttNames4BuzzwordCalculation, iMaxNumberOfBuzzwords, bSkipSimilarTerms, hsIndexPaths);
    //
    //     // wenn es keinen Content gibt, mache mer gar nix
    //     if(lBuzzwords == null) return false;
    //
    //     StringBuilder strbBuzzWordz = new StringBuilder();
    //
    //     for (int i = 0; i < Math.min(iMaxNumberOfBuzzwords, lBuzzwords.size()); i++)
    //         strbBuzzWordz.append(lBuzzwords.get(i)).append(" ");
    //
    //
    //     // wenn es das Buzzword-feld schon gibt, wirds gelöscht
    //     doc2modify.removeFields(strNewField4Buzzwords);
    //     // die neu berechneten Buzzwords werden zum Doc hinzugefügt
    //     doc2modify.add(new TextWithTermVectorOffsetsField(strNewField4Buzzwords, strbBuzzWordz.toString()));
    //
    //
    //     return true;
    // }



    /**
     * Gets the value of an attribute inside the document as String.
     * 
     * @param doc
     * @param strFieldName the attributes name
     * 
     * @return the first attribute value under the given attribute name
     */
    private static String getAttributeValue(Document doc, String strFieldName)
    {

        IndexableField docAtt = doc.getField(strFieldName);
        if(docAtt == null) return null;


        return docAtt.stringValue();
    }



    //
    // /**
    //  * Gets the buzzwords for fields of a document. The metohd makes use of the IndexAccessor default Analyzer.
    //  *
    //  * @param strDocID the ID of the document from which the buzzwords should be extracted
    //  * @param sAttNames4BuzzwordCalculation the name of the attributes the buzzwords should be extracted from
    //  * @param iMaxNumberOfBuzzwords the maximum number of buzzwords
    //  * @param bSkipSimilarTerms true: similar terms (according to the Levenshtein-distance) will be skipped for better readability
    //  * @param hsIndexPaths the list of indices that should be used for buzzword calculation. The document must be stored in exactly one index, referenced by the document
    //  *            object value of strIdFieldName.
    //  *
    //  * @return the list of the extracted buzzwords, null in the case the given attribute doesn't exist
    //  *
    //  * @throws CorruptIndexException
    //  * @throws IOException
    //  * @throws URINotFoundException
    //  * @throws URISyntaxException
    //  */
    // static public List<String> getBuzzwords(String strDocID, Set<String> sAttNames4BuzzwordCalculation, int iMaxNumberOfBuzzwords, boolean bSkipSimilarTerms,
    //         LinkedHashSet<String> hsIndexPaths) throws CorruptIndexException, IOException, URINotFoundException, URISyntaxException
    // {
    //
    //     LinkedHashMap<String, Float> buzzwordsWithTfIdf =
    //             getBuzzwordsWithTfIdf(strDocID, sAttNames4BuzzwordCalculation, iMaxNumberOfBuzzwords, bSkipSimilarTerms, hsIndexPaths);
    //
    //     LinkedList<String> llBuzzwords = new LinkedList<String>(buzzwordsWithTfIdf.keySet());
    //
    //
    //     return llBuzzwords;
    // }


    //
    //
    // /**
    //  * Gets the buzzwords for fields of a document. The metohd makes use of the IndexAccessor default Analyzer.
    //  *
    //  * @param strDocID the ID of the document from which the buzzwords should be extracted
    //  * @param strFieldName the name of the attribute the buzzwords should be extracted from
    //  * @param iMaxNumberOfBuzzwords the maximum number of buzzwords
    //  * @param bSkipSimilarTerms true: similar terms (according to the Levenshtein-distance) will be skipped for better readability
    //  * @param hsIndexPaths the list of indices that should be used for buzzword calculation. The document must be stored in exactly one index, referenced by the document
    //  *            object value of strIdFieldName.
    //  *
    //  * @return the list of the extracted buzzwords, null in the case the given attribute doesn't exist
    //  * @throws CorruptIndexException
    //  * @throws IOException
    //  * @throws URINotFoundException
    //  * @throws URISyntaxException
    //  */
    // static public List<List<String>> getBuzzwords4AllFieldValues(String strDocID, String strFieldName, int iMaxNumberOfBuzzwords, boolean bSkipSimilarTerms,
    //         LinkedHashSet<String> hsIndexPaths) throws CorruptIndexException, IOException, URINotFoundException, URISyntaxException
    // {
    //
    //     List<LinkedHashMap<String, Float>> buzzwordsWithTfIdfMaps =
    //             getBuzzwordsWithTfIdf4AllFieldValues(strDocID, strFieldName, iMaxNumberOfBuzzwords, bSkipSimilarTerms, hsIndexPaths);
    //
    //     LinkedList<List<String>> llbuzzwords4AllFieldValues = new LinkedList<List<String>>();
    //     for (LinkedHashMap<String, Float> hsBuzzwords2TfIdf : buzzwordsWithTfIdfMaps)
    //     {
    //
    //         LinkedList<String> llBuzzwords = new LinkedList<String>(hsBuzzwords2TfIdf.keySet());
    //
    //         llbuzzwords4AllFieldValues.add(llBuzzwords);
    //     }
    //
    //
    //     return llbuzzwords4AllFieldValues;
    // }


    //
    //
    // /**
    //  * Gets the buzzwords for fields of a document, together with their document TfIdf value. The metohd makes use of the IndexAccessor default Analyzer.
    //  *
    //  * @param strDocID the ID of the document from which the buzzwords should be extracted
    //  * @param sAttNames4BuzzwordCalculation the name of the attributes the buzzwords should be extracted from.
    //  * @param iMaxNumberOfBuzzwords the maximum number of buzzwords
    //  * @param bSkipSimilarTerms true: similar terms (according to the Levenshtein-distance) will be skipped for better readability
    //  * @param hsIndexPaths the list of indices that should be used for buzzword calculation. The document must be stored in exactly one index, referenced by the document
    //  *            object value of strIdFieldName.
    //  *
    //  * @return the extracted buzzwords, boosted according their score. Key: the term itself. Value: the according score. null in the case the given attribute doesn't
    //  *         exist.
    //  * @throws CorruptIndexException
    //  * @throws IOException
    //  * @throws URINotFoundException
    //  * @throws URISyntaxException
    //  */
    // static public LinkedHashMap<String, Float> getBuzzwordsWithTfIdf(String strDocID, Set<String> sAttNames4BuzzwordCalculation, int iMaxNumberOfBuzzwords,
    //         boolean bSkipSimilarTerms, LinkedHashSet<String> hsIndexPaths) throws CorruptIndexException, IOException, URINotFoundException, URISyntaxException
    // {
    //
    //     MultiValueTreeMap<Float, String> tmScore2Term =
    //             retrieveInterestingTerms(strDocID, sAttNames4BuzzwordCalculation, iMaxNumberOfBuzzwords, 2, 1, 2, bSkipSimilarTerms, hsIndexPaths);
    //
    //     if(tmScore2Term.valueSize() < iMaxNumberOfBuzzwords)
    //     {
    //
    //         MultiValueTreeMap<Float, String> tmScore2TermWeak =
    //                 retrieveInterestingTerms(strDocID, sAttNames4BuzzwordCalculation, iMaxNumberOfBuzzwords, 1, 1, 2, bSkipSimilarTerms, hsIndexPaths);
    //
    //         while (tmScore2TermWeak.keySize() > 0)
    //         {
    //             Float fTfIdf = tmScore2TermWeak.firstKey();
    //             String strTopTerm = tmScore2TermWeak.getFirst(fTfIdf);
    //             tmScore2TermWeak.remove(fTfIdf, strTopTerm);
    //
    //             if(!tmScore2Term.containsValue(strTopTerm)) tmScore2Term.add(fTfIdf, strTopTerm);
    //
    //             if(tmScore2Term.valueSize() >= iMaxNumberOfBuzzwords) break;
    //         }
    //     }
    //
    //     LinkedHashMap<String, Float> hsTerm2TfIdf = new LinkedHashMap<String, Float>();
    //     for (Entry<Float, String> score2term : tmScore2Term.entryList())
    //         hsTerm2TfIdf.put(score2term.getValue(), score2term.getKey());
    //
    //
    //     return hsTerm2TfIdf;
    // }



    //
    // /**
    //  * This method is for calculating buzzwords out of an arbritrary String, by giving an index attribute as 'context. The string will be tokenized according the given
    //  * analyzer for this attribute (as set by the IndexAccessor default analyzer), and also takes the document frequencies for all terms of this attribute.
    //  *
    //  * @param strDocumentText the text of the document. This text influences the buzzword calculation as it would be an attribute value of
    //  *            strAttributeName4BuzzwordCalculation
    //  * @param strAttributeName4BuzzwordCalculation this is the name of the attribute the given text should be differentiated against with buzzwords
    //  * @param iMaxNumberOfBuzzwords the maximum number of buzzwords
    //  * @param bSkipSimilarTerms true: similar terms (according to the Levenshtein-distance) will be skipped for better readability
    //  * @param hsIndexPaths the list of indices that should be used for buzzword calculation. The document must be stored in exactly one index, referenced by the document
    //  *            object value of strIdFieldName.
    //  *
    //  * @return the extracted buzzwords, with their according tfidf value, sorted by TfIdf values. Key: the term itself. Value: the tfIdf value.
    //  *
    //  * @throws CorruptIndexException
    //  * @throws IOException
    //  * @throws URINotFoundException
    //  * @throws URISyntaxException
    //  */
    // static public LinkedHashMap<String, Float> getBuzzwordsWithTfIdf(String strDocumentText, String strAttributeName4BuzzwordCalculation, int iMaxNumberOfBuzzwords,
    //         boolean bSkipSimilarTerms, LinkedHashSet<String> hsIndexPaths) throws CorruptIndexException, IOException, URINotFoundException, URISyntaxException
    // {
    //     MultiValueTreeMap<Float, String> tmScore2Term =
    //             retrieveInterestingTerms(strDocumentText, strAttributeName4BuzzwordCalculation, iMaxNumberOfBuzzwords, 2, 1, 2, bSkipSimilarTerms, hsIndexPaths);
    //
    //     if(tmScore2Term.valueSize() < iMaxNumberOfBuzzwords)
    //     {
    //
    //         MultiValueTreeMap<Float, String> tmScore2TermWeak =
    //                 retrieveInterestingTerms(strDocumentText, strAttributeName4BuzzwordCalculation, iMaxNumberOfBuzzwords, 1, 1, 2, bSkipSimilarTerms, hsIndexPaths);
    //
    //         while (tmScore2TermWeak.keySize() > 0)
    //         {
    //             Float fTfIdf = tmScore2TermWeak.firstKey();
    //             String strTopTerm = tmScore2TermWeak.getFirst(fTfIdf);
    //             tmScore2TermWeak.remove(fTfIdf, strTopTerm);
    //
    //             if(!tmScore2Term.containsValue(strTopTerm)) tmScore2Term.add(fTfIdf, strTopTerm);
    //
    //             if(tmScore2Term.valueSize() >= iMaxNumberOfBuzzwords) break;
    //         }
    //     }
    //
    //     LinkedHashMap<String, Float> hsTerm2TfIdf = new LinkedHashMap<String, Float>();
    //     for (Entry<Float, String> score2term : tmScore2Term.entryList())
    //         hsTerm2TfIdf.put(score2term.getValue(), score2term.getKey());
    //
    //
    //     return hsTerm2TfIdf;
    //
    // }



    // /**
    //  * Gets the buzzwords for fields of a document, together with their document TfIdf value. The metohd makes use of the IndexAccessor default Analyzer.
    //  *
    //  * @param strDocID the ID of the document from which the buzzwords should be extracted
    //  * @param strFieldName the name of the attribute the buzzwords should be extracted from.
    //  * @param iMaxNumberOfBuzzwords the maximum number of buzzwords
    //  * @param bSkipSimilarTerms true: similar terms (according to the Levenshtein-distance) will be skipped for better readability
    //  * @param hsIndexPaths the list of indices that should be used for buzzword calculation. The document must be stored in exactly one index, referenced by the document
    //  *            object value of strIdFieldName.
    //  *
    //  * @return the extracted buzzwords, boosted according their score. Key: the term itself. Value: the according score. null in the case the given attribute doesn't
    //  *         exist.
    //  * @throws CorruptIndexException
    //  * @throws IOException
    //  * @throws URINotFoundException
    //  * @throws URISyntaxException
    //  */
    // static public List<LinkedHashMap<String, Float>> getBuzzwordsWithTfIdf4AllFieldValues(String strDocID, String strFieldName, int iMaxNumberOfBuzzwords,
    //         boolean bSkipSimilarTerms, LinkedHashSet<String> hsIndexPaths) throws CorruptIndexException, IOException, URINotFoundException, URISyntaxException
    // {
    //
    //     List<MultiValueTreeMap<Float, String>> tmScore2TermMaps =
    //             retrieveInterestingTerms4AllFieldValues(strDocID, strFieldName, iMaxNumberOfBuzzwords, 2, 1, 2, bSkipSimilarTerms, hsIndexPaths);
    //
    //     // aus Performancegründen verzichte ich hier mal auf eine 'weichere' Strategie, falls unsere Maximalanzahl der Buzzwords nicht erreicht wurde
    //
    //     LinkedList<LinkedHashMap<String, Float>> hsTerm2ScoreMaps = new LinkedList<LinkedHashMap<String, Float>>();
    //
    //     for (MultiValueTreeMap<Float, String> hsScore2Term : tmScore2TermMaps)
    //     {
    //         LinkedHashMap<String, Float> hsTerm2TfIdf = new LinkedHashMap<String, Float>();
    //         for (Entry<Float, String> score2term : hsScore2Term.entryList())
    //             hsTerm2TfIdf.put(score2term.getValue(), score2term.getKey());
    //
    //         hsTerm2ScoreMaps.add(hsTerm2TfIdf);
    //     }
    //
    //
    //     return hsTerm2ScoreMaps;
    // }


    

    /**
     * Adds calculated buzzwords to the given document. The method makes use of the IndexAccessor default Analyzer.
     * 
     * @param iDocNo the lucene document number inside the index behind reader, for the document doc2modify
     * @param doc2modify the document that should enriched with a new buzzword field
     * @param strNewField4Buzzwords the attribute that should be created for the buzzword. Becomes part of the document object
     * @param sAttNames4BuzzwordCalculation the attributes that should be considered for buzzword generation
     * @param iMaxNumberOfBuzzwords the maximum number of buzzwords the method should generate
     * @param bSkipSimilarTerms true: similar terms (according to the Levenshtein-distance) will be skipped for better readability
     * @param reader the lucene index reader
     * 
     * @return true in the case the document object was modified, false otherwise. The method do not modify the index entry
     * 
     * @throws Exception
     */
    static public boolean addBuzzwords(int iDocNo, Document doc2modify, String strNewField4Buzzwords, Set<String> sAttNames4BuzzwordCalculation,
            int iMaxNumberOfBuzzwords, boolean bSkipSimilarTerms, IndexReader reader) throws Exception
    {


        List<String> lBuzzwords = getBuzzwords(iDocNo, doc2modify, sAttNames4BuzzwordCalculation, iMaxNumberOfBuzzwords, bSkipSimilarTerms, reader);

        // wenn es keinen Content gibt, mache mer gar nix
        if(lBuzzwords == null) return false;

        StringBuilder strbBuzzWordz = new StringBuilder();

        for (int i = 0; i < Math.min(iMaxNumberOfBuzzwords, lBuzzwords.size()); i++)
            strbBuzzWordz.append(lBuzzwords.get(i)).append(" ");


        // wenn es das Buzzword-feld schon gibt, wirds gelöscht
        doc2modify.removeFields(strNewField4Buzzwords);
        // die neu berechneten Buzzwords werden zum Doc hinzugefügt
        FieldType fieldType =
                new DynamicFieldType().setIndexOptionS(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS).setStoreD(true).setStoreTermVectorS(true)
                        .setStoreTermVectorOffsetS(true).setTokenizeD(true).freezE();

        Field field4buzzwords = new Field(strNewField4Buzzwords, strbBuzzWordz.toString(), fieldType);
        doc2modify.add(field4buzzwords);


        return true;
    }



    static protected int docID2DocNo(String strDocIdAttributeName, String strDocID, IndexReader reader) throws Exception
    {
        int luceneDocumentNumber;

        IndexSearcher searcher = new IndexSearcher(reader);

        TopDocs topDocs = searcher.search(new TermQuery(new Term(strDocIdAttributeName, strDocID)), 1);

        if(topDocs.totalHits == 0) throw new Exception("no lucene document found with id '" + strDocID + "'");

        // es sollte lediglich ein Dokument mit dieser id aufzufinden sein...
        luceneDocumentNumber = topDocs.scoreDocs[0].doc;

        return luceneDocumentNumber;
    }







    /**
     * Gets the buzzwords for fields of a document. The metohd makes use of the IndexAccessor default Analyzer.
     * 
     * @param iDocNo the lucene document number inside the index behind reader, for the document doc2modify
     * @param doc2modify the document that should enriched with a new buzzword field
     * @param sAttNames4BuzzwordCalculation the name of the attributes the buzzwords should be extracted from
     * @param iMaxNumberOfBuzzwords the maximum number of buzzwords
     * @param bSkipSimilarTerms true: similar terms (according to the Levenshtein-distance) will be skipped for better readability
     * @param reader the lucene index reader
     * 
     * @return the list of the extracted buzzwords, null in the case the given attribute doesn't exist
     * 
     * @throws Exception
     * @throws URINotFoundException
     */
    static public List<String> getBuzzwords(int iDocNo, Document doc2modify, Set<String> sAttNames4BuzzwordCalculation, int iMaxNumberOfBuzzwords,
            boolean bSkipSimilarTerms, IndexReader reader) throws Exception
    {

        LinkedHashMap<String, Float> buzzwordsWithTfIdf =
                getBuzzwordsWithTfIdf(iDocNo, doc2modify, sAttNames4BuzzwordCalculation, iMaxNumberOfBuzzwords, bSkipSimilarTerms, reader);

        LinkedList<String> llBuzzwords = new LinkedList<String>(buzzwordsWithTfIdf.keySet());


        return llBuzzwords;
    }



    /**
     * Gets the buzzwords for fields of a document, together with their document TfIdf value. The metohd makes use of the IndexAccessor default Analyzer.
     * 
     * @param iDocNo the lucene document number inside the index behind reader, for the document doc2modify
     * @param doc2modify the document that should enriched with a new buzzword field
     * @param sAttNames4BuzzwordCalculation the name of the attributes the buzzwords should be extracted from.
     * @param iMaxNumberOfBuzzwords the maximum number of buzzwords
     * @param bSkipSimilarTerms true: similar terms (according to the Levenshtein-distance) will be skipped for better readability
     * @param reader the lucene index reader
     * 
     * @return the extracted buzzwords, boosted according their score. Key: the term itself. Value: the according score. null in the case the given attribute doesn't
     *         exist.
     * 
     * @throws Exception
     */
    static public LinkedHashMap<String, Float> getBuzzwordsWithTfIdf(int iDocNo, Document doc2modify, Set<String> sAttNames4BuzzwordCalculation,
            int iMaxNumberOfBuzzwords, boolean bSkipSimilarTerms, IndexReader reader) throws Exception
    {

        MultiValueTreeMap<Float, String> tmScore2Term =
                retrieveInterestingTerms(iDocNo, doc2modify, sAttNames4BuzzwordCalculation, iMaxNumberOfBuzzwords, 2, 1, 2, bSkipSimilarTerms, reader);

        if(tmScore2Term.valueSize() < iMaxNumberOfBuzzwords)
        {

            MultiValueTreeMap<Float, String> tmScore2TermWeak =
                    retrieveInterestingTerms(iDocNo, doc2modify, sAttNames4BuzzwordCalculation, iMaxNumberOfBuzzwords, 1, 1, 2, bSkipSimilarTerms, reader);

            while (tmScore2TermWeak.keySize() > 0)
            {
                Float fTfIdf = tmScore2TermWeak.firstKey();
                String strTopTerm = tmScore2TermWeak.getFirst(fTfIdf);
                tmScore2TermWeak.remove(fTfIdf, strTopTerm);

                if(!tmScore2Term.containsValue(strTopTerm)) tmScore2Term.add(fTfIdf, strTopTerm);

                if(tmScore2Term.valueSize() >= iMaxNumberOfBuzzwords) break;
            }
        }

        LinkedHashMap<String, Float> hsTerm2TfIdf = new LinkedHashMap<String, Float>();
        for (Entry<Float, String> score2term : tmScore2Term.entryList())
            hsTerm2TfIdf.put(score2term.getValue(), score2term.getKey());


        return hsTerm2TfIdf;
    }



    /**
     * 
     * @param iDocNo the lucene document number inside the index behind reader, for the document doc2modify
     * @param doc2modify the document that should enriched with a new buzzword field
     * @param strFieldName the field where you want the top frequent terms for.
     * @param iMinFrequency the minimum frequency a term must appear in this field
     * @param iMinWordLength the minimum word length a term must have
     * @param iMaxNumberOfTerms the maximum number of terms the method returns
     * @param reader the lucene index reader
     * 
     * @return
     * 
     * @throws Exception
     */
    public static List<Term2FrequencyEntry> getTopFrequentTerms(int iDocNo, Document doc2modify, String strFieldName, int iMinFrequency, int iMinWordLength,
                                                                int iMaxNumberOfTerms, IndexReader reader) throws Exception
    {

        LinkedList<Term2FrequencyEntry> llTerm2Frequency = new LinkedList<Term2FrequencyEntry>();
        PriorityQueue<Term2FrequencyEntry> pqTerm2Frequency = new PriorityQueue<Term2FrequencyEntry>(iMaxNumberOfTerms, new Comparator<Term2FrequencyEntry>()
        {

            @Override
            public int compare(Term2FrequencyEntry o1, Term2FrequencyEntry o2)
            {
                return o1.getFrequency().compareTo(o2.getFrequency());
            }
        });

        // wenn es das feld gar nicht gibt in diesem doc, dann machen wir gar nix! (das überprüfen ist erheblich billiger als das unnötige iterieren durch alles im reader
        if(doc2modify.getField(strFieldName) == null) return llTerm2Frequency;

        Terms termVector = reader.getTermVector(iDocNo, strFieldName);
        if(termVector == null) return llTerm2Frequency;

        TermsEnum termsEnum = termVector.iterator();

        while (termsEnum.next() != null)
        {
            String strTerm = termsEnum.term().utf8ToString();
            long lFrequency = termsEnum.totalTermFreq();

            if(lFrequency >= iMinFrequency && strTerm.length() >= iMinWordLength)
                pqTerm2Frequency.add(new Term2FrequencyEntry(strTerm, Long.valueOf(lFrequency).intValue()));

            if(pqTerm2Frequency.size() > iMaxNumberOfTerms) pqTerm2Frequency.poll();
        }

        for (Term2FrequencyEntry term2Frq : pqTerm2Frequency)
            llTerm2Frequency.add(0, term2Frq);



        return llTerm2Frequency;
    }



    static MultiValueTreeMap<Float, String> retrieveInterestingTerms(int iDocNo, Document doc2modify, Set<String> sAttNames4BuzzwordCalculation,
            int iMaxNumberOfBuzzwords, int iMinDocFreq, int iMinTermFreq, int iMinWordLen, boolean bSkipSimilarTerms, IndexReader reader) throws Exception
    {

        int iIndexDocumentCount = reader.numDocs();

        HashMap<String, Integer> hsTerm2Frequency = new HashMap<String, Integer>();

        // als erstes werden die frequencies aller fields aufsummiert
        for (String strFieldName : sAttNames4BuzzwordCalculation)
        {

            // XXX: hier ist erst mal die Anzahl der verschiedenen Terme des docs hartkodiert
            List<Term2FrequencyEntry> topFrequentTerms = getTopFrequentTerms(iDocNo, doc2modify, strFieldName, iMinTermFreq, iMinWordLen, 1234, reader);

            for (Term2FrequencyEntry topTerm2FreqLocal : topFrequentTerms)
            {
                Integer iFreqOld = hsTerm2Frequency.get(topTerm2FreqLocal.getTerm());
                if(iFreqOld == null)
                    iFreqOld = topTerm2FreqLocal.getFrequency();
                else
                    iFreqOld += topTerm2FreqLocal.getFrequency();

                hsTerm2Frequency.put(topTerm2FreqLocal.getTerm(), iFreqOld);
            }
        }

        // nun werden die Terme bezüglich ihres scores (tfIdf) sortiert
        MultiValueTreeMap<Float, String> tmScore2Term = new MultiValueTreeMap<Float, String>(HashSet.class);
        for (Entry<String, Integer> term2Frequency : hsTerm2Frequency.entrySet())
        {
            String strTerm = term2Frequency.getKey();
            Integer iTermFrequency = term2Frequency.getValue();

            // wir haben angegeben, wie oft der Term mindestens da sein muß
            if(iMinTermFreq > 0 && iTermFrequency < iMinTermFreq) continue;

            // Zahlen ignorieren wir
            if(!strTerm.matches("\\D+")) continue;

            // es wird die max-docFrequency berücksichtig (wie in MoreLikeThis)
            int iMaxDocumentFrequency = 0;
            for (String strField : sAttNames4BuzzwordCalculation)
            {
                int iDocumentFrequency = reader.docFreq(new Term(strField, strTerm));
                if(iMaxDocumentFrequency < iDocumentFrequency) iMaxDocumentFrequency = iDocumentFrequency;
            }

            if(iMinDocFreq > 0 && iMaxDocumentFrequency < iMinDocFreq) continue;

            // das sollte eigentlich nicht passieren - im Fehlerfall ignorieren wir das einfach
            if(iMaxDocumentFrequency == 0) continue;

            // das ist die Formel der defaultSimilarity. Eine andere werden wir einfach nie brauchen
            float fIdf = m_defaultSimilarity.idf(iMaxDocumentFrequency, iIndexDocumentCount);
            float fScore = m_defaultSimilarity.tf(iTermFrequency) * fIdf * fIdf;

            boolean bRemoveLastTerm4Score = false;
            // nur die top -Terme - wenn wir über die max-Anzahl sind, dann tauschen wir den kleinsten aus
            if(tmScore2Term.valueSize() >= iMaxNumberOfBuzzwords)
            {
                // wir sind drüber
                // wenn unser kleinster schon größer ist, dann ignorieren wir den neuen
                if(tmScore2Term.firstKey() >= fScore) continue;
                // ansonsten tauschen wir unseren kleinsten aus
                bRemoveLastTerm4Score = true;
            }


            // wir schauen, ob wir schon einen term drin haben, der uns sehr ähnlich sieht - dann nehmen wir den mit dem höchsten score (alternativ
            // wäre auch der kürzere möglich, aber der könnte einen niederen score haben, und dann später wieder rausfliegen - das würde die Qualität
            // verschlechtern)
            Boolean bBetterSimilarTermInList = false;
            if(bSkipSimilarTerms)
            {
                for (Entry<Float, String> score2TermInList : tmScore2Term.entryList())
                {
                    if(!Levenshtein.isInDistance(score2TermInList.getValue(), strTerm, 3)) continue;
                    // wenn der existierende größer ist, dann brauchen wir gar nix eintragen
                    if(score2TermInList.getKey() >= fScore)
                    {
                        bBetterSimilarTermInList = true;
                        break;
                    }
                    // wenn der neue vom score her besser ist, dann müssen wir den austauschen
                    tmScore2Term.remove(score2TermInList.getKey(), score2TermInList.getValue());
                }
            }

            if(bRemoveLastTerm4Score && !bBetterSimilarTermInList) tmScore2Term.remove(tmScore2Term.firstKey());
            if(!bBetterSimilarTermInList) tmScore2Term.add(fScore, strTerm);
        }


        return tmScore2Term;
    }
    
    
    
    
    

    // static MultiValueTreeMap<Float, String> retrieveInterestingTerms(String strDocID, Set<String> sAttNames4BuzzwordCalculation, int iMaxNumberOfBuzzwords,
    //         int iMinDocFreq, int iMinTermFreq, int iMinWordLen, boolean bSkipSimilarTerms, LinkedHashSet<String> hsIndexPaths) throws CorruptIndexException, IOException,
    //         URINotFoundException, URISyntaxException
    // {
    //
    //     RemoteIndexReader reader = IndexAccessor.getMultiIndexReader(hsIndexPaths, true);
    //     int iIndexDocumentCount = reader.numDocs();
    //
    //     HashMap<String, Integer> hsTerm2Frequency = new HashMap<String, Integer>();
    //
    //     // als erstes werden die frequencies aller fields aufsummiert
    //     for (String strFieldName : sAttNames4BuzzwordCalculation)
    //     {
    //
    //         // XXX: hier ist erst mal die Anzahl der verschiedenen Terme des docs hartkodiert
    //         List<Term2FrequencyEntry> topFrequentTerms = reader.getTopFrequentTerms(strDocID, strFieldName, iMinTermFreq, iMinWordLen, 1234);
    //
    //         for (Term2FrequencyEntry topTerm2FreqLocal : topFrequentTerms)
    //         {
    //             Integer iFreqOld = hsTerm2Frequency.get(topTerm2FreqLocal.getTerm());
    //             if(iFreqOld == null)
    //                 iFreqOld = topTerm2FreqLocal.getFrequency();
    //             else
    //                 iFreqOld += topTerm2FreqLocal.getFrequency();
    //
    //             hsTerm2Frequency.put(topTerm2FreqLocal.getTerm(), iFreqOld);
    //         }
    //     }
    //
    //     // nun werden die Terme bezüglich ihres scores (tfIdf) sortiert
    //     MultiValueTreeMap<Float, String> tmScore2Term = new MultiValueTreeMap<Float, String>(HashSet.class);
    //     for (Entry<String, Integer> term2Frequency : hsTerm2Frequency.entrySet())
    //     {
    //         String strTerm = term2Frequency.getKey();
    //         Integer iTermFrequency = term2Frequency.getValue();
    //
    //         // wir haben angegeben, wie oft der Term mindestens da sein muß
    //         if(iMinTermFreq > 0 && iTermFrequency < iMinTermFreq) continue;
    //
    //         // Zahlen ignorieren wir
    //         if(!strTerm.matches("\\D+")) continue;
    //
    //         // es wird die max-docFrequency berücksichtig (wie in MoreLikeThis)
    //         int iMaxDocumentFrequency = 0;
    //         for (String strField : sAttNames4BuzzwordCalculation)
    //         {
    //             int iDocumentFrequency = reader.documentFrequency(strField, strTerm);
    //             if(iMaxDocumentFrequency < iDocumentFrequency) iMaxDocumentFrequency = iDocumentFrequency;
    //         }
    //
    //         if(iMinDocFreq > 0 && iMaxDocumentFrequency < iMinDocFreq) continue;
    //
    //         // das sollte eigentlich nicht passieren - im Fehlerfall ignorieren wir das einfach
    //         if(iMaxDocumentFrequency == 0) continue;
    //
    //         // das ist die Formel der defaultSimilarity. Eine andere werden wir einfach nie brauchen
    //         float fIdf = m_defaultSimilarity.idf(iMaxDocumentFrequency, iIndexDocumentCount);
    //         float fScore = m_defaultSimilarity.tf(iTermFrequency) * fIdf * fIdf;
    //
    //         boolean bRemoveLastTerm4Score = false;
    //         // nur die top -Terme - wenn wir über die max-Anzahl sind, dann tauschen wir den kleinsten aus
    //         if(tmScore2Term.valueSize() >= iMaxNumberOfBuzzwords)
    //         {
    //             // wir sind drüber
    //             // wenn unser kleinster schon größer ist, dann ignorieren wir den neuen
    //             if(tmScore2Term.firstKey() >= fScore) continue;
    //             // ansonsten tauschen wir unseren kleinsten aus
    //             bRemoveLastTerm4Score = true;
    //         }
    //
    //
    //         // wir schauen, ob wir schon einen term drin haben, der uns sehr ähnlich sieht - dann nehmen wir den mit dem höchsten score (alternativ
    //         // wäre auch der kürzere möglich, aber der könnte einen niederen score haben, und dann später wieder rausfliegen - das würde die Qualität
    //         // verschlechtern)
    //         Boolean bBetterSimilarTermInList = false;
    //         if(bSkipSimilarTerms)
    //         {
    //             for (Entry<Float, String> score2TermInList : tmScore2Term.entryList())
    //             {
    //                 if(!Levenshtein.isInDistance(score2TermInList.getValue(), strTerm, 3)) continue;
    //                 // wenn der existierende größer ist, dann brauchen wir gar nix eintragen
    //                 if(score2TermInList.getKey() >= fScore)
    //                 {
    //                     bBetterSimilarTermInList = true;
    //                     break;
    //                 }
    //                 // wenn der neue vom score her besser ist, dann müssen wir den austauschen
    //                 tmScore2Term.remove(score2TermInList.getKey(), score2TermInList.getValue());
    //             }
    //         }
    //
    //         if(bRemoveLastTerm4Score && !bBetterSimilarTermInList) tmScore2Term.remove(tmScore2Term.firstKey());
    //         if(!bBetterSimilarTermInList) tmScore2Term.add(fScore, strTerm);
    //     }
    //
    //
    //     return tmScore2Term;
    // }


    //
    // /**
    //  * This method is for calculating buzzwords out of an arbritrary String, by giving an index attribute as 'context. The string will be tokenized according the given
    //  * analyzer for this attribute (as set by the IndexAccessor default analyzer), and also takes the document frequencies for all terms of this attribute.
    //  *
    //  * @param strDocumentText the text of the document. This text influences the buzzword calculation as it would be an attribute value of
    //  *            strAttributeName4BuzzwordCalculation
    //  * @param strAttributeName4BuzzwordCalculation this is the name of the attribute the given text should be differentiated against with buzzwords
    //  * @param iMaxNumberOfBuzzwords the maximum number of buzzwords
    //  * @param iMinDocFreq
    //  * @param iMinTermFreq
    //  * @param iMinWordLen
    //  * @param bSkipSimilarTerms true: similar terms (according to the Levenshtein-distance) will be skipped for better readability
    //  * @param hsIndexPaths the list of indices that should be used for buzzword calculation. The document must be stored in exactly one index, referenced by the document
    //  *            object value of strIdFieldName.
    //  *
    //  * @return the extracted buzzwords, sorted by their according tfidf value. Key: the tfIdf value. Value: the term.
    //  *
    //  * @throws CorruptIndexException
    //  * @throws IOException
    //  * @throws URINotFoundException
    //  * @throws URISyntaxException
    //  */
    // static MultiValueTreeMap<Float, String> retrieveInterestingTerms(String strDocumentText, String strAttributeName4BuzzwordCalculation, int iMaxNumberOfBuzzwords,
    //         int iMinDocFreq, int iMinTermFreq, int iMinWordLen, boolean bSkipSimilarTerms, LinkedHashSet<String> hsIndexPaths) throws CorruptIndexException, IOException,
    //         URINotFoundException, URISyntaxException
    // {
    //
    //     RemoteIndexReader reader = IndexAccessor.getMultiIndexReader(hsIndexPaths, true);
    //     int iIndexDocumentCount = reader.numDocs();
    //
    //     // hier tokenisieren wir den übergebenen Text und ermitteln die term frequencies
    //     HashMap<String, Integer> hsTerm2Frequency = new HashMap<String, Integer>();
    //
    //     TokenStream tokenStream = IndexAccessor.getDefaultAnalyzer().tokenStream(strAttributeName4BuzzwordCalculation, strDocumentText);
    //
    //     tokenStream.reset();
    //     while (tokenStream.incrementToken())
    //     {
    //         // hier ermitteln wir die termfrequenzen für das aktuelle AttValue
    //         CharTermAttribute termAttribute = tokenStream.getAttribute(CharTermAttribute.class);
    //         String strTerm = termAttribute.toString();
    //
    //         Integer iFrequency = hsTerm2Frequency.get(strTerm);
    //         if(iFrequency == null)
    //             hsTerm2Frequency.put(strTerm, 1);
    //         else
    //             hsTerm2Frequency.put(strTerm, iFrequency + 1);
    //     }
    //     tokenStream.close();
    //
    //
    //
    //     // nun werden die Terme bezüglich ihres scores (tfIdf) sortiert
    //     MultiValueTreeMap<Float, String> tmScore2Term = new MultiValueTreeMap<Float, String>(HashSet.class);
    //     for (Entry<String, Integer> term2Frequency : hsTerm2Frequency.entrySet())
    //     {
    //         String strTerm = term2Frequency.getKey();
    //         Integer iTermFrequency = term2Frequency.getValue();
    //
    //
    //         if(strTerm.length() < iMinWordLen) continue;
    //         // wir haben angegeben, wie oft der Term mindestens da sein muß
    //         if(iMinTermFreq > 0 && iTermFrequency < iMinTermFreq) continue;
    //
    //         // Zahlen ignorieren wir
    //         if(!strTerm.matches("\\D+")) continue;
    //
    //         int iDocumentFrequency = reader.documentFrequency(strAttributeName4BuzzwordCalculation, strTerm);
    //
    //         if(iMinDocFreq > 0 && iDocumentFrequency < iMinDocFreq) continue;
    //
    //         // das sollte eigentlich nicht passieren - im Fehlerfall ignorieren wir das einfach
    //         if(iDocumentFrequency == 0) continue;
    //
    //         // das ist die Formel der defaultSimilarity. Eine andere werden wir einfach nie brauchen
    //         float fIdf = m_defaultSimilarity.idf(iDocumentFrequency, iIndexDocumentCount);
    //         float fScore = m_defaultSimilarity.tf(iTermFrequency) * fIdf * fIdf;
    //
    //         boolean bRemoveLastTerm4Score = false;
    //         // nur die top -Terme - wenn wir über die max-Anzahl sind, dann tauschen wir den kleinsten aus
    //         if(tmScore2Term.valueSize() >= iMaxNumberOfBuzzwords)
    //         {
    //             // wir sind drüber
    //             // wenn unser kleinster schon größer ist, dann ignorieren wir den neuen
    //             if(tmScore2Term.firstKey() >= fScore) continue;
    //             // ansonsten tauschen wir unseren kleinsten aus
    //             bRemoveLastTerm4Score = true;
    //         }
    //
    //
    //         // wir schauen, ob wir schon einen term drin haben, der uns sehr ähnlich sieht - dann nehmen wir den mit dem höchsten score (alternativ
    //         // wäre auch der kürzere möglich, aber der könnte einen niederen score haben, und dann später wieder rausfliegen - das würde die Qualität
    //         // verschlechtern)
    //         Boolean bBetterSimilarTermInList = false;
    //         if(bSkipSimilarTerms)
    //         {
    //             for (Entry<Float, String> score2TermInList : tmScore2Term.entryList())
    //             {
    //                 if(!Levenshtein.isInDistance(score2TermInList.getValue(), strTerm, 3)) continue;
    //                 // wenn der existierende größer ist, dann brauchen wir gar nix eintragen
    //                 if(score2TermInList.getKey() >= fScore)
    //                 {
    //                     bBetterSimilarTermInList = true;
    //                     break;
    //                 }
    //                 // wenn der neue vom score her besser ist, dann müssen wir den austauschen
    //                 tmScore2Term.remove(score2TermInList.getKey(), score2TermInList.getValue());
    //             }
    //         }
    //
    //         if(bRemoveLastTerm4Score && !bBetterSimilarTermInList) tmScore2Term.remove(tmScore2Term.firstKey());
    //         if(!bBetterSimilarTermInList) tmScore2Term.add(fScore, strTerm);
    //     }
    //
    //
    //
    //     return tmScore2Term;
    // }

    //
    //
    // static List<MultiValueTreeMap<Float, String>> retrieveInterestingTerms4AllFieldValues(String strDocID, String strFieldName, int iMaxNumberOfBuzzwords,
    //         int iMinDocFreq, int iMinTermFreq, int iMinWordLen, boolean bSkipSimilarTerms, LinkedHashSet<String> hsIndexPaths) throws CorruptIndexException, IOException,
    //         URINotFoundException, URISyntaxException
    // {
    //
    //     RemoteIndexReader reader = IndexAccessor.getMultiIndexReader(hsIndexPaths, true);
    //     int iIndexDocumentCount = reader.numDocs();
    //
    //
    //     LinkedList<MultiValueTreeMap<Float, String>> llScore2TermMaps = new LinkedList<MultiValueTreeMap<Float, String>>();
    //
    //     // XXX: hier ist erst mal die Anzahl der verschiedenen Terme des docs hartkodiert
    //     for (List<Term2FrequencyEntry> lTerm2Frequencies : reader.getTopFrequentTermsPerAttributeValue(strDocID, strFieldName, iMinTermFreq, iMinWordLen, 1234))
    //     {
    //
    //         // nun werden die Terme bezüglich ihres scores (tfIdf) sortiert
    //         MultiValueTreeMap<Float, String> tmScore2Term = new MultiValueTreeMap<Float, String>(HashSet.class);
    //         for (Term2FrequencyEntry term2Frequency : lTerm2Frequencies)
    //         {
    //             String strTerm = term2Frequency.getTerm();
    //             Integer iTermFrequency = term2Frequency.getFrequency();
    //
    //             // wir haben angegeben, wie oft der Term mindestens da sein muß
    //             if(iMinTermFreq > 0 && iTermFrequency < iMinTermFreq) continue;
    //
    //             // Zahlen ignorieren wir
    //             if(!strTerm.matches("\\D+")) continue;
    //
    //             int iDocumentFrequency = reader.documentFrequency(strFieldName, strTerm);
    //
    //             if(iMinDocFreq > 0 && iDocumentFrequency < iMinDocFreq) continue;
    //
    //             // das sollte eigentlich nicht passieren - im Fehlerfall ignorieren wir das einfach
    //             if(iDocumentFrequency == 0) continue;
    //
    //             // das ist die Formel der defaultSimilarity. Eine andere werden wir einfach nie brauchen
    //             float fIdf = m_defaultSimilarity.idf(iDocumentFrequency, iIndexDocumentCount);
    //             float fScore = m_defaultSimilarity.tf(iTermFrequency) * fIdf * fIdf;
    //
    //             boolean bRemoveLastTerm4Score = false;
    //             // nur die top -Terme - wenn wir über die max-Anzahl sind, dann tauschen wir den kleinsten aus
    //             if(tmScore2Term.valueSize() >= iMaxNumberOfBuzzwords)
    //             {
    //                 // wir sind drüber
    //                 // wenn unser kleinster schon größer ist, dann ignorieren wir den neuen
    //                 if(tmScore2Term.firstKey() >= fScore) continue;
    //                 // ansonsten tauschen wir unseren kleinsten aus
    //                 bRemoveLastTerm4Score = true;
    //             }
    //
    //
    //             // wir schauen, ob wir schon einen term drin haben, der uns sehr ähnlich sieht - dann nehmen wir den mit dem höchsten score
    //             // (alternativ
    //             // wäre auch der kürzere möglich, aber der könnte einen niederen score haben, und dann später wieder rausfliegen - das würde die
    //             // Qualität
    //             // verschlechtern)
    //             Boolean bBetterSimilarTermInList = false;
    //             if(bSkipSimilarTerms)
    //             {
    //                 for (Entry<Float, String> score2TermInList : tmScore2Term.entryList())
    //                 {
    //                     if(!Levenshtein.isInDistance(score2TermInList.getValue(), strTerm, 3)) continue;
    //                     // wenn der existierende größer ist, dann brauchen wir gar nix eintragen
    //                     if(score2TermInList.getKey() >= fScore)
    //                     {
    //                         bBetterSimilarTermInList = true;
    //                         break;
    //                     }
    //                     // wenn der neue vom score her besser ist, dann müssen wir den austauschen
    //                     tmScore2Term.remove(score2TermInList.getKey(), score2TermInList.getValue());
    //                 }
    //             }
    //
    //             if(bRemoveLastTerm4Score && !bBetterSimilarTermInList) tmScore2Term.remove(tmScore2Term.firstKey());
    //             if(!bBetterSimilarTermInList) tmScore2Term.add(fScore, strTerm);
    //         }
    //
    //         llScore2TermMaps.add(tmScore2Term);
    //     }
    //
    //
    //
    //     return llScore2TermMaps;
    // }







}
