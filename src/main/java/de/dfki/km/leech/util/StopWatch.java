package de.dfki.km.leech.util;



import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;



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



    static public void logTime(long timeMillis, Level logLevel)
    {
        Date date = new Date(timeMillis);

        Logger.getLogger(StopWatch.class.getName()).log(logLevel, dateFormat.format(date));
    }



    static public void logTimeDistance(long timeMillisDistance, Level logLevel)
    {
        Logger.getLogger(StopWatch.class.getName()).log(logLevel, formatTimeDistance(timeMillisDistance));
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



    static public long startAndLogTime(Level logLevel)
    {
        long currentTimeMillis = System.currentTimeMillis();

        logTime(currentTimeMillis, logLevel);

        // wir frischen die Zeit auf, damit wir nicht die Verarbeitungszeit für die Ausgabe mit reinrechnen
        currentTimeMillis = System.currentTimeMillis();

        return currentTimeMillis;
    }



    static public long startAndPrintTime()
    {
        long currentTimeMillis = System.currentTimeMillis();

        printTime(currentTimeMillis);

        // wir frischen die Zeit auf, damit wir nicht die Verarbeitungszeit für die Ausgabe mit reinrechnen
        currentTimeMillis = System.currentTimeMillis();

        return currentTimeMillis;
    }



    static public long stopAndLogDistance(long startTime, Level logLevel)
    {
        long currentTimeMillis = System.currentTimeMillis();

        logTimeDistance(currentTimeMillis - startTime, logLevel);

        // wir frischen die Zeit auf, damit wir nicht die Verarbeitungszeit für die Ausgabe mit reinrechnen
        currentTimeMillis = System.currentTimeMillis();

        return currentTimeMillis;
    }



    static public long stopAndPrintDistance(long startTime)
    {
        long currentTimeMillis = System.currentTimeMillis();

        printTimeDistance(currentTimeMillis - startTime);

        // wir frischen die Zeit auf, damit wir nicht die Verarbeitungszeit für die Ausgabe mit reinrechnen
        currentTimeMillis = System.currentTimeMillis();

        return currentTimeMillis;
    }



}
