package de.dfki.km.leech.lucene.basic;





public class Term2FrequencyEntry
{

    public String term;

    public Integer frequency;



    public Term2FrequencyEntry()
    {
    }



    public Term2FrequencyEntry(String strTerm, Integer iFrequency)
    {
        term = strTerm;
        frequency = iFrequency;

    }



    public String getTerm()
    {
        return term;
    }



    public void setTerm(String term)
    {
        this.term = term;
    }



    public Integer getFrequency()
    {
        return frequency;
    }



    public void setFrequency(Integer frequency)
    {
        this.frequency = frequency;
    }



    @Override
    public String toString()
    {
        return "Term:" + getTerm() + " FRQ:" + getFrequency();
    }

}
