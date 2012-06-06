#How to start

**Add Leech to your maven project**

Leech is offered in our own repository. Add following entries to your pom.xml:

    <repositories>
       <repository>
          <url>http://www.dfki.uni-kl.de/artifactory2/libs-releases-local</url>
       </repository>
       <repository>
          <url>http://www.dfki.uni-kl.de/artifactory2/libs-snapshots-local</url>
       </repository>
	</repositories>

and in the \<dependencies\> section

  	<dependency>
  	   <groupId>de.dfki.km</groupId>
  	   <artifactId>leech</artifactId>
  	   <version>0.9-SNAPSHOT</version>
  	</dependency>
