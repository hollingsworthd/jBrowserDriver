package com.machinepublishers.jbrowserdriver;

import javafx.util.Callback;

class DynamicConfirmHandler implements Callback<String, Boolean> {
  private final JBrowserDriver driver;
  private final Object browserContext;

  public DynamicConfirmHandler(final JBrowserDriver driver, final Object browserContext) {
    this.driver = driver;
    this.browserContext = browserContext;
  }

  @Override
  public Boolean call(String arg0) {
    return null;
  }

}
