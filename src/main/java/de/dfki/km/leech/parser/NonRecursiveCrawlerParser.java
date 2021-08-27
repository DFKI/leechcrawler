package de.dfki.km.leech.parser;



import de.dfki.inquisitor.collections.MultiValueHashMap;
import de.dfki.km.leech.SubDataEntityContentHandler;
import de.dfki.km.leech.metadata.LeechMetadata;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.xml.sax.ContentHandler;

import java.io.InputStream;
import java.util.Map.Entry;



/**
 * A crawlerParser that simply adds all extracted metadata of subEntities to the handler, without further parsing / recursive call. This is for convinience, if you have
 * simple data containers e.g. big json arrays of csv files that just have to be parsed and can then be written directly to the data sinks. The class ignores the data
 * from the container, e.g. the metadata from the csv file itself. If you want to change this, overwrite
 * {@link #processCurrentDataEntity(InputStream, Metadata, ContentHandler, ParseContext)}<br>
 * <br>
 * Remark: <br>
 * <li>You can ignore using the history for performance reasons if this is necessary. This is the default behaviour. If you want to change this, set
 * {@link #ignoreHistory} to false.<br> <li>The Tika fulltext/body becomes the attribute named {@link LeechMetadata#body} returned by
 * {@link #getSubDataEntitiesInformation(InputStream, ContentHandler, Metadata, ParseContext)} <br>
 * 
 * 
 * @author reuschling
 */
abstract public class NonRecursiveCrawlerParser extends CrawlerParser
{

    private static final long serialVersionUID = 5788180849326209405L;


    public boolean ignoreHistory = true;



    @Override
    protected void processCurrentDataEntity(InputStream stream, Metadata metadata, ContentHandler handler, ParseContext context) throws Exception
    {
        // NOP
    }



    @Override
    protected void processSubDataEntity(MultiValueHashMap<String, Object> subDataEntityInformation, Metadata metadata2use4recursiveCall,
            ContentHandler handler2use4recursiveCall, ParseContext context) throws Exception
    {

        if(subDataEntityInformation == null || subDataEntityInformation.isEmpty()) return;

        StringBuilder strbBody = new StringBuilder();
        boolean bBody = false;
        for (Entry<String, Object> attName2Value : subDataEntityInformation.entryList())
        {
            if(attName2Value.getKey().equals(LeechMetadata.body))
            {
                if(strbBody.length() != 0) strbBody.append("\n\n");
                strbBody.append(attName2Value.getValue().toString());
                bBody = true;
            }
            else
                metadata2use4recursiveCall.add(attName2Value.getKey(), attName2Value.getValue().toString());
        }



        SubDataEntityContentHandler subHandler =
                new SubDataEntityContentHandler(handler2use4recursiveCall, metadata2use4recursiveCall, bBody ? strbBody.toString() : null);

        if(ignoreHistory)
            subHandler.triggerSubDataEntityHandling();
        else
            subHandler.triggerSubDataEntityHandling(context);

    }
}
