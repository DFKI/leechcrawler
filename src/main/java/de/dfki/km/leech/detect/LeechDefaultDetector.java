package de.dfki.km.leech.detect;



import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.tika.config.ServiceLoader;
import org.apache.tika.detect.CompositeDetector;
import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MimeTypes;



/**
 * A detector implementation that detects everything from the tika DefaultDetector, plus some extra datasource detectors (e.g. for directories)
 * 
 * @author Christian Reuschling, Dipl.Ing.(BA)
 */
public class LeechDefaultDetector extends CompositeDetector
{

    private static final long serialVersionUID = -4879286813440313595L;



    protected static List<Detector> getDefaultDetectors(MimeTypes types, ServiceLoader loader)
    {
        List<Detector> detectors = new ArrayList<Detector>();

        detectors.add(new DirectoryDatasourceDetector());
        detectors.add(new ImapDatasourceDetector());

        detectors.add(types);
        detectors.addAll(loader.loadServiceProviders(Detector.class));


        return detectors;
    }



    public LeechDefaultDetector()
    {
        this(MimeTypes.getDefaultMimeTypes());
    }



    public LeechDefaultDetector(ClassLoader loader)
    {
        this(MimeTypes.getDefaultMimeTypes(), loader);
    }



    public LeechDefaultDetector(MimeTypes types)
    {
        this(types, new ServiceLoader());
    }



    public LeechDefaultDetector(MimeTypes types, ClassLoader loader)
    {
        this(types, new ServiceLoader(loader));
    }



    private LeechDefaultDetector(MimeTypes types, ServiceLoader loader)
    {
        super(types.getMediaTypeRegistry(), getDefaultDetectors(types, loader));

    }
    
    
    
    @Override
    public MediaType detect(InputStream input, Metadata metadata) throws IOException
    {
        // wenn in den Metadaten schon eins drin steht, dann werten wir den stream hier nicht nochmal extra aus
        String strType = metadata.get("Content-Type");
        if(strType != null) return MediaType.parse(strType);
        
        return super.detect(input, metadata);
    }



}
