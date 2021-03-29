package de.dfki.km.leech.parser.incremental;



import java.io.Serializable;



public class DataEntityHistoryEntry implements Serializable
{

    public String dataEntityContentFingerprint;

    public String dataEntityId;

    public String masterDataEntityId;

    public Long lastCrawledTime;



    public DataEntityHistoryEntry(String dataEntityId, String dataEntityContentFingerprint, String masterDataEntityId, Long lastCrawledTime)
    {
        this.dataEntityContentFingerprint = dataEntityContentFingerprint;
        this.dataEntityId = dataEntityId;
        this.masterDataEntityId = masterDataEntityId;
        this.lastCrawledTime = lastCrawledTime;
    }
}
