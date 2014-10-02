#How to start

**Add Leech to your maven project**

Leech is offered in our own repository. Add following entries to your pom.xml:

    <repositories>
       <repository>
          <id>dfki-km-libs-releases</id>
          <url>http://www.dfki.uni-kl.de/artifactory2/libs-releases</url>
       </repository>
       <repository>
          <id>dfki-km-libs-snapshots</id>
          <url>http://www.dfki.uni-kl.de/artifactory2/libs-snapshots</url>
       </repository>
	</repositories>

and in the \<dependencies\> section

  	<dependency>
  	   <groupId>de.dfki.km</groupId>
  	   <artifactId>leech</artifactId>
  	   <version>1.6.1</version>
  	</dependency>


The version corresponds to the used Tika release version. Currently, the versions 1.3, 1.4, 1.5 and 1.6 are available.

**You can also [download](http://www.dfki.uni-kl.de/leech/free/) all needed libraries in the case you don't use maven.**

**As a next step, try out our [Code snippets / examples](https://github.com/leechcrawler/leech/blob/master/codeSnippets.md) section.**
