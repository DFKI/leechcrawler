import ch.qos.logback.classic.AsyncAppender
//import ch.qos.logback.classic.PatternLayout
//import static ch.qos.logback.classic.Level.INFO



scan("30 seconds")

def LOG_PATH = "logs"
def LOG_ARCHIVE = "${LOG_PATH}/archive"
def HOSTNAME=hostname
def appenderList = ["Console-Appender","RollingAsyncFileAppender"]
//def appenderList = ["Console-Appender"]


appender("Console-Appender", ConsoleAppender) {

    encoder(PatternLayoutEncoder) {
				//alternativ %date{ISO8601}
        pattern = "%cyan(%date{dd.MM.yyyy HH:mm:ss}) %highlight(%5level:) %msg %cyan(<< %-40.40logger{50}) [%thread]%n%rEx{50}"

    }

}

//appender("File-Appender", FileAppender) {
//
//    file = "${LOG_PATH}/logfile_${HOSTNAME}.log"
//    append = true
//
//    encoder(PatternLayoutEncoder) {
//        pattern = "%cyan(%date{dd.MM.yyyy HH:mm:ss}) %highlight(%5level:) %msg %cyan(<< %-40.40logger{40}) [%thread]%n%rEx"
//        outputPatternAsHeader = true
//    }
//
//}

appender("RollingFile-Appender", RollingFileAppender) {

    file = "${LOG_PATH}/rollingLogfile_${HOSTNAME}.log"

    rollingPolicy(TimeBasedRollingPolicy) {
        fileNamePattern = "${LOG_ARCHIVE}/rollingLogfile_${HOSTNAME}.%d{yyyy-MM}.log.gz"
        maxHistory = 300000
        totalSizeCap = "1GB"
    }
    encoder(PatternLayoutEncoder) {
        pattern = "%cyan(%date{dd.MM.yyyy HH:mm:ss}) %highlight(%5level:) %msg %cyan(<< %-40.40logger{40}) [%thread]%n%rEx"
    }
}

appender("RollingAsyncFileAppender", AsyncAppender) {

    appenderRef("RollingFile-Appender")

}




//OFF, ERROR, WARN, INFO, DEBUG, TRACE, or ALL
root(INFO, appenderList)

// doesn't work for the initial logback massages: logger("ch.qos.logback.classic.*", OFF, appenderList, false)
// doesn't work for the initial logback massages: logger("ch.qos.logback.core.rolling.*", OFF, appenderList, false)


logger("de.dfki.inquisitor.processes.StopWatch", ALL, appenderList, false)

//logger("org.apache.pdfbox.util.PDFStreamEngine", OFF, appenderList, false)
//logger("org.apache.pdfbox.encoding.Encoding", OFF, appenderList, false)
//logger("org.apache.pdfbox.pdfparser.BaseParser", OFF, appenderList, false)
//logger("org.apache.pdfbox.pdmodel.font.PDSimpleFont", OFF, appenderList, false)
//logger("org.apache.pdfbox.pdfparser.XrefTrailerResolver", OFF, appenderList, false)
//logger("org.apache.pdfbox.filter.FlateFilter", OFF, appenderList, false)
//logger("org.apache.pdfbox.pdfparser.PDFParser", OFF, appenderList, false)
//logger("org.apache.pdfbox.util.operator.SetTextFont", OFF, appenderList, false)
logger("org.apache.pdfbox.*", OFF, appenderList, false)

logger("info.bliki.extensions.scribunto.template.*", OFF, appenderList, false)
logger("info.bliki.wiki.filter.*", OFF, appenderList, false)

logger("com.gargoylesoftware.htmlunit", OFF, appenderList, false)
logger("org.apache.commons.httpclient", OFF, appenderList, false)

logger("info.bliki.extensions.scribunto.template.Invoke", OFF, appenderList, false)
