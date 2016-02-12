# jBrowserDriver
A programmable, embedded web browser driver compatible with the Selenium WebDriver spec -- fast, headless, WebKit-based, 100% pure Java, and no browser dependencies

Licensed under the Apache License v2.0 ([details](https://raw.githubusercontent.com/MachinePublishers/jBrowserDriver/master/LICENSE)).

*Contact ops@machinepublishers.com to sponsor development of new features, have jBrowserDriver integrated into your project, or get warranty & support.*

- - -

#### Download
Get a ZIP archive of the [latest release](https://github.com/MachinePublishers/jBrowserDriver/releases/latest).

Or install via Maven:
```xml
<dependency>
  <groupId>com.machinepublishers</groupId>
  <artifactId>jbrowserdriver</artifactId>
  <version>[0.8.8, 2.0)</version>
</dependency>
```
For other install options, see the [Central Repository](http://search.maven.org/#artifactdetails|com.machinepublishers|jbrowserdriver|0.8.8|jar).

#### Pre-requisites
There's no need to install any web browser and this works fine on a server (headless). Java 8 (Oracle JDK/JRE or OpenJDK) is required.

Linux users: on Debian/Ubuntu install the following, `apt-get install openjdk-8-jdk openjfx`

#### Usage
For specific details, refer to the [API documentation](http://machinepublishers.github.io/jBrowserDriver/).

Use this library like any other Selenium WebDriver or RemoteWebDriver (it implements Selenium's JavascriptExecutor, HasInputDevices, TakesScreenshot, Killable, FindsById, FindsByClassName, FindsByLinkText, FindsByName, FindsByCssSelector, FindsByTagName, and FindsByXPath).

You can optionally create a [Settings](http://machinepublishers.github.io/jBrowserDriver/com/machinepublishers/jbrowserdriver/Settings.html) object, [configure it](http://machinepublishers.github.io/jBrowserDriver/com/machinepublishers/jbrowserdriver/Settings.Builder.html), and pass it to the [JBrowserDriver constructor](http://machinepublishers.github.io/jBrowserDriver/com/machinepublishers/jbrowserdriver/JBrowserDriver.html#JBrowserDriver-com.machinepublishers.jbrowserdriver.Settings-) to specify a proxy, request headers, time zone, user agent, or navigator details. By default, the browser mimics the fingerprint of Tor Browser.

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

#### Global Properties
The following Java system properties can be set:
* `jbd.traceconsole` Mirror trace-level log messages to standard out. Otherwise these logs are only available through the Selenium APIs. Defaults to `false`.
* `jbd.warnconsole` Mirror warning-level log messages to standard error. Otherwise these logs are only available through the Selenium APIs. Defaults to `true`.
* `jbd.wireconsole` Log full requests and responses (except response bodies) to standard out. This produces an enormous amount of output and logs potentially sensitive data--use only as needed. Defaults to `false`.
* `jbd.ports` Ports and port ranges available to run browser processes over RMI. Separate values with comma. Defaults to `10000-10007` (a max of 8 processes).
* `jbd.testport` Port over which a loopback-only webserver is run when testing is performed. Defaults to `9000`.
* `jbd.maxlogs` Maximum number of log entries to store in memory, accessible via the Selenium APIs. Oldest log entry is dropped once max is reached. Regardless of this setting, logs are cleared per instance of JBrowserDriver after a call to quit(), reset(), or Logs.get(String). Defaults to `5000`.
* `jbd.browsergui` Show the browser GUI window. Defaults to `false`.
* `jbd.quickrender` Exclude web page images and binary data from rendering. These resources are still requested and can optionally be saved to disk (see the Settings options). Some versions of Java are inefficient (memory-wise) in rendering images. Defaults to `true`.
* `jbd.blockads` Whether requests to ad/spam servers should be blocked. Based on hosts in ad-hosts.txt in the source tree. Defaults to `true`.
* `jbd.ajaxwait` The idle time (no pending AJAX requests) required in milliseconds before a page is considered to have been loaded completely. For very slow or overloaded CPUs, set a higher value. Defaults to `120`.
* `jbd.ajaxresourcetimeout` The time in milliseconds after which an AJAX request will be ignored when considering whether all AJAX requests have completed. Defaults to `2000`.
* `jbd.pemfile` Specifies a source of trusted certificate authorities. Can take one of four values: (1) `compatible` to accept standard browser certs, (2) `trustanything` to accept any SSL cert, (3) a file path, or (4) a URL. The default when this property is not set is your JRE's keystore, so you can use JDK's keytool to import specific certs.
* `jbd.maxrouteconnections` Maximum number of concurrent connections to a specific host+proxy combo. Defaults to `8`.
* `jbd.maxconnections` Maximum number of concurrent connections overall. Defaults to `3000`.

Example: `java -Djbd.browsergui=true -Djbd.quickrender=false -jar myapp.jar`

#### Building
Install and configure [Maven v3.x](https://maven.apache.org/download.cgi) (which is also available in most Linux package repos) and then from the project root run `mvn clean compile install`. To use in [Eclipse](http://www.eclipse.org/downloads/), either import the existing Java project from the root directory or import the pom.xml file via the [M2E plugin](https://marketplace.eclipse.org/content/maven-integration-eclipse-luna-and-newer). However, if you merely want to use this as a dependency in a separate project, see the [Download](https://github.com/MachinePublishers/jBrowserDriver#download) section. Pull requests are welcome, and we ask contributors to agree to the [CLA](https://github.com/MachinePublishers/jBrowserDriver/blob/master/CLA-individual.txt). Feel free to discuss bugs and new features by opening a [new issue](https://github.com/MachinePublishers/jBrowserDriver/issues/new).

#### To-do
Iframes are not yet handled. Stay tuned.

- - -

Copyright (C) 2014-2016 [Machine Publishers, LLC](https://machinepublishers.com)
