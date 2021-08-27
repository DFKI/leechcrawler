package de.dfki.km.leech.elasticsearch;



import com.jayway.jsonpath.JsonPath;
import de.dfki.inquisitor.collections.CollectionUtilz;
import de.dfki.km.leech.metadata.LeechMetadata;
import de.dfki.km.leech.parser.incremental.IncrementalCrawlingHistory;
import de.dfki.km.leech.sax.DataSinkContentHandler;
import de.dfki.km.leech.util.LeechException;
import net.minidev.json.JSONValue;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.apache.tika.metadata.Metadata;

import java.util.HashMap;



public class ToElasticSearchContentHandler extends DataSinkContentHandler
{


    protected HashMap<String, String> m_hsRenameAtts = new HashMap<>();

    protected int m_iCurrentBulkSize = 0;

    protected int m_iMaxBulkSize = 100;

    protected int m_iPort = 9200;

    protected String m_strEsSearchIndex = "";

    protected String m_strEsUrl = "http://localhost";

    protected StringBuilder m_strbCurrentNdJsonBulk = new StringBuilder();



    /**
     * Gives the possibility to rename attributes during indexing. Default is:
     */
    public ToElasticSearchContentHandler attributeRenames(HashMap<String, String> old2newAttNames)
    {
        m_hsRenameAtts = old2newAttNames;

        return this;
    }



    /**
     * Default: 100
     */
    public ToElasticSearchContentHandler bulkSize(int maxBulkSize)
    {
        m_iMaxBulkSize = maxBulkSize;

        return this;
    }



