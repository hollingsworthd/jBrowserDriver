# jBrowserDriver
A programmable embedded browser compatible with the WebDriver Selenium spec -- fast, headless, 100% pure Java

Licensed under the GNU Affero General Public License version 3 ([details](https://raw.githubusercontent.com/MachinePublishers/jBrowserDriver/master/LICENSE)).

Projects utilizing jBrowserDriver must be licensed as Affero GPLv3 except when commercial licensing or service are [purchased](https://screenslicer.com/pricing) from Machine Publishers, LLC.

See our project [ScreenSlicer](https://github.com/MachinePublishers/ScreenSlicer) which uses jBrowserDriver for web scraping.

### Guide
This project is in active development. The API is relatively stable given that it's tied to the Selenium WebDriver API, but it's not ready for heavy production use yet. For instance, proxy support is not yet implemented (aside from JRE-wide proxies), it's vulnerable to tracking, and currently it supports only a single window (no popups or _blank targets). These issues will be addressed within days as development is a top priority.

This project requires Java 8. Note to Linux users: JavaFX is needed and it's part of Oracle's standard JRE, but numerous Linux repositories have separated JavaFX in error. In Ubuntu, add the Utopic repo (`deb http://cz.archive.ubuntu.com/ubuntu utopic main universe`) and run `apt-get install openjdk-8-jdk openjfx`

The primary class here is `JBrowserDriver`. Your code should be something like `WebDriver driver = new JBrowserDriver();` and then use it like any other Selenium WebDriver. A notable addition to the API is ability to get HTTP response codes, `((JBrowserDriver)driver).getStatusCode();`

There are two dependencies, Selenium and Monocle (a headless windowing toolkit). No build script yet exists but the Eclipse project is part of the source tree (just import this project into your Eclipse workspace). And the required JARs are in the `lib` directory. Binary releases will be posted when the project is stable.

It runs headless by default, but for debugging you can force GUI to be shown with JVM argument: `-Dbrowsergui=true`

Currently there's no way to bypass untrusted certificates (a config option will be added), but you can specify trusted PEMs and their source. Recommended is the Mozilla CA list, which can be added with JVM argument: `-Dpemfile='https://raw.githubusercontent.com/bagder/ca-bundle/master/ca-bundle.crt'`

- - -

Copyright (C) 2014-2015 [Machine Publishers, LLC](https://machinepublishers.com)
