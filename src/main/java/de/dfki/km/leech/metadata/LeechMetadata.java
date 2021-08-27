package de.dfki.km.leech.metadata;



import de.dfki.km.leech.parser.incremental.IncrementalCrawlingHistory;



public interface LeechMetadata
{

    public static final String childId = "leechChildId";

    public static final String id = "leechId";

    public static final String dataEntityId = IncrementalCrawlingHistory.dataEntityId;

    public static final String dataEntityContentFingerprint = IncrementalCrawlingHistory.dataEntityContentFingerprint;

    public static final String masterDataEntityId = IncrementalCrawlingHistory.masterDataEntityId;

    public static final String parentId = "leechParentId";

    public static final String body = "body";
    
    public static final String isHeuristicPageCount = "isHeuristicPageCount";
    
    public static final String containerSource = "containerSource";
    
    public static final String containerSourceContentType = "containerSourceContentType";

    // depending the Tika version, this is Metadata.RESOURCE_NAME_KEY or LeechMetadata.RESOURCE_NAME_KEY. Sadly they doesn't mark it deprecated and did a breaking change
    public static final String RESOURCE_NAME_KEY = "resourceName";

    

}
