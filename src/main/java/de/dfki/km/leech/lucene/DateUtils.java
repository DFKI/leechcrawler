package de.dfki.km.leech.lucene;



import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;



/**
 * This class gives some utility methods for handling date representations in the lucene index.
 * 
 * @author reuschling
 */
public class DateUtils
{


    static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyyMMddHHmmssSSS");



    /**
     * Converts a date into an lucene-index appropriate number representation. Dates after Christ will be represented as yyyyMMddHHmmssSSS.
     * Dates before Christ (BC) will be represented as -yyyyMMddHHmmssSSS. Thus, it is possible to read the date for debugging purposes directly from
     * the number. Further, the order for sorting and range searches is correct. Last but not least we are able to represent dates in the BC area.
     * Year numbers greater than 9999 will be formatted correctly, e.g. 93045
     * 
     * @param date the date to convert into a number representation
     * 
     * @return the number representation of the date
     */
    public static Long date2Number(Date date)
    {

        Calendar cal = new GregorianCalendar();
        cal.setTime(date);

        String strDate = dateFormatter.format(date);

        Long lDate = Long.valueOf(strDate);

        if(cal.get(Calendar.ERA) == GregorianCalendar.BC) lDate = lDate * -1;


        return lDate;
    }



    /**
     * Calculates the difference between two dates. Works also for dates before 1970. Taken from
     * http://tripoverit.blogspot.com/2007/07/java-calculate-difference-between-two.html. Thanks Ryan
     * 
     * @param date1
     * @param date2
     * 
     * @return the number of days between the two given dates
     */
    public static long daysBetween(final Calendar date1, final Calendar date2)
    {
        Calendar startDate;
        Calendar endDate;
        if(date1.before(date2))
        {
            startDate = date1;
            endDate = date2;
        }
        else
        {
            startDate = date2;
            endDate = date1;
        }




        Calendar sDate = (Calendar) startDate.clone();
        long daysBetween = 0;

        int y1 = sDate.get(Calendar.YEAR);
        int y2 = endDate.get(Calendar.YEAR);
        int m1 = sDate.get(Calendar.MONTH);
        int m2 = endDate.get(Calendar.MONTH);

        // **year optimization**
        while (((y2 - y1) * 12 + (m2 - m1)) > 12)
        {

            // move to Jan 01
            if(sDate.get(Calendar.MONTH) == Calendar.JANUARY && sDate.get(Calendar.DAY_OF_MONTH) == sDate.getActualMinimum(Calendar.DAY_OF_MONTH))
            {

                daysBetween += sDate.getActualMaximum(Calendar.DAY_OF_YEAR);
                sDate.add(Calendar.YEAR, 1);
            }
            else
            {
                int diff = 1 + sDate.getActualMaximum(Calendar.DAY_OF_YEAR) - sDate.get(Calendar.DAY_OF_YEAR);
                sDate.add(Calendar.DAY_OF_YEAR, diff);
                daysBetween += diff;
            }
            y1 = sDate.get(Calendar.YEAR);
        }

        // ** optimize for month **
        // while the difference is more than a month, add a month to start month
        while ((m2 - m1) % 12 > 1)
        {
            daysBetween += sDate.getActualMaximum(Calendar.DAY_OF_MONTH);
            sDate.add(Calendar.MONTH, 1);
            m1 = sDate.get(Calendar.MONTH);
        }

        // process remainder date
        while (sDate.before(endDate))
        {
            sDate.add(Calendar.DAY_OF_MONTH, 1);
            daysBetween++;
        }

        return daysBetween;
    }







    /**
     * Calculates the difference between two dates. Works also for dates before 1970
     * 
     * @param date1
     * @param date2
     * 
     * @return the number of days between the two given dates
     */
    public static long daysBetween(final Date date1, final Date date2)
    {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);

        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);


        return daysBetween(cal1, cal2);
    }



    /**
     * converts a datenumber into a Date Object.
     * 
     * @param dateNumber the datenumber to convert
     * 
     * @return the corresponding Date Object, or null in the case the value was not convertable
     */
    public static Date number2Date(Number dateNumber)
    {
        try
        {

            // das letzte Jahr steht an der length-13ten Stelle. So kriegen wir raus, wieviele y unser parser braucht. Das muß passen, sonst kommt
            // Gülle raus
            String strDateNumber = String.valueOf(dateNumber);
            int iYearLength = strDateNumber.length() - 13;

            StringBuilder strbParseFormat = new StringBuilder();
            for (int i = 0; i < iYearLength; i++)
                strbParseFormat.append('y');
            strbParseFormat.append("MMddHHmmssSSS");

            SimpleDateFormat dateParser = new SimpleDateFormat(strbParseFormat.toString());


            Date date = dateParser.parse(strDateNumber);


            // Wenn wir ein BC-Date haben, müssen wir 1 abziehen - das simpledateformat parsed bei einer negativen Zahl ausgehend von einem
            // Jahr 0. Also verschiebt es sich um 1, da unsere dateNumber dem echten Jahr entspricht (ohne Jahr 0).
            if(dateNumber.doubleValue() < 0)
            {
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) - 1);

                date = cal.getTime();
            }


            return date;


        }
        catch (ParseException e)
        {
            return null;
        }

    }
}
