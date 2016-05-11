# jBrowserDriver
A programmable, embedded web browser driver compatible with the Selenium WebDriver spec -- fast, headless, WebKit-based, 100% pure Java, and no browser dependencies

Licensed under the Apache License v2.0 ([details](https://raw.githubusercontent.com/MachinePublishers/jBrowserDriver/master/LICENSE)).

[Machine Publishers, LLC](https://machinepublishers.com/about) provides commercial support and consulting for jBrowserDriver.

- - -

#### Download
Get a ZIP archive of the [latest release](https://github.com/MachinePublishers/jBrowserDriver/releases/latest).

Or install via Maven:
```xml
<dependency>
  <groupId>com.machinepublishers</groupId>
  <artifactId>jbrowserdriver</artifactId>
  <version>[0.13.0, 2.0)</version>
</dependency>
```
For other install options, see the [Central Repository](http://search.maven.org/#artifactdetails|com.machinepublishers|jbrowserdriver|0.13.0|jar).

#### Prerequisites
There's no need to install any web browser and this works fine on a server (headless). Java 8 (Oracle JDK/JRE or OpenJDK) is required.

Currently only Java is supported (Scala, Groovy, etc might have compatibility issues--pull requests or paid support could help address this gap). As a workaround for non-Java tech stacks, Selenium Server is recommended.

Ubuntu 16.04 or Debian Jessie: `sudo apt-get install openjdk-8-jre openjfx`

Ubuntu 14.04: `sudo add-apt-repository ppa:webupd8team/java && sudo apt-get install oracle-java8-installer libgtk2.0 libxtst6 libxslt1.1 fonts-freefont-ttf libasound2`

#### Usage
For specific details, refer to the [API documentation](http://machinepublishers.github.io/jBrowserDriver/).

Use this library like any other Selenium WebDriver or RemoteWebDriver. It also works with Selenium Server (use browser name "jbrowserdriver").

You can optionally create a [Settings](http://machinepublishers.github.io/jBrowserDriver/com/machinepublishers/jbrowserdriver/Settings.html) object, [configure it](http://machinepublishers.github.io/jBrowserDriver/com/machinepublishers/jbrowserdriver/Settings.Builder.html), and pass it to the [JBrowserDriver constructor](http://machinepublishers.github.io/jBrowserDriver/com/machinepublishers/jbrowserdriver/JBrowserDriver.html#JBrowserDriver-com.machinepublishers.jbrowserdriver.Settings-) to specify a proxy, request headers, time zone, user agent, or navigator details. By default, the browser mimics the fingerprint of Tor Browser.

Settings can alternately be configured using Java system properties or Selenium Capabilities. See [Settings builder](http://machinepublishers.github.io/jBrowserDriver/com/machinepublishers/jbrowserdriver/Settings.Builder.html) documentation for details.

Each instance of JBrowserDriver is backed by a separate Java process, so any native browser crashes will not take down your app.

Example:
```java
import org.openqa.selenium.WebDriver;
import com.machinepublishers.jbrowserdriver.Timezone;
import com.machinepublishers.jbrowserdriver.JBrowserDriver;
import com.machinepublishers.jbrowserdriver.Settings;
    
public class Example {
  public static void main(String[] args) {

    // You can optionally pass a Settings object here,
    // constructed using Settings.Builder
    JBrowserDriver driver = new JBrowserDriver(Settings.builder().
      timezone(Timezone.AMERICA_NEWYORK).build());

    // This will block for the page load and any
    // associated AJAX requests
    driver.get("http://example.com");

    // You can get status code unlike other Selenium drivers.
    // It blocks for AJAX requests and page loads after clicks 
    // and keyboard events.
    System.out.println(driver.getStatusCode());

    // Returns the page source in its current state, including
    // any DOM updates that occurred after page load
    System.out.println(driver.getPageSource());
    
    // Close the browser. Allows this thread to terminate.
    driver.quit();
  }
}
```

#### Building
Install and configure [Maven v3.x](https://maven.apache.org/download.cgi) (which is also available in most Linux package repos) and then from the project root run `mvn clean compile install`. To use in [Eclipse](http://www.eclipse.org/downloads/), either import the existing Java project from the root directory or import the pom.xml file via the [M2E plugin](https://marketplace.eclipse.org/content/maven-integration-eclipse-luna-and-newer). However, if you merely want to use this as a dependency in a separate project, see the [Download](https://github.com/MachinePublishers/jBrowserDriver#download) section.

#### Contributing
Pull requests are welcome, and we ask contributors to agree to the [CLA](https://github.com/MachinePublishers/jBrowserDriver/blob/master/CLA-individual.txt). Feel free to discuss bugs and new features by opening a [new issue](https://github.com/MachinePublishers/jBrowserDriver/issues/new).

- - -

Copyright (C) 2014-2016 [Machine Publishers, LLC](https://machinepublishers.com)
