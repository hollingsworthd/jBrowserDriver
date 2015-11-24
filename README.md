# jBrowserDriver
A programmable embedded browser compatible with the WebDriver Selenium spec -- fast, headless, WebKit-based, 100% pure Java, and no browser dependencies

Licensed under the GNU Affero General Public License version 3 with a linking exception for proprietary apps ([details](https://raw.githubusercontent.com/MachinePublishers/jBrowserDriver/master/LICENSE)).

If you find this project useful, then consider hiring the author to make enhancements to it for you. Contact dan@machinepublishers.com

- - -

#### Download
[Latest release](https://github.com/MachinePublishers/jBrowserDriver/releases/latest)

#### Pre-requisites
There's no need to install any web browser and this works fine on a server (headless). Java 8 (Oracle JDK/JRE or OpenJDK) is required.

Linux users: on Debian/Ubuntu install the following, `apt-get install openjdk-8-jdk openjfx`

#### Usage
Use this library like any other Selenium WebDriver or RemoteWebDriver (it implements Selenium's JavascriptExecutor, HasInputDevices, TakesScreenshot, Killable, FindsById, FindsByClassName, FindsByLinkText, FindsByName, FindsByCssSelector, FindsByTagName, and FindsByXPath).

You can optionally pass a [Settings](https://github.com/MachinePublishers/jBrowserDriver/blob/master/browser/src/com/machinepublishers/jbrowserdriver/Settings.java#L193) object to the [JBrowserDriver](https://github.com/MachinePublishers/jBrowserDriver/blob/master/browser/src/com/machinepublishers/jbrowserdriver/JBrowserDriver.java#L96) constructor to specify a proxy, request headers, time zone, user agent, or navigator details. By default, the browser mimics the fingerprint of Tor Browser.

Also, you can run as many instances of JBrowserDriver as you want (it's thread safe), and the browser sessions will be fully isolated from each other when run in headless mode, which is the default (when it's run with the GUI shown, some of the memory between instances is shared out of necessity).

Example:

    import org.openqa.selenium.WebDriver;
    import com.machinepublishers.jbrowserdriver.Timezone;
    import com.machinepublishers.jbrowserdriver.JBrowserDriver;
    import com.machinepublishers.jbrowserdriver.Settings;
    
    public class Example {
      public static void main(String[] args) {

        // You can optionally pass a Settings object here,
        // constructed using Settings.Builder
        WebDriver driver = new JBrowserDriver(Settings.builder().
          timezone(Timezone.AMERICA_NEWYORK).build());

        // This will block for the page load and any
        // associated AJAX requests
        driver.get("http://example.com");

        // You can get status code unlike other Selenium drivers.
        // It blocks for AJAX requests and page loads after clicks 
        // and keyboard events.
        System.out.println(((JBrowserDriver)driver).getStatusCode());

        // Returns the page source in its current state, including
        // any DOM updates that occurred after page load
        System.out.println(driver.getPageSource());
      }
    }
    

#### Global Properties
The following Java system properties can be set:
* `jbd.logconsole` Mirror log output to standard out/error. Otherwise logs are only available through the Selenium APIs. Defaults to `true`.
* `jbd.maxlogs` Maximum number of log entries to store in memory, accessible via the Selenium APIs. Oldest log entry is dropped once max is reached. Regardless of this setting, logs are cleared per instance of JBrowserDriver after a call to quit(), reset(), or Logs.get(String). Defaults to `5000`.
* `jbd.browsergui` Show the browser GUI window. Defaults to `false`.
* `jbd.quickrender` Exclude web page images and binary data from rendering. These resources are still requested and can optionally be saved to disk (see the Settings options). Some versions of Java are inefficient (memory-wise) in rendering images. Defaults to `true`.
* `jbd.blockads` Whether requests to ad/spam servers should be blocked. Based on hosts in ad-hosts.txt in the source tree. Defaults to `true`.
* `jbd.ajaxwait` The idle time (no pending AJAX requests) required in milliseconds before a page is considered to have been loaded completely. For very slow or overloaded CPUs, set a higher value. Defaults to `120`.
* `jbd.ajaxresourcetimeout` The time in milliseconds after which an AJAX request will be ignored when considering whether all AJAX requests have completed. Defaults to `2000`.
* `jbd.pemfile` Specifies a source of trusted certificate authorities. Recommended value is: `'https://raw.githubusercontent.com/bagder/ca-bundle/master/ca-bundle.crt'`. Defaults to the JRE keystore.
* `jbd.maxrouteconnections` Maximum number of concurrent connections to a specific host+proxy combo. Defaults to `8`.
* `jbd.maxconnections` Maximum number of concurrent connections overall. Defaults to `3000`.

Example: `java -Djbd.browsergui=true -Djbd.quickrender=false -jar myapp.jar`

#### Building
The Maven POM file is included. Currently this project is not in Maven Central. Eclipse project files are also included. The head of the source tree is always unstable, please checkout a tagged release for production use.

#### To-do
Iframes and alert dialogs are not yet handled. Stay tuned.

- - -

Copyright (C) 2014-2015 [Machine Publishers, LLC](https://machinepublishers.com)
