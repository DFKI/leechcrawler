package de.dfki.km.leech.util;



/**
 * Class implementing the Levenshtein distance measure to compute letter-related distance measurements between terms
 *  
 * @author Christian Reuschling
 *
 */
public class Levenshtein
{


    /**
     * Gets the minimum of three values
     */
    static private int getMinimum(int a, int b, int c)
    {
        int iMinumum;

        iMinumum = a;
        if(b < iMinumum) iMinumum = b;
        if(c < iMinumum) iMinumum = c;

        return iMinumum;
    }



    /**
     * Computes the Levenshtein distance between two Strings
     * 
     * @param stringFirst the first String to compare
     * @param stringSecond the second String to compare
     */
    static public int getDistance(String stringFirst, String stringSecond)
    {
        if(stringFirst == null) stringFirst = "";
        if(stringSecond == null) stringSecond = "";
        
        // der triviale Fall, wenn ein String leer ist
        int lengthFirst = stringFirst.length();
        int lengthSecond = stringSecond.length();

        if(lengthFirst == 0) return lengthSecond;
        if(lengthSecond == 0) return lengthFirst;


        // initialisieren der Matrix
        int matrix[][] = new int[lengthFirst + 1][lengthSecond + 1];

        for (int i = 0; i <= lengthFirst; i++)
            matrix[i][0] = i;
        for (int z = 0; z <= lengthSecond; z++)
            matrix[0][z] = z;


        // Füllen der Matrix anhand der Unterschiede
        int iCost;
        for (int i = 1; i <= lengthFirst; i++)
        {
            for (int z = 1; z <= lengthSecond; z++)
            {
                if(stringFirst.charAt(i - 1) == stringSecond.charAt(z - 1))
                    iCost = 0;
                else
                    iCost = 1;

                matrix[i][z] = getMinimum(matrix[i - 1][z] + 1, matrix[i][z - 1] + 1, matrix[i - 1][z - 1] + iCost);
            }
        }


        return matrix[lengthFirst][lengthSecond];
    }



    /**
     * Checks whether two Strings are inside a defined distance
     *  
     * @param stringFirst the first String to compare
     * @param stringSecond the second String to compare
     * @param iMaxDistance the maximum distance the two Strings are allowed to have
     * 
     * @return true: the distance between the Strings is less or equal to the allowed maximum distance, false otherwise
     */
    static public boolean isInDistance(String stringFirst, String stringSecond, int iMaxDistance)
    {
        if(Levenshtein.getDistance(stringFirst, stringSecond) <= iMaxDistance) return true;

        return false;
    }







}
