/*
 * Leech - crawling capabilities for Apache Tika
 * 
 * Copyright (C) 2012 DFKI GmbH, Author: Christian Reuschling
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Contact us by mail: christian.reuschling@dfki.de
 */

package de.dfki.km.leech.util;



import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.Iterator;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



public class StringUtils
{


    public static boolean containsIgnoreCase(String strString2search, String... strings4lookup)
    {
        
        for(String strValue : strings4lookup)
            if(strValue.equalsIgnoreCase(strString2search)) return true;


        return false;
    }



    /**
     * Starts at a given index, looking for a opening bracket. Goes further and returns the index of the corresponding closing bracket. The method
     * supports "()", "{}", and "[]". The method will ignore opening brackets that are excaped with '\'.
     * 
     * @param iStartIndex the index to start from
     * @param strInput the input String
     * 
     * @return the index of the closing bracket corresponding to the first opening bracket after the given start index, -1 in the case there were no
     *         brackets found or there is no matching bracket
     */
    public static int findMatchingBracket(int iStartIndex, String strInput)
    {

        char cOpeningBracket = '-';
        char cClosingBracket = '-';
        char lastChar = '-';

        int iInnerBrackets = 0;

        for (int i = iStartIndex; i < strInput.length(); i++)
        {
            char charAtI = strInput.charAt(i);

            if(cOpeningBracket == '-')
            {
                if(charAtI == '(')
                {
                    cOpeningBracket = '(';
                    cClosingBracket = ')';
                }
                else if(charAtI == '{')
                {
                    cOpeningBracket = '{';
                    cClosingBracket = '}';
                }
                else if(charAtI == '[')
                {
                    cOpeningBracket = '[';
                    cClosingBracket = ']';
                }
            }
            else
            {
                if(charAtI == cOpeningBracket && lastChar != '\\') iInnerBrackets++;

                if(charAtI == cClosingBracket && iInnerBrackets == 0) return i;

                if(charAtI == cClosingBracket) iInnerBrackets--;
            }


            lastChar = charAtI;
        }

        return -1;
    }



    /**
     * Escapes all occurences of the given chars by adding a preceding '\'
     * 
     * @param strOriginString the String you want to escape
     * @param chars2escape the characters you want to escape
     * 
     * @return the escaped String
     */
    static public String escapeChars(String strOriginString, char... chars2escape)
    {
        String strEscapedString = strOriginString;
        for (char char2escape : chars2escape)
            strEscapedString = strEscapedString.replace(String.valueOf(char2escape).toString(), "\\" + char2escape);

        return strEscapedString;
    }



    /**
     * Finds the occurences of a regular expression in a given String
     * 
     * @param strRegEx the regular expression to search
     * @param strInput the input String
     * 
     * @return the Matcher Object, the results can be 'iterated' with 'Matcher.find()' and 'Matcher.toMatchResult();'
     */
    public static Matcher find(String strRegEx, String strInput)
    {
        return Pattern.compile(strRegEx).matcher(strInput);
    }



    /**
     * Gets the first occurence of a regular expression in a given String
     * 
     * @param strRegEx the regular expression to search
     * @param strInput the input String
     * 
     * @return the first occurence, of null in the case there was no match at all
     */
    public static MatchResult findFirst(String strRegEx, String strInput)
    {
        Matcher matcher = Pattern.compile(strRegEx).matcher(strInput);

        if(matcher.find())
            return matcher.toMatchResult();
        else
            return null;
    }



    /**
     * Gets the last occurence of a regular expression in a given String
     * 
     * @param strRegEx the regular expression to search
     * @param strInput the input String
     * 
     * @return the last occurence, of null in the case there was no match at all
     */
    public static MatchResult findLast(String strRegEx, String strInput)
    {
        Matcher matcher = Pattern.compile(strRegEx).matcher(strInput);

        MatchResult lastMatch = null;

        while (matcher.find())
            lastMatch = matcher.toMatchResult();

        return lastMatch;
    }



    /**
     * Checks whether a given String can be parsed as Double
     * 
     * @param string2Check the String you want to check
     * 
     * @return true in the case the String can be parsed as Double
     */
    static public boolean isDouble(String string2Check)
    {
        try
        {
            Double.valueOf(string2Check);

            return true;
        }
        catch (NumberFormatException e)
        {
            return false;
        }
    }



    /**
     * Checks whether a given String can be parsed as Float
     * 
     * @param string2Check the String you want to check
     * 
     * @return true in the case the String can be parsed as Float
     */
    static public boolean isFloat(String string2Check)
    {
        try
        {
            Float.valueOf(string2Check);

            return true;
        }
        catch (NumberFormatException e)
        {
            return false;
        }
    }



    /**
     * Checks whether a given String can be parsed as Integer
     * 
     * @param string2Check the String you want to check
     * 
     * @return true in the case the String can be parsed as Integer
     */
    static public boolean isInteger(String string2Check)
    {
        try
        {
            Integer.valueOf(string2Check);

            return true;
        }
        catch (NumberFormatException e)
        {
            return false;
        }
    }



    /**
     * Checks whether a given String can be parsed as Long
     * 
     * @param string2Check the String you want to check
     * 
     * @return true in the case the String can be parsed as Long
     */
    static public boolean isLong(String string2Check)
    {
        try
        {
            Long.valueOf(string2Check);

            return true;
        }
        catch (NumberFormatException e)
        {
            return false;
        }
    }



    /**
     * Checks whether a String is not null and - in the case it isn't - has more content than whitespace. This is usefull in the case you wnt to
     * ensure there is at least some (non-whitespace) content inside a String.
     * 
     * @param string2check the String you wants to check
     * 
     * @return true in the case there is some none-whitespace content, false otherwise
     */
    static public boolean nullOrWhitespace(String string2check)
    {
        return !(string2check != null && string2check.trim().length() > 0);
    }



    /**
     * Computes the SHA1 hash for the given byte array.
     * 
     * @param bytes the byte array
     * 
     * @return SHA1 hash for the given byte array
     */
    public static String sha1Hash(byte[] bytes)
    {
        try
        {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(bytes);

            byte[] digest = md.digest();

            BigInteger integer = new BigInteger(1, digest);

            return integer.toString(16);


        }
        catch (Exception e)
        {
            return null;
        }
    }



    /**
     * Computes the SHA1 hash for the given string.
     * 
     * @param string The string for which we'd like to get the SHA1 hash.
     * 
     * @return The generated SHA1 hash
     */
    public static String sha1Hash(String string)
    {
        try
        {
            return sha1Hash(string.getBytes());
        }
        catch (Exception e)
        {
            return null;
        }
    }



    /**
     * Unescapes all occurences of the given chars by deleting a preceding '\'
     * 
     * @param strEscapedString the String you want to unescape
     * @param chars2unescape the characters you want to unescape
     * 
     * @return the unescaped String
     */
    static public String unescapeChars(String strEscapedString, char... chars2unescape)
    {
        String strUnescapedString = strEscapedString;
        for (char char2unescape : chars2unescape)
            strUnescapedString = strUnescapedString.replace("\\" + char2unescape, String.valueOf(char2unescape));

        return strUnescapedString;
    }







}
