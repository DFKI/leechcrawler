package de.dfki.km.leech.lucene;



import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.FieldInfo.IndexOptions;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.DefaultSimilarity;

import de.dfki.km.leech.util.Levenshtein;
import de.dfki.km.leech.util.MultiValueTreeMap;



/**
 * The class Buzzwords extracts keywords out of documents - these can be in the form of lucene-documents, which enables to calculate the buzzwords very fast because the
 * most information is still in the lucene index. But also strings can be processed, with an index as a base for calculation
 * 
 * @author Christian Reuschling, Elisabeth Wolf
 * 
 */
public class Buzzwords
{


    static protected DefaultSimilarity m_defaultSimilarity = new DefaultSimilarity();




    /**
     * Adds calculated buzzwords to the given document. The method makes use of the IndexAccessor default Analyzer.
     * 
     * @param iDocNo the lucene document number inside the index behind reader, for the document doc2modify
     * @param doc2modify the document that should enriched with a new buzzword field
     * @param strIdFieldName the attribute name that should be used to identify the documents according to their id String
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
                new DynamicFieldType().setIndexeD(true).setIndexOptionS(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS).setStoreD(true)
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

        TermsEnum termsEnum = termVector.iterator(null);

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







}
