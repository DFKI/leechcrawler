***
**[How to start](https://github.com/leechcrawler/leech/blob/master/how2start.md) | [Code snippets / Examples](https://github.com/leechcrawler/leech/blob/master/codeSnippets.md) | [Extending LeechCrawler](https://github.com/leechcrawler/leech/blob/master/extending.md) | [Mailing list](https://github.com/leechcrawler/leech/blob/master/mailinglist.md) | [People/Legal Information](https://github.com/leechcrawler/leech/blob/master/people.md) | [Supporters](https://github.com/leechcrawler/leech/blob/master/supporters.md)| [Data Protection](https://github.com/leechcrawler/leech/blob/master/dataprotection.md)**
***

# How to start

**Add LeechCrawler to your maven project**

LeechCrawler is offered in our own repository. Add following entries to your pom.xml:

    <repositories>
        <repository>
            <id>dfki-artifactory-libs-releases</id>
            <url>http://www.dfki.uni-kl.de/artifactory/libs-releases</url>
        </repository>
        <repository>
            <id>dfki-artifactory-libs-snapshots</id>
            <url>http://www.dfki.uni-kl.de/artifactory/libs-snapshots</url>
        </repository>
    </repositories>

and in the \<dependencies\> section

        <dependency>
            <groupId>de.dfki.sds</groupId>
            <artifactId>leechcrawler</artifactId>
            <version>2.0.0</version>
        </dependency>



The version corresponds to the used Tika release version. Currently, these versions are available:

1.3, 1.4, 1.5, 1.6, 1.6.1 (groupId: de.dfki.km, artifactId: leech)

1.7, 1.8, 1.8.1, 1.10.0, 1.10.1, 1.11 (groupId: de.dfki.km, artifactId: leechcrawler)

1.25.0, 1.25.1, 1.26.0, 1.27.0, 2.0.0 (groupId: de.dfki.sds, artifactId: leechcrawler)

**You can also [download](https://cloud.dfki.de/owncloud/index.php/s/CzEY8kyzx2pRAwF) all needed libraries in the case you don't use maven.**

**As a next step, try out our [Code snippets / examples](https://github.com/leechcrawler/leech/blob/master/codeSnippets.md) section.**


