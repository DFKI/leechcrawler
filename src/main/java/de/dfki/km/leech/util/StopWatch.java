package de.dfki.km.leech.util;



import java.text.SimpleDateFormat;
import java.util.Date;



public class StopWatch
{

    static final protected SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss:SSS");



    static final protected long lDayMs = (24 * 60 * 60 * 1000);

    
    static final protected long lHourMs = (60 * 60 * 1000);


    static final protected long lMinuteMs = (60 * 1000);


    static final protected long lSecondMs = (1000);




    static public String formatTimeDistance(long timeMillisDistance)
    {
        long distanceLeft = timeMillisDistance;

        long lDay = distanceLeft / lDayMs;
        distanceLeft = distanceLeft % lDayMs;
                
        long lHour = distanceLeft / lHourMs;
        distanceLeft = distanceLeft % lHourMs;

        long lMinute = distanceLeft / lMinuteMs;
        distanceLeft = distanceLeft % lMinuteMs;

        long lSecond = distanceLeft / lSecondMs;
        long lMillis = distanceLeft % lSecondMs;

        StringBuilder strbResult = new StringBuilder();
        
        if(lDay != 0) strbResult.append(lDay).append('d');
        if(lHour != 0) strbResult.append(lHour).append('h');
        if(lMinute != 0) strbResult.append(lMinute).append('m');
        if(lSecond != 0) strbResult.append(lSecond).append('s');
        if(lMillis != 0 || strbResult.length() == 0) strbResult.append(lMillis).append("ms");


        return strbResult.toString();
    }



    /**
     * @param args
     * @throws InterruptedException
     */
    public static void main(String[] args) throws InterruptedException
    {

        long lStartTime = StopWatch.stopAndPrintTime();

        Thread.sleep(2512);

        lStartTime = StopWatch.stopAndPrintDistance(lStartTime);




    }



    static public void printTime(long timeMillis)
    {
        Date date = new Date(timeMillis);

        System.out.println(dateFormat.format(date));
    }



    static public void printTimeDistance(long timeMillisDistance)
    {
        System.out.println(formatTimeDistance(timeMillisDistance));
    }



    static public long stopAndPrintDistance(long startTime)
    {
        long currentTimeMillis = System.currentTimeMillis();

        printTimeDistance(currentTimeMillis - startTime);

        // wir frischen die Zeit auf, damit wir nicht die Verarbeitungszeit für die Ausgabe mit reinrechnen
        currentTimeMillis = System.currentTimeMillis();

        return currentTimeMillis;
    }



    static public long stopAndPrintTime()
    {
        long currentTimeMillis = System.currentTimeMillis();

        printTime(currentTimeMillis);

        // wir frischen die Zeit auf, damit wir nicht die Verarbeitungszeit für die Ausgabe mit reinrechnen
        currentTimeMillis = System.currentTimeMillis();

        return currentTimeMillis;
    }

}
