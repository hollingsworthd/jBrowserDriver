# jBrowserDriver
A programmable, embeddable web browser driver compatible with the Selenium WebDriver spec -- headless, WebKit-based, pure Java

Licensed under the Apache License v2.0 ([details](https://raw.githubusercontent.com/MachinePublishers/jBrowserDriver/master/LICENSE)).

- - -

## Download
Get a ZIP archive of a [recent release](https://github.com/MachinePublishers/jBrowserDriver/releases/latest).

Or install via Maven:
```xml
<dependency>
  <groupId>com.machinepublishers</groupId>
  <artifactId>jbrowserdriver</artifactId>
  <version>1.0.0-RC1</version>
</dependency>
```
For other install options, see the [Central Repository](http://search.maven.org/#artifactdetails|com.machinepublishers|jbrowserdriver|1.0.0-RC1|jar).

## Prerequisites
Java 8 with JavaFX:
 * Ubuntu Xenial 16.04 LTS, Debian 8 Jessie ([Backports](https://backports.debian.org/Instructions/#index2h2)), Debian 9 Stretch:<br>&nbsp;&nbsp;&nbsp;&nbsp;`sudo apt-get install openjdk-8-jre openjfx`
 * Ubuntu Trusty 14.04 LTS:<br>&nbsp;&nbsp;&nbsp;&nbsp;`sudo add-apt-repository ppa:webupd8team/java && sudo apt-get update && sudo apt-get install oracle-java8-installer libgtk2.0 libxtst6 libxslt1.1 fonts-freefont-ttf libasound2 && sudo update-alternatives --config java`
 * Mac, Windows, Linux:<br>&nbsp;&nbsp;&nbsp;&nbsp;[install Oracle Java 8](http://www.oracle.com/technetwork/java/javase/downloads/index.html) *(note: choose either the JRE or JDK but not the "Server JRE" since it doesn't include JavaFX)*

## Usage
For specific details, refer to the [API documentation](http://machinepublishers.github.io/jBrowserDriver/).

Use this library like any other Selenium WebDriver or RemoteWebDriver. It also works with Selenium Server and Selenium Grid (see example below).

You can optionally create a [Settings](http://machinepublishers.github.io/jBrowserDriver/com/machinepublishers/jbrowserdriver/Settings.html) object, [configure it](http://machinepublishers.github.io/jBrowserDriver/com/machinepublishers/jbrowserdriver/Settings.Builder.html), and pass it to the [JBrowserDriver constructor](http://machinepublishers.github.io/jBrowserDriver/com/machinepublishers/jbrowserdriver/JBrowserDriver.html#JBrowserDriver-com.machinepublishers.jbrowserdriver.Settings-) to specify a proxy, request headers, time zone, user agent, or navigator details. By default, the browser mimics the fingerprint of Tor Browser.

Settings can alternately be configured using Java system properties or Selenium Capabilities. See [Settings builder](http://machinepublishers.github.io/jBrowserDriver/com/machinepublishers/jbrowserdriver/Settings.Builder.html) documentation for details.

Each instance of JBrowserDriver is backed by a separate Java process.

#### Example:
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

#### Running from a remote Selenium server:

You can also run JBrowserDriver from a remotely running Selenium server.

First start the remote selenium server(s):

 * If you are running using Selenium standalone mode:

    Start the standalone server: `java -classpath "selenium-server-standalone-2.53.0.jar:jBrowserDriver-v0.17.0/dist/*" org.openqa.grid.selenium.GridLauncher -browser browserName=jbrowserdriver,version=1,platform=ANY`

 * If you are running using Selenium Grid mode:

    Start the hub: `java -jar selenium-server-standalone-2.53.0.jar -role hub`

    Start the node: `java -classpath "selenium-server-standalone-2.53.0.jar:jBrowserDriver-v0.17.0/dist/*" org.openqa.grid.selenium.GridLauncher -role node http://localhost:4444/grid/register -browser browserName=jbrowserdriver,version=1,platform=ANY`

&nbsp;&nbsp;*On Windows, replace the colon in the classpath with a semi-colon.*

Whether you are using standalone mode or grid mode, you can use this code to call jBrowserDriver remotely:

```java
import java.net.MalformedURLException;
import java.net.URL;
import org.openqa.selenium.Platform;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import com.machinepublishers.jbrowserdriver.Settings;
import com.machinepublishers.jbrowserdriver.Timezone;

public class Example {
  public static void main(String[] args) throws MalformedURLException {
  
    DesiredCapabilities capabilities = 
        new DesiredCapabilities("jbrowserdriver", "1", Platform.ANY);
    
    // Optionally customize the settings
    capabilities.merge(
        Settings.builder().
        timezone(Timezone.AMERICA_NEWYORK).
        buildCapabilities());
    
    RemoteWebDriver driver = new RemoteWebDriver(
        new URL("http://localhost:4444/wd/hub"), capabilities);
    
    driver.get("http://example.com");
    
    System.out.println(driver.getPageSource());
    
    driver.quit();
  }
}
```

## Building
Install and configure [Maven v3.x](https://maven.apache.org/download.cgi) (which is also available in most Linux package repos) and then from the project root run `mvn clean compile install`. To use in [Eclipse](http://www.eclipse.org/downloads/), either import the existing Java project from the root directory or import the Maven pom.xml file. However, if you merely want to use this as a dependency in a separate project, see the [Download](https://github.com/MachinePublishers/jBrowserDriver#download) section.

## Contributing
Pull requests are welcome, and we ask people contributing code to agree to the [CLA](https://github.com/MachinePublishers/jBrowserDriver/blob/master/CLA-rev2-digital.txt) which is similar to the agreement used by the Selenium Project. Signing the CLA is simply a matter of editing the file to add your digital "signature" and adding it to your pull request.

Feel free to discuss bugs and new features by opening a [new issue](https://github.com/MachinePublishers/jBrowserDriver/issues/new).

- - -

Copyright (C) 2014-2017 jBrowserDriver committers
