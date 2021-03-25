package de.dfki.km.leech.lucene.basic;





public class Term2FrequenciesEntry
{

    public String term;

    public Integer documentFrequency;

    public Long totalIndexFrequency;



    public Term2FrequenciesEntry()
    {
    }



    public Term2FrequenciesEntry(String term, Integer documentFrequency, Long totalIndexFrequency)
    {
        this.term = term;
        this.documentFrequency = documentFrequency;
        this.totalIndexFrequency = totalIndexFrequency;

    }




    @Override
    public String toString()
    {
        return "Term:" + term + " docFRQ:" + documentFrequency + " totalFRQ:" + totalIndexFrequency;
    }

}
