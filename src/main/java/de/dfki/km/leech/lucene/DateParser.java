/*
 * Created on Feb 27, 2006
 */
package de.dfki.km.leech.lucene;



import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Logger;



/**
 * Handle parsing of common date format strings. Examples of recognized date strings are: "Fri, 24 Feb 2006 12:14:53 +0000", "Mon, 27 Feb 2006
 * 09:06:43 GMT", "2006-02-27T07:34:00+01:00", "Freitag, 24 Feb 2006 12:14:53 +0100", "Fr, 24 Feb 2006 12:14:53 +0000", "Fr, 24 Februar 2006 12:14:53
 * +0000", "Fr, 24 02 2006 12:14:53 +1100", "Feb 3 1000 12:14:53 +0000", "February 3, 2000 12:15:00 +0000", "3 Feb 2000 12:15:00 +0000", "2000 Feb 3
 * 12:15:00 +0200", "July 4, 1999 12:15:00 +0000", "Tue, Jan 05, 1999 12:15:00 +0000", "2000/05/08 12:15:00 +0000", "2-3-01 12:15:00 +0000", "1-1-2001
 * 12:15:00 +0000", "2001-2-1 12:15:00 -1000", "Oct 12, 2001 12:15:00 +0000", "2001 Oct 12 12:15:00 +0000", "12 2001 Oct 12:15:00 +0000", "1 12 Oct
 * 12:15:00 +0000", "12/27/00 12:15:00 +0000", "8/30/00 12:15:00 +0000", "8/30/99 12:15:00 -0500", "8/30/49 12:15:00 +0000", "8/30/50 12:15:00 +0000",
 * "2000-08-30 12:15:00 +0000", "2000-8-30 12:15:00 +0000", "20000830 12:15:00 +0000", "Feb 1st, 2001 12:15:00 +0000", "Feb 3rd, 2001 12:15:00 +0000",
 * "Dec 5th, 2001 12:15:00 +0000", "Oct 2nd, 2001 12:15:00 +0000"
 * 
 * @author kirchman
 */
public class DateParser
{
    /*
     * Some possible date formats could be: "Feb 3 1000", "February 3, 2000", "3 Feb 2000", "2000 Feb 3", "July 4, 1999", "Tue, Jan 05, 1999",
     * "2000/05/08", "2-3-01", "1-1-2001", "2001-2-1", "Feb 1st, 2001", "Feb 3rd, 2001", "Dec 5th, 2001", "Oct 12th, 2001", "Oct 12, 2001", "2001 Oct
     * 12", "12 2001 Oct", "1 12 Oct", "12/27/00", "8/30/00", "8/30/99", "8/30/49", "8/30/50", "2000-08-30", "2000-8-30", "20000830"
     */
    private static String LOCALE_MARKER = "(*L*)";

    private static Locale[] locales = { Locale.ENGLISH, Locale.GERMAN, Locale.FRENCH };

    // This is special
    private static String[] specialFormats = { "y-M-d'T'H:m:ssz", "y-M-d'T'H:m:s", "yyyyMMddHHmmss", "yyyyMMddHHmm" };

    private static String[] validDates = { "y/M/d", "d-M-y", "y-M-d", "M/d/y", "yMd", "d.M.y", "y.M.d" };

    private static LinkedList<String> llValidFormats;

    private static String[] validLocalDates = { "E, d MMM y", "E, d M y", "MMM d y", "MMM d, y", "d MMM y", "y MMM d", "E, MMM d, y", "d y MMM",
            "y d MMM" };

    private static String[] validTimes = { "H:m:s Z", "H:m:s z", "H:m:s", "H:m", "h:m:s Z", "h:m:s z", "h:m:s", "h:m", "HHmm", "HHmmss" };

    static
    {
        llValidFormats = new LinkedList<String>(); // list containing all valid format strings

        for (int i = 0; i < specialFormats.length; i++)
            llValidFormats.add(specialFormats[i]);


        // einmal mit Zeit

        for (int i = 0; i < validDates.length; i++)
            for (int j = 0; j < validTimes.length; j++)
                llValidFormats.add(validDates[i] + " " + validTimes[j]);

        for (int i = 0; i < validLocalDates.length; i++)
            // mark as: dependent from locale settings
            for (int j = 0; j < validTimes.length; j++)
                llValidFormats.add(LOCALE_MARKER + validLocalDates[i] + " " + validTimes[j]);


        // einmal ohne Zeit

        for (int i = 0; i < validDates.length; i++)
            llValidFormats.add(validDates[i]);

        for (int i = 0; i < validLocalDates.length; i++)
            // mark as: dependent from locale settings
            llValidFormats.add(LOCALE_MARKER + validLocalDates[i]);

        // und nur die Zeit

        for (int j = 0; j < validTimes.length; j++)
            llValidFormats.add(validTimes[j]);

    }



    /**
     * Parse a date format string and create an instance of class Date for the given date string, if possible. Several common date format strings are
     * accepted.
     * 
     * @param toParse the given date string to be parsed
     * @return an instance of Date representing the date of <code>toParse</code>, if possible. <code>null</code> otherwise
     */
    public static Date parseDateString(String toParse)
    {
        return DateParser.parseDateString(toParse, null);
    }



    public static void main(String[] args)
    {
        Date parseDateString = DateParser.parseDateString("2011-11-23 12:43:05");

        System.out.println(parseDateString);
    }




