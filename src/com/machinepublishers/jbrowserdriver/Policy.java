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

import java.io.File;
import java.nio.file.Files;

class Policy {
  static void init() {
    if (System.getSecurityManager() == null) {
      try {
        File policy = File.createTempFile("jbd", ".policy");
        policy.deleteOnExit();
        Files.write(policy.toPath(), "grant{permission java.security.AllPermission;};".getBytes("utf-8"));
        System.setProperty("java.security.policy", policy.getAbsolutePath());
        System.setSecurityManager(new SecurityManager());
      } catch (Throwable t) {
        t.printStackTrace();
      }
    }
  }
}
