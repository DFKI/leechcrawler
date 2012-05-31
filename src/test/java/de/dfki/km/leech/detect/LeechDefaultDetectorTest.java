/*
    Leech - crawling capabilities for Apache Tika
    
    Copyright (C) 2012 DFKI GmbH, Author: Christian Reuschling

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

    Contact us by mail: christian.reuschling@dfki.de
*/

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
