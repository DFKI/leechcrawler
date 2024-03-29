***
**[How to start](https://github.com/leechcrawler/leech/blob/master/how2start.md) | [Code snippets / Examples](https://github.com/leechcrawler/leech/blob/master/codeSnippets.md) | [Extending LeechCrawler](https://github.com/leechcrawler/leech/blob/master/extending.md) | [Mailing list](https://github.com/leechcrawler/leech/blob/master/mailinglist.md) | [People/Legal Information](https://github.com/leechcrawler/leech/blob/master/people.md) | [Supporters](https://github.com/leechcrawler/leech/blob/master/supporters.md)| [Data Protection](https://github.com/leechcrawler/leech/blob/master/dataprotection.md)**
***

# Extending LeechCrawler

**How to write your own Parser**  
Since LeechCrawler deals with the original Tika Parsers, you simply have to write a new Tika Parser. Tika documentation can be found at e.g. [Parser 5min Quick Start Guide](http://tika.apache.org/1.0/parser_guide.html)

**How to write your own MimeType Detector**  
Sometimes specifying a glob pattern inside Tikas custom-mimetypes.xml is not enough for detecting a mimetype, since the glob patterns are a bit limited and glob parsing is only on the URL.getPath() part, which misses relevant parts sometimes. In this case, for detecting your mimetype, you have to write and register your own MimeType Detecor:
 1. Implement the Tika Detector interface. Inside the detect(..) method, return MediaType.OCTET_STREAM in the case you can not make an assumption about the current MimeType, i.e. if the current case is not of your MimeType. You can use the stream or the metadata detected yet (e.g. the file path) for detection.
 2. Register your Detector inside a 'META-INF/services/org.apache.tika.detect.Detector' file. Put that file somewhere into the class path (e.g. with Maven inside 'src/main/resources/META-INF/services/org.apache.tika.detect.Detector'). Just add a line with your Detector class reference, e.g. 'de.dfki.leech.LeechExampleDetector'
 

**How to write your own CrawlerParser**  
To write your own crawler, following steps have to be considered:
 1. In the case you have a new URL protocol (as e.g. imap://), implement a new de.dfki.km.leech.io.URLStreamProvider.
You plug it in by creating a folder '/META-INF/services' with a file named 'de.dfki.km.leech.io.URLStreamProvider' inside your jar
manifest. Each line inside this text file names a URLStreamProvider class
(e.g. de.dfki.km.leech.io.HttpURLStreamProvider).
This is to generate streams and preliminary metadata out of URLs before extraction. You can also set an according mimetype here inside the metadata if this is possible.
 2. If still necessary, register a new mime type in the list of the Tika mimetypes. [How to](http://tika.apache.org/1.0/parser_guide.html#Add_your_MIME-Type)
 3. Create a new implementation of de.dfki.km.leech.parser.CrawlerParser for your mimetype. You will get incremental crawling, crawl interruption, error handling, etc. for free.  

Thus, LeechCrawler supports URLs with your new datasource, including the protocol, connection is according your own url scheme.
In the case you can't or don't want to write everything that is necessary for connection into your URL scheme, write an
own configuration object / parse context class and put it into the parse context parameter. You can consider it both
at URLStreamProvider and CrawlerParser. Your crawler will become fully configurable.

