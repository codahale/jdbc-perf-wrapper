JDBC Performance Wrapper
========================

In your `pom.xml`:
  
    <repositories>
      <repository>
        <id>repo.codahale.com</id>
        <url>http://repo.codahale.com</url>
      </repository>
    </repositories>
    …
    <dependencies>
      <dependency>
        <groupId>com.codahale</groupId>
        <artifactId>jdbc-perf-wrapper</artifactId>
        <version>0.1-SNAPSHOT</version>
      </dependency>
    <dependencies>

Then change your JDBC URL from something like this:
    
    jdbc:mysql://example.com/db_name

To this:
    
    jdbc:perf-mysql://example.com/db_name

And then in your code:
    
    Stopwatch.getInstance().getElapsedTime();
    Stopwatch.getInstance().reset();

For funsies.