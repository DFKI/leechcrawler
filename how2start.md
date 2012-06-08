#How to start

**Add Leech to your maven project**

Leech is offered in our own repository. Add following entries to your pom.xml:

    <repositories>
       <repository>
          <id>dfki-km-libs-releases-local</id>
          <url>http://www.dfki.uni-kl.de/artifactory2/libs-releases-local</url>
       </repository>
       <repository>
          <id>dfki-km-libs-snapshots-local</id>
          <url>http://www.dfki.uni-kl.de/artifactory2/libs-snapshots-local</url>
       </repository>
	</repositories>

and in the \<dependencies\> section

  	<dependency>
  	   <groupId>de.dfki.km</groupId>
  	   <artifactId>leech</artifactId>
  	   <version>0.9-SNAPSHOT</version>
  	</dependency>


**You can also [download](https://github.com/leechcrawler/leech/downloads) all needed libraries.**