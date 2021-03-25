package de.dfki.km.leech.lucene.basic;



import de.dfki.inquisitor.collections.TwoValuesBox;
// import de.dfki.inquisitor.lucene.FieldConfig;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.MultiTermQuery.RewriteMethod;
import org.apache.lucene.util.Bits;

import java.io.IOException;
import java.util.*;



public class LuceneUtilz
{




    /**
     * There exists a bug in lucene (at least currently) which yields to the fact that some field attributes are gone if reading a document, which makes re-inserting this
     * document to the index impossible. As workaround we reinsert all attributes with stored values again to the given document object, with the according fieldType from
     * fieldConfig.
     * 
     * @param doc the doc object that should be processed
     */
    static public void reInsertStoredFieldTypes(Document doc, FieldConfig fieldConfig)
    {
        LinkedList<IndexableField> llReInsertFields = new LinkedList<>();

        Iterator<IndexableField> itFields = doc.iterator();
        while (itFields.hasNext())
        {
            IndexableField oldField = itFields.next();

            if(!oldField.fieldType().stored()) continue;

            itFields.remove();

            IndexableField newField;
            if(oldField.fieldType().docValuesType() == DocValuesType.NUMERIC)
                newField = fieldConfig.createField(oldField.name(), oldField.numericValue());
            else
                newField = fieldConfig.createField(oldField.name(), oldField.stringValue());

            llReInsertFields.add(newField);
        }

        for (IndexableField newField : llReInsertFields)
            doc.add(newField);

    }