    /**
     * Parse a date format string and create an instance of class Date for the given date string, if possible. Several common date format strings are
     * accepted.
     * 
     * @param toParse the given date string to be parsed
     * @param timeZone2Use4 the time zone that should be used for parsing the String. Can be null in the case you want not set (same as
     *            parseDateString(String toParse)).
     * 
     * @return an instance of Date representing the date of <code>toParse</code>, if possible. <code>null</code> otherwise
     */
    public static Date parseDateString(String toParse, TimeZone timeZone2Use4)
    {

        Date parsedDate = null;

        // wir führen auch noch unser DynaQ-LuceneIndex-Datumsformat ein. Das ist mir negativen Zahlen für Datumsangaben vor Christi. Das machen wir
        // als erstes
        parsedDate = dynaQIndexDateNumber2Date(toParse);
        if(parsedDate != null) return parsedDate;


        for (int i = 0; i < llValidFormats.size(); i++)
        {

            String currentFormat = llValidFormats.get(i);

            if(currentFormat.startsWith(LOCALE_MARKER))
            {
                currentFormat = currentFormat.substring(LOCALE_MARKER.length());
                for (int j = 0; parsedDate == null && j < locales.length; j++)
                {
                    SimpleDateFormat dateFormat = new SimpleDateFormat(currentFormat, locales[j]);
                    if(timeZone2Use4 != null) dateFormat.setTimeZone(timeZone2Use4);

                    dateFormat.setLenient(false);
                    ParsePosition newPosition = new ParsePosition(0);
                    parsedDate = dateFormat.parse(toParse, newPosition);
                }
            }
            else
            {
                SimpleDateFormat dateFormat = new SimpleDateFormat(currentFormat);
                if(timeZone2Use4 != null) dateFormat.setTimeZone(timeZone2Use4);

                dateFormat.setLenient(false);
                ParsePosition newPosition = new ParsePosition(0);
                parsedDate = dateFormat.parse(toParse, newPosition);
            }

            if(parsedDate != null)
            {
                break;
            }
        }

        // special case, unparseable by standard SimpleDateFormat
        if(parsedDate == null && toParse.endsWith(":00")) parsedDate = parseDateString(toParse.substring(0, toParse.length() - 3) + "00"); // simple
                                                                                                                                           // time
        // zones may not contain ':'

        if(parsedDate == null) parsedDate = parseUsingOrdinals(toParse);

        if(parsedDate == null) Logger.getLogger(DateParser.class.getPackage().getName()).warning("Could not parse date string '" + toParse + "'");

        return parsedDate;
    }



    /**
     * Converts a dynaq lucene index datenumber into a Date Object.
     * 
     * @param strDateNumber the datenumber to convert as String
     * 
     * @return the corresponding Date Object, or null in the case the value was not convertable
     */
    protected static Date dynaQIndexDateNumber2Date(String strDateNumber)
    {
        try
        {

            // das letzte Jahr steht an der length-13ten Stelle. So kriegen wir raus, wieviele y unser parser braucht. Das muß passen, sonst kommt
            // Gülle raus
            long dateNumber = Long.valueOf(strDateNumber);
            int iYearLength = strDateNumber.length() - 13;
            if(iYearLength <= 0) return null;

            StringBuilder strbParseFormat = new StringBuilder();
            for (int i = 0; i < iYearLength; i++)
                strbParseFormat.append('y');
            strbParseFormat.append("MMddHHmmssSSS");

            SimpleDateFormat dateParser = new SimpleDateFormat(strbParseFormat.toString());


            Date date = dateParser.parse(strDateNumber);


            // Wenn wir ein BC-Date haben, müssen wir 1 abziehen - das simpledateformat parsed bei einer negativen Zahl ausgehend von einem
            // Jahr 0. Also verschiebt es sich um 1, da unsere dateNumber dem echten Jahr entspricht (ohne Jahr 0).
            if(dateNumber < 0)
            {
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) - 1);

                date = cal.getTime();
            }


            return date;


        }
        catch (Exception e)
        {
            return null;
        }

    }



    /**
     * Try to parse the given string <code>toParse</code> using the format "MMM d'st|nd|rd|th', y" that is: an English date string using ordinals.
     * The consistency of the given date and the ordinal extension is checked (that is: 1th or 2rd is rejected, since the extension does not match the
     * ordinal
     * 
     * @param toParse the string to be parsed
     * @return an instance of Date representing the date of <code>toParse</code>, if parsing using one of the above patterns was possible.
     *         <code>null</code> otherwise
     */
    private static Date parseUsingOrdinals(String toParse)
    {
        Date toReturn = null;
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        String[] ordinalFormats = { "MMM d'st', y", "MMM d'nd', y", "MMM d'rd', y", "MMM d'th', y" };

        for (int i = 0; toReturn == null && i < ordinalFormats.length; i++)
        {
            for (int j = 0; toReturn == null && j < validTimes.length; j++)
            {
                String format = ordinalFormats[i] + " " + validTimes[j];
                SimpleDateFormat dateFormat = new SimpleDateFormat(format, Locale.ENGLISH);
                dateFormat.setLenient(false);
                ParsePosition newPosition = new ParsePosition(0);
                toReturn = dateFormat.parse(toParse, newPosition);
                if(toReturn != null)
                {
                    gregorianCalendar.setTime(toReturn);
                    int theDay = gregorianCalendar.get(Calendar.DAY_OF_MONTH);
                    if((format.indexOf("'st'") != -1 && theDay % 10 != 1) || (format.indexOf("'nd'") != -1 && theDay % 10 != 2)
                            || (format.indexOf("'rd'") != -1 && theDay % 10 != 3) || (format.indexOf("'th'") != -1 && (theDay - 1) % 10 <= 2))
                    {
                        toReturn = null; // invalid ordinal
                        break; // hopelessly unparseable
                    }
                }
            }
        }

        return (toReturn);
    }



}
