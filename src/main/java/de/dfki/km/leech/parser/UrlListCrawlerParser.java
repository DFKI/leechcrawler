package de.dfki.km.leech.parser;



import de.dfki.inquisitor.collections.MultiValueHashMap;
import de.dfki.km.leech.Leech;
import de.dfki.km.leech.io.URLStreamProvider;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;

import javax.mail.URLName;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;



public class UrlListCrawlerParser extends CrawlerParser
{

    private static final long serialVersionUID = -1061129792080490892L;




    protected Leech m_leech;



    @Override
    protected Iterator<MultiValueHashMap<String, Object>> getSubDataEntitiesInformation(InputStream stream, ContentHandler handler, Metadata metadata,
            ParseContext context) throws Exception
    {

        LinkedList<MultiValueHashMap<String, Object>> llSubEntityData = new LinkedList<>();

        try (Scanner s = new Scanner(stream, StandardCharsets.UTF_8).useDelimiter("\n"))
        {
            while (s.hasNext())
            {
                String strUrl = s.next();

                if(strUrl.startsWith("//")) continue;

                if(!strUrl.contains("://") && !strUrl.startsWith("file:")) strUrl = "file:" + strUrl;

                MultiValueHashMap<String, Object> hsData4Entity = new MultiValueHashMap<>();
                hsData4Entity.add("url", strUrl);


                llSubEntityData.add(hsData4Entity);
            }
        }

        return llSubEntityData.iterator();
    }



    @Override
    public Set<MediaType> getSupportedTypes(ParseContext context)
    {
        return Collections.singleton(new MediaType("application", "leechUrlList"));
    }



    @Override
    protected void processCurrentDataEntity(InputStream stream, Metadata metadata, ContentHandler handler, ParseContext context) throws Exception
    {
        // NOP

    }



    @Override
    protected void processSubDataEntity(MultiValueHashMap<String, Object> subDataEntityInformation, Metadata metadata2use4recursiveCall,
            ContentHandler handler2use4recursiveCall, ParseContext context) throws Exception
    {

        String strUrl = (String) subDataEntityInformation.getFirst("url");

        URLName url = new URLName(strUrl);

        metadata2use4recursiveCall = URLStreamProvider.getURLStreamProvider(url).addFirstMetadata(url, metadata2use4recursiveCall, context);
        InputStream stream = URLStreamProvider.getURLStreamProvider(url).getStream(url, metadata2use4recursiveCall, context);


        try
        {

            if(m_leech == null) m_leech = new Leech();

            LoggerFactory.getLogger(UrlListCrawlerParser.class.getName()).info("Will crawl " + strUrl);

            Parser parser = m_leech.getParser();

            parser.parse(stream, handler2use4recursiveCall, metadata2use4recursiveCall, context);

        }
        finally
        {
            if(stream != null) stream.close();
        }

    }

}