    /**
     * Extract all the terms in the index matching the query terms. Works also with wildcard queries
     * 
     * @return the terms in the index matching the query terms. Works also with wildcard queries
     */
    @SuppressWarnings("javadoc")
    static public Set<Term> extractQueryTerms(String strQuery, QueryParser queryParser, IndexReader reader)
    {
        try
        {
            Query query = queryParser.parse(strQuery);


            return extractQueryTerms(query, reader);

        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }



    /**
     * Extract all the terms in the index matching the query terms. Works also with wildcard queries
     * 
     * @return the terms in the index matching the query terms. Works also with wildcard queries
     */
    @SuppressWarnings("javadoc")
    static public Set<Term> extractQueryTerms(Query query, IndexReader reader)
    {
        try
        {

            HashSet<Query> subQueries = LuceneUtilz.getSubQueries(query);
            List<TwoValuesBox<MultiTermQuery, RewriteMethod>> llQuery2FormerRewrite = new LinkedList<>();

            for (Query subQuery : subQueries)
            {
                if(subQuery instanceof MultiTermQuery)
                {
                    llQuery2FormerRewrite.add(new TwoValuesBox<MultiTermQuery, RewriteMethod>((MultiTermQuery) subQuery, ((MultiTermQuery) subQuery).getRewriteMethod()));
                    // das brauchen wir, damit Lucene wieder die Terme in BooleanQueries reinmultipliziert (prefixQueries, etc.)
                    ((MultiTermQuery) subQuery).setRewriteMethod(MultiTermQuery.CONSTANT_SCORE_BOOLEAN_REWRITE);
                }
            }

            Query rewritten = query.rewrite(reader);

            HashSet<Term> hsTerms = new HashSet<>();

            Weight rewrittenWeight = rewritten.createWeight(new IndexSearcher(reader), false);
            rewrittenWeight.extractTerms(hsTerms);
            // rewritten.extractTerms(hsTerms);

            // jetzt setzen wir die rewrite Method wieder auf das ursprüngliche zurück
            for (TwoValuesBox<MultiTermQuery, RewriteMethod> subQuery2FormerRewrite : llQuery2FormerRewrite)
                subQuery2FormerRewrite.getFirst().setRewriteMethod(subQuery2FormerRewrite.getSecond());


            return hsTerms;

        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

    }



    public static List<String> analyzeText(String strFieldName, String strText, Analyzer analyzer, int iMaxResults)
    {
        try
        {
            LinkedList<String> llTokenStrings = new LinkedList<>();

            // wir analysieren/normalisieren den Term für den Lookup
            TokenStream tokenstream = analyzer.tokenStream(strFieldName, strText);

            CharTermAttribute termAtt = tokenstream.addAttribute(CharTermAttribute.class);
            tokenstream.reset(); // Resets this stream to the beginning. (Required)

            for (int i = 0; i < iMaxResults; i++)
            {

                if(!tokenstream.incrementToken()) break;

                llTokenStrings.add(termAtt.toString());
            }

            tokenstream.end(); // Perform end-of-stream operations, e.g. set the final offset.
            tokenstream.close(); // Release resources associated with this stream.


            return llTokenStrings;

        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }



    static public Bits bits4Doc(final int iDocId, final int iBitsLength)
    {
        return new Bits()
        {

            @Override
            public boolean get(int index)
            {
                if(index == iDocId)
                    return true;
                else
                    return false;
            }



            @Override
            public int length()
            {
                return iBitsLength;
            }
        };
    }




    static public Bits bits4Docs(final Set<Integer> sDocIds, final int iBitsLength)
    {
        return new Bits()
        {

            @Override
            public boolean get(int index)
            {
                if(sDocIds.contains(index))
                    return true;
                else
                    return false;
            }



            @Override
            public int length()
            {
                return iBitsLength;
            }
        };
    }






    /**
     * This method creates a query out of given text for a specific field, with a given analyzer. The method will create a TermQuery in the case the analyzer did not
     * tokenized the input text, or a PhraseQuery in the case the analyzer did. All values in the query are fully analyzed an this searchable for the given field with
     * respect to the given analyzer.
     * 
     * @return a TermQuery, PhraseQuery or null in the case there was no text left after processing the text with the analyzer
     */
    public static Query createQuery(String strFieldName, String strText, Analyzer analyzer)
    {
        List<String> lAnalyzedText = analyzeText(strFieldName, strText, analyzer, Integer.MAX_VALUE);

        if(lAnalyzedText.size() > 1)
        {
            PhraseQuery pq = new PhraseQuery(strFieldName, lAnalyzedText.toArray(new String[0]));
            // for (String strTerm : lAnalyzedText)
            //     pq.add(new Term(strFieldName, strTerm));

            return pq;
        }
        else if(lAnalyzedText.size() == 1) return new TermQuery(new Term(strFieldName, lAnalyzedText.get(0)));

        return null;
    }



    public static List<Document> getDocsWithTerm(Term term2search, int iMaxResults, IndexSearcher indexSearcher, Set<String> fields2load)
    {

        try
        {
            LinkedList<Document> llDocs = new LinkedList<>();

            TopDocs topDocs = indexSearcher.search(new TermQuery(term2search), iMaxResults);

            for (int i = 0; i < topDocs.scoreDocs.length; i++)
            {

                int doc = topDocs.scoreDocs[i].doc;

                if(fields2load == null)
                    llDocs.add(indexSearcher.doc(doc));
                else
                    llDocs.add(indexSearcher.doc(doc, fields2load));

            }

            return llDocs;

        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

    }



    /**
     * Extracts all subqueries which have a boost factor of a given Query into an array
     * 
     * @param query Query to extract subqueries from
     * @return an array of the subqueries which have a boost factor
     */
    public static Set<BooleanClause> getSubClauses(Query query)
    {
        HashSet<BooleanClause> subqueries = new HashSet<BooleanClause>();

        getSubClauses(query, subqueries);


        return subqueries;
    }



    private static void getSubClauses(Query query, HashSet<BooleanClause> subClauses)
    {
        if(!(query instanceof BooleanQuery)) return;

        BooleanClause[] queryClauses = ((BooleanQuery) query).clauses().toArray(new BooleanClause[0]);

        for (BooleanClause clause : queryClauses)
        {
            subClauses.add(clause);

            if(clause.getQuery() instanceof BooleanQuery) getSubClauses(clause.getQuery(), subClauses);
        }
    }



    /**
     * Extracts all subqueries of a given Query. The given query will also be part of the returned set.
     * 
     * @param query Query to extract subqueries from
     * 
     * @return all subqueries
     */
    public static HashSet<Query> getSubQueries(Query query)
    {
        HashSet<Query> subqueries = new HashSet<Query>();
        getSubQueries(query, subqueries);

        return subqueries;
    }



    protected static void getSubQueries(Query query, HashSet<Query> subQueries)
    {
        if(query instanceof BooleanQuery)
        {
            BooleanClause[] queryClauses = ((BooleanQuery) query).clauses().toArray(new BooleanClause[0]);

            for (int i = 0; i < queryClauses.length; i++)
                getSubQueries(queryClauses[i].getQuery(), subQueries);
        }

        subQueries.add(query);
    }


    //
    // static public int getTermFrq4Doc(Term term, int iDocId, IndexReader reader)
    // {
    //     return getTermFrq4Docs(term, bits4Doc(iDocId, reader.maxDoc()), reader);
    // }
    //
    //
    //
    // static public int getTermFrq4Docs(Term term, Bits docBits, IndexReader reader)
    // {
    //
    //     try
    //     {
    //         DocsEnum docEnum = MultiFields.getTermDocsEnum(reader, docBits, term.field(), term.bytes());
    //         int termFreq = 0;
    //
    //         @SuppressWarnings("unused")
    //         int doc = DocsEnum.NO_MORE_DOCS;
    //         while ((doc = docEnum.nextDoc()) != DocsEnum.NO_MORE_DOCS)
    //         {
    //             termFreq += docEnum.freq();
    //         }
    //
    //
    //         return termFreq;
    //
    //     }
    //     catch (Exception e)
    //     {
    //         throw new RuntimeException(e);
    //     }
    // }
    //
    //
    //
    //
    //
    // static public int getTermFrq4Docs(Term term, Set<Integer> sDocIds, IndexReader reader)
    // {
    //     return getTermFrq4Docs(term, bits4Docs(sDocIds, reader.maxDoc()), reader);
    // }
    //
    //
    //
    //
    // static public int getTermFrq4Index(Term term, IndexReader reader)
    // {
    //     return getTermFrq4Docs(term, MultiFields.getLiveDocs(reader), reader);
    // }



    /**
     * Gets the document object and the index document index/number
     */
    @SuppressWarnings("javadoc")
    public static TwoValuesBox<Document, Integer> getUniqueDocWithTerm(Term idTerm2search, IndexSearcher indexSearcher)
    {
        return getUniqueDocWithTerm(idTerm2search, indexSearcher, null);
    }



    /**
     * Gets the document object and the index document index/number
     */
    @SuppressWarnings("javadoc")
    public static TwoValuesBox<Document, Integer> getUniqueDocWithTerm(Term idTerm2search, IndexSearcher indexSearcher, Set<String> fields2load)
    {

        try
        {
            // XXX hier wollen wir einen einfachen Collecor, wir brauchen keine Scores!
            TopDocs topDocs = indexSearcher.search(new TermQuery(idTerm2search), 1);


            if(topDocs.totalHits == 0) return null;

            if(topDocs.totalHits > 1) throw new IllegalStateException("multiple document entries for ID term search");


            int doc = topDocs.scoreDocs[0].doc;

            Document document;
            if(fields2load == null)
                document = indexSearcher.doc(doc);
            else
                document = indexSearcher.doc(doc, fields2load);

            if(document == null) return null;


            return new TwoValuesBox<Document, Integer>(document, doc);

        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

    }
}
