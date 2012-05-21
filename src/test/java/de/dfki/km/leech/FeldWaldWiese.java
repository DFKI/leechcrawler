package de.dfki.km.leech;



import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;



public class FeldWaldWiese
{






    public static void main(String[] args) throws Exception
    {

        System.out.println("FeldWaldWiese");

        URL testBla = new URL("http:///home/reuschling/Projectz/DynaQ/import/xmlRpcDelight");
        
        
        InputStream inputStream = testBla.openStream();
        
        String text = new Scanner( inputStream ).useDelimiter("\\A").next();
        
        System.out.println(text);

    }



}
