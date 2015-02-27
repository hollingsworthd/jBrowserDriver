# jBrowserDriver
A programmable embedded browser compatible with the WebDriver Selenium spec -- fast, headless, WebKit-based, 100% pure Java, and no browser dependencies

Licensed under the GNU Affero General Public License version 3 ([details](https://raw.githubusercontent.com/MachinePublishers/jBrowserDriver/master/LICENSE)).

Projects utilizing jBrowserDriver must be licensed as Affero GPLv3 except when on-premise or cloud-based services are [purchased](https://screenslicer.com/pricing) from Machine Publishers, LLC. jBrowserDriver is available bundled with [ScreenSlicer](https://github.com/MachinePublishers/ScreenSlicer), a web scraper.

- - -

#### Download
[Latest release](https://github.com/MachinePublishers/jBrowserDriver/releases/latest)

#### Pre-requisites
There's no need to install any web browser and this works fine on a server (headless). Java 8 (Oracle JDK/JRE or OpenJDK) is required. Note to Linux users: JavaFX is needed and it's part of Oracle's standard JRE, but numerous Linux repositories have separated JavaFX in error. In Ubuntu, add the Utopic repo (`deb http://cz.archive.ubuntu.com/ubuntu utopic main universe`) and run `apt-get install openjdk-8-jdk openjfx`

#### Usage
Use this library like any other Selenium WebDriver. You can optionally pass a [Settings](https://github.com/MachinePublishers/jBrowserDriver/blob/master/browser/src/com/machinepublishers/jbrowserdriver/Settings.java) object to the constructor to specify a proxy, request headers, time zone, user agent, or navigator details. By default, the browser mimics the fingerprint of Tor Browser. Note that resources from ad servers are blocked. Also, you can run as many instances of JBrowserDriver as you want (it's thread safe), and the browser sessions will be isolated from each other.

    WebDriver driver = new JBrowserDriver();
    driver.get("http://example.com");
    System.out.println(((JBrowserDriver)driver).getStatusCode()); //yes, you can get status code unlike other Selenium drivers! 
    System.out.println(driver.getPageSource());

#### Global Properties
The following Java system properties can be set:
* `jbd.trace` Log details of every request to standard out. Defaults to `false`.
* `jbd.standarderror` Mirror log output to standard out. Otherwise logs are only available through the Selenium APIs. Defaults to `false`.
* `jbd.browsergui` Show the browser GUI window. Defaults to `false`.
* `jbd.quickrender` Discard web page image data. Recommended because Java is very inefficient (memory-wise) in handling images. Defaults to `true`.
* `jbd.pemfile` Specifies a source of trusted certificate authorities. Otherwise the JRE default keystore is used. Recommended value is: 'https://raw.githubusercontent.com/bagder/ca-bundle/master/ca-bundle.crt'

#### Building
No build scripts yet exist, but Eclipse project files are part of this source tree.

#### To-do
Iframes and alert dialogs are not yet handled. Stay tuned.

- - -

Copyright (C) 2014-2015 [Machine Publishers, LLC](https://machinepublishers.com)
