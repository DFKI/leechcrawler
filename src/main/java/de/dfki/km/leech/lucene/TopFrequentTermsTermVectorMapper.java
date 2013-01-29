package de.dfki.km.leech.lucene;



import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.lucene.index.TermVectorEntry;
import org.apache.lucene.index.TermVectorMapper;
import org.apache.lucene.index.TermVectorOffsetInfo;

import de.dfki.km.leech.util.MultiValueTreeMap;



public class TopFrequentTermsTermVectorMapper extends TermVectorMapper
{


    String m_strCurrentField = "";

    Set<String> m_straSelectedTerms = new HashSet<String>();;

    int m_iNumTerms = -1;

    boolean m_bStoreOffsets = false;

    boolean m_bStorePositions = false;


    Map<String, MultiValueTreeMap<Integer, TermVectorEntry>> m_hsFieldToTerms = new HashMap<String, MultiValueTreeMap<Integer, TermVectorEntry>>();

    MultiValueTreeMap<Integer, TermVectorEntry> m_sCurrentTreeMapFrq2TermInfo;


    int m_iMinWordLength;

    int m_iMinFrequency;



    public TopFrequentTermsTermVectorMapper()
    {

    }



    @Override
    public boolean isIgnoringOffsets()
    {
        return !m_bStoreOffsets;
    }



    @Override
    public boolean isIgnoringPositions()
    {
        return !m_bStorePositions;
    }



    @Override
    public void map(String term, int frequency, TermVectorOffsetInfo[] offsets, int[] positions)
    {
        // wir filtern nach den gesetzten constraints

        // wenn die Frequenz bei allen schon größer ist, nehmen wir diesen Term nicht auf - ansonsten löschen wir den kleinsten und nehmen den neuen
        // auf
        if(m_sCurrentTreeMapFrq2TermInfo.valueSize() >= m_iNumTerms) if(m_sCurrentTreeMapFrq2TermInfo.firstKey() >= frequency)
        {
            return;
        }

        // wenn der Term zu kurz ist, wird er auch ignoriert
        if(m_iMinWordLength > 0 && term.length() < m_iMinWordLength) return;

        TermVectorEntry entry = new TermVectorEntry(m_strCurrentField, term, frequency, offsets, positions);

        m_sCurrentTreeMapFrq2TermInfo.add(frequency, entry);

        // wenn wir unsere maximalAnzahl Terme überschritten haben, löschen wir einen niedrigfrequenten Term raus
        while (m_sCurrentTreeMapFrq2TermInfo.valueSize() > m_iNumTerms)
        {
            Integer iLowestFRQ = m_sCurrentTreeMapFrq2TermInfo.firstKey();
            Collection<TermVectorEntry> colTermsWithLowestFRQ = m_sCurrentTreeMapFrq2TermInfo.get(iLowestFRQ);
            if(colTermsWithLowestFRQ.size() > 0) m_sCurrentTreeMapFrq2TermInfo.remove(iLowestFRQ, colTermsWithLowestFRQ.iterator().next());
        }

    }



    @Override
    /**
     * Remark: the internal numterms will be only set by first invocation - this is because the upper class invokes this method again while processing
     * reader.getTermFreqVector(..) with the total number of terms from the document...this is not what we want ;) 
     */
    public void setExpectations(String field, int numTerms, boolean storeOffsets, boolean storePositions)
    {
        m_strCurrentField = field;
         if(m_iNumTerms == -1) m_iNumTerms = numTerms;
        // m_iNumTerms = numTerms;
        m_bStoreOffsets = storeOffsets;
        m_bStorePositions = storePositions;

        m_sCurrentTreeMapFrq2TermInfo = new MultiValueTreeMap<Integer, TermVectorEntry>(HashSet.class);
        m_strCurrentField = field;
        m_hsFieldToTerms.put(field, m_sCurrentTreeMapFrq2TermInfo);
    }



    /**
     * Tell the mapper what to expect in regards to field, number of terms, offset and position storage. This method will be called once before
     * retrieving the vector for a field. Additionally to the standard interface, this method enables to specifically select terms that only should be
     * collected. This is to get smaller objects, in order to e.g. send them over a wire for remote access.
     * 
     * This method will be called before {@link #map(String,int,TermVectorOffsetInfo[],int[])}.
     * 
     * @param field The field the vector is for
     * @param iMinFrequency
     * @param iMinWordLength
     * @param iMaxNumberOfTerms
     * @param terms the terms for which information should be collected. All other terms will be ignored.
     * @param numTerms The number of terms that need to be mapped
     * @param storeOffsets true if the mapper should expect offset information
     * @param storePositions true if the mapper should expect positions info
     */
    public void setExpectations(String field, int iMinFrequency, int iMinWordLength, int iMaxNumberOfTerms, boolean storeOffsets,
            boolean storePositions)
    {
        m_strCurrentField = field;
        m_iMinFrequency = iMinFrequency;
        m_iMinWordLength = iMinWordLength;

        m_bStoreOffsets = storeOffsets;
        m_bStorePositions = storePositions;

        this.setExpectations(field, iMaxNumberOfTerms, storeOffsets, storePositions);
    }




    /**
     * Get the mapping between fields and terms, sorted by the comparator
     * 
     * @return A map between field names and {@link java.util.SortedSet}s per field. SortedSet entries are {@link TermVectorEntry}
     */
    public Map<String, List<TermVectorEntry>> getFieldToTerms()
    {
        Map<String, List<TermVectorEntry>> hsField2TermsFinal = new HashMap<String, List<TermVectorEntry>>();

        for (Entry<String, MultiValueTreeMap<Integer, TermVectorEntry>> field2TermSet : m_hsFieldToTerms.entrySet())
        {
            String strField = field2TermSet.getKey();
            MultiValueTreeMap<Integer, TermVectorEntry> frq2TermInfo = field2TermSet.getValue();

            LinkedList<TermVectorEntry> llTermInfos = new LinkedList<TermVectorEntry>(frq2TermInfo.values());

            hsField2TermsFinal.put(strField, llTermInfos);
        }


        return hsField2TermsFinal;
    }



}