    @Override
    public void crawlFinished()
    {
        try
        {

            if(m_strbCurrentNdJsonBulk.length() == 0)
                return;

            Content returnContent = Request.Put(String.format("http://%s:%s/%s/_bulk", m_strEsUrl, m_iPort, m_strEsSearchIndex))
                    .bodyString(m_strbCurrentNdJsonBulk.toString(), ContentType.APPLICATION_JSON).execute().returnContent();


            Boolean bError = JsonPath.read(returnContent.toString(), "$.errors");
            if(bError)
                throw new LeechException(returnContent.toString());

        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }



    public ToElasticSearchContentHandler index(String esIndexName)
    {
        m_strEsSearchIndex = esIndexName;
        return this;
    }



    /**
     * This is a default attribute name rename set for Tika generated attributes. The attribute target names are for our dynamic mappings. This is for our convinience ;)
     */
    public ToElasticSearchContentHandler leechAttRenames()
    {
        m_hsRenameAtts =
                CollectionUtilz.createHashMap("dc:title", "tns_title", "dc:creator", "tns_creator", "dcterms:modified", "date_modified", "source", "tk_source", "dcterms:created",
                        "date_created", "xmpTPg:NPages", "i_pageCount", "dc:language", "k_language", "body", "tns_body", "location", "g_location");

        return this;
    }



    /**
     * Default: 9200
     */
    public ToElasticSearchContentHandler port(int port)
    {
        m_iPort = port;

        return this;
    }



    @Override
    public void processErrorData(Metadata metadata)
    {

    }



    @Override
    public void processModifiedData(Metadata metadata, String strFulltext)
    {
        try
        {
            // hier gibt es 2 Möglichkeiten: 'index' überschreibt das alte und 'update' überschreibt lediglich die übergebenen fields ('partial update')

            StringBuilder strbLines4Bulk = new StringBuilder();

            String strId = metadata.get(IncrementalCrawlingHistory.dataEntityId);
            if(strId == null)
                strId = metadata.get(LeechMetadata.id);
            if(strId == null)
                strId = metadata.get(Metadata.SOURCE);
            if(strId == null)
                strId = metadata.get(LeechMetadata.RESOURCE_NAME_KEY);

            strbLines4Bulk.append(String.format("{\"update\":{\"_id\":\"%s\"}}\n", JSONValue.escape(strId)));
            strbLines4Bulk.append("{\"doc\":{");
            boolean bFirstKey = true;
            for (String strKey : metadata.names())
            {
                if(!bFirstKey)
                    strbLines4Bulk.append(',');
                String strKeyCleaned = JSONValue.escape(m_hsRenameAtts.getOrDefault(strKey, strKey));
                strbLines4Bulk.append('"').append(strKeyCleaned).append("\":[");

                boolean bFirstVal4Key = true;
                for (String strValue : metadata.getValues(strKey))
                {
                    if(!bFirstVal4Key)
                        strbLines4Bulk.append(',');

                    String strValueCleaned = JSONValue.escape(strValue);
                    strbLines4Bulk.append('"').append(strValueCleaned).append('"');

                    bFirstVal4Key = false;
                }
                strbLines4Bulk.append(']');

                bFirstKey = false;
            }


            if(!bFirstKey)
                strbLines4Bulk.append(',');
            strbLines4Bulk.append('"').append(m_hsRenameAtts.getOrDefault(LeechMetadata.body, LeechMetadata.body)).append('"');
            String strValueCleaned = JSONValue.escape(strFulltext);
            strbLines4Bulk.append(":\"").append(strValueCleaned).append('"');


            strbLines4Bulk.append("}}\n");
            m_strbCurrentNdJsonBulk.append(strbLines4Bulk);

            m_iCurrentBulkSize++;

            // Der String ist fertig, jetzt schicken wir das Zeugs evtl raus
            if(m_iCurrentBulkSize < m_iMaxBulkSize)
                return;


            Content returnContent = Request.Put(String.format("http://%s:%s/%s/_bulk", m_strEsUrl, m_iPort, m_strEsSearchIndex))
                    .bodyString(m_strbCurrentNdJsonBulk.toString(), ContentType.APPLICATION_JSON).execute().returnContent();


            m_strbCurrentNdJsonBulk = new StringBuilder();
            m_iCurrentBulkSize = 0;


            Boolean bError = JsonPath.read(returnContent.toString(), "$.errors");
            if(bError)
                throw new LeechException(returnContent.toString());


        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new RuntimeException(e);
        }



    }



    @Override
    public void processNewData(Metadata metadata, String strFulltext)
    {
        try
        {
            // https://hc.apache.org/httpcomponents-client-4.5.x/quickstart.html
            // https://www.elastic.co/guide/en/elasticsearch/reference/current/getting-started.html#add-data

            // Als erstes erzeugen wir ein NDJSON Objekt und schieben dann mit der Bulk Api alles raus


            // wir brauchen einen _id Eintrag für ES, damit wir damit später deletes und updates machen können
            String strId = metadata.get(IncrementalCrawlingHistory.dataEntityId);
            if(strId == null)
                strId = metadata.get(LeechMetadata.id);
            if(strId == null)
                strId = metadata.get(Metadata.SOURCE);
            if(strId == null)
                strId = metadata.get(LeechMetadata.RESOURCE_NAME_KEY);


            StringBuilder strbLines4Bulk = new StringBuilder();

            if(strId != null)
                strbLines4Bulk.append(String.format( "{\"create\":{\"_id\":\"%s\"}}\n", strId));
            else
                strbLines4Bulk.append("{\"create\":{}}\n"); strbLines4Bulk.append('{');
            boolean bFirstKey = true;
            for (String strKey : metadata.names())
            {
                if(!bFirstKey)
                    strbLines4Bulk.append(',');
                String strKeyCleaned = JSONValue.escape(m_hsRenameAtts.getOrDefault(strKey, strKey));
                strbLines4Bulk.append('"').append(strKeyCleaned).append("\":[");

                boolean bFirstVal4Key = true;
                for (String strValue : metadata.getValues(strKey))
                {
                    if(!bFirstVal4Key)
                        strbLines4Bulk.append(',');

                    String strValueCleaned = JSONValue.escape(strValue);
                    strbLines4Bulk.append('"').append(strValueCleaned).append('"');

                    bFirstVal4Key = false;
                }
                strbLines4Bulk.append(']');

                bFirstKey = false;
            }


            if(!bFirstKey)
                strbLines4Bulk.append(',');
            strbLines4Bulk.append('"').append(m_hsRenameAtts.getOrDefault(LeechMetadata.body, LeechMetadata.body)).append('"');
            String strValueCleaned = JSONValue.escape(strFulltext);
            strbLines4Bulk.append(":\"").append(strValueCleaned).append('"');


            strbLines4Bulk.append("}\n");
            m_strbCurrentNdJsonBulk.append(strbLines4Bulk);

            m_iCurrentBulkSize++;

            // Der String ist fertig, jetzt schicken wir das Zeugs evtl raus
            if(m_iCurrentBulkSize < m_iMaxBulkSize)
                return;


            Content returnContent = Request.Put(String.format("http://%s:%s/%s/_bulk", m_strEsUrl, m_iPort, m_strEsSearchIndex))
                    .bodyString(m_strbCurrentNdJsonBulk.toString(), ContentType.APPLICATION_JSON).execute().returnContent();


            m_strbCurrentNdJsonBulk = new StringBuilder();
            m_iCurrentBulkSize = 0;


            Boolean bError = JsonPath.read(returnContent.toString(), "$.errors");
            if(bError)
                throw new LeechException(returnContent.toString());


        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }



    @Override
    public void processProcessedData(Metadata metadata)
    {

    }



    @Override
    public void processRemovedData(Metadata metadata)
    {
        try
        {

            // { "delete" : { "_index" : "test", "_id" : "2" } }
            String strId = metadata.get(IncrementalCrawlingHistory.dataEntityId);
            if(strId == null)
                strId = metadata.get(LeechMetadata.id);
            if(strId == null)
                strId = metadata.get(Metadata.SOURCE);
            if(strId == null)
                strId = metadata.get(LeechMetadata.RESOURCE_NAME_KEY);

            //TODO sadly, in the bulk api we have no possibility to delete by query - only

            m_strbCurrentNdJsonBulk.append(String.format("{\"delete\":{\"_id\":\"%s\"}}\n", JSONValue.escape(strId)));

            m_iCurrentBulkSize++;

            // Der String ist fertig, jetzt schicken wir das Zeugs evtl raus
            if(m_iCurrentBulkSize < m_iMaxBulkSize)
                return;


            Content returnContent = Request.Put(String.format("http://%s:%s/%s/_bulk", m_strEsUrl, m_iPort, m_strEsSearchIndex))
                    .bodyString(m_strbCurrentNdJsonBulk.toString(), ContentType.APPLICATION_JSON).execute().returnContent();


            m_strbCurrentNdJsonBulk = new StringBuilder();
            m_iCurrentBulkSize = 0;


            Boolean bError = JsonPath.read(returnContent.toString(), "$.errors");
            if(bError)
                throw new LeechException(returnContent.toString());

        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }



    @Override
    public void processUnmodifiedData(Metadata metadata)
    {

    }



    /**
     * Default: localhost
     */
    public ToElasticSearchContentHandler server(String esServerUrl)
    {
        m_strEsUrl = esServerUrl;

        return this;
    }
}
