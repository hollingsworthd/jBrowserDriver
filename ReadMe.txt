Add the JARs in the 'dist' directory to your project's classpath.

For general project info, see https://github.com/MachinePublishers/jBrowserDriver

Mavan/Gradle:
  This library is not yet in Maven Central.
  As a workaround, see https://github.com/MachinePublishers/jBrowserDriver/issues/8#issuecomment-143940280
  The dependencies of this library are:
    <dependency>
      <groupId>org.apache.httpcomponents</groupId>
      <artifactId>httpclient</artifactId>
      <version>[4.5.1, 5.0)</version>
    </dependency>
    <dependency>
      <groupId>org.apache.httpcomponents</groupId>
      <artifactId>httpclient-cache</artifactId>
      <version>[4.5.1, 5.0)</version>
    </dependency>
    <dependency>
      <groupId>org.slf4j</groupId>
      <artifactId>slf4j-simple</artifactId>
      <version>1.7.2</version>
    </dependency>
    <dependency>
      <groupId>org.seleniumhq.selenium</groupId>
      <artifactId>selenium-api</artifactId>
      <version>[2.48.2, 3.0)</version>
      <exclusions>
        <exclusion>
            <groupId>*</groupId>
            <artifactId>*</artifactId>
        </exclusion>
      </exclusions>
    </dependency>
    <dependency>
      <groupId>org.zeroturnaround</groupId>
      <artifactId>zt-exec</artifactId>
      <version>[1.8, 2.0)</version>
    </dependency>
    <dependency>
      <groupId>org.zeroturnaround</groupId>
      <artifactId>zt-process</artifactId>
      <version>[1.3, 2.0)</version>
    </dependency>