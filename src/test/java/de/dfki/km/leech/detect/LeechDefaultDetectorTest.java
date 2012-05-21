package de.dfki.km.leech.detect;



import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;
import de.dfki.km.leech.Leech;



public class LeechDefaultDetectorTest extends TestCase
{

    public void testDetect() throws IOException
    {
//        LeechDefaultDetector detector = new LeechDefaultDetector();

//        Tika tika = new Tika(detector);
//        Tika tika = new Tika();
        
        
//        System.out.println(tika.detect(new File("/home/reuschling/muell")));
//        System.out.println(tika.detect(new File("/home/reuschling/muell/tricia.jpg")));
//        System.out.println(tika.detect("/home/reuschling/muell"));
        
        Leech leech = new Leech();
        
        System.out.println(leech.detect(new File("/home/reuschling/muell")));
        System.out.println(leech.detect(new File("/home/reuschling/muell/tricia.jpg")));
         
        
        
        
    }
}
