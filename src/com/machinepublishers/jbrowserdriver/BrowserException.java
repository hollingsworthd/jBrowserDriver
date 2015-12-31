/* 
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2016 Machine Publishers, LLC
 * 
 * Sales and support: ops@machinepublishers.com
 * Updates: https://github.com/MachinePublishers/jBrowserDriver
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.machinepublishers.jbrowserdriver;

public class BrowserException {

  /**
   * A problem occurred, but it's likely temporary,
   * so the original operation should simply be retried.
   */
  public static class Retry extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public Retry() {
      super();
    }

    public Retry(Throwable nested) {
      super(nested);
    }

    public Retry(String message) {
      super(message);
    }
  }

  /**
   * A fatal error occurred and merely retrying the operation will likely fail.
   */
  public static class Fatal extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public Fatal() {
      super();
    }

    public Fatal(Throwable nested) {
      super(nested);
    }

    public Fatal(String message) {
      super(message);
    }

    public Fatal(String message, Throwable nested) {
      super(message, nested);
    }
  }

}
