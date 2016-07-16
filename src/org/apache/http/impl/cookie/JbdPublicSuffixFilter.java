/* 
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2016 Machine Publishers, LLC and the jBrowserDriver contributors
 * https://github.com/MachinePublishers/jBrowserDriver
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
package org.apache.http.impl.cookie;

import org.apache.http.cookie.CommonCookieAttributeHandler;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.cookie.SetCookie;

public class JbdPublicSuffixFilter implements CommonCookieAttributeHandler {

  private CommonCookieAttributeHandler parent;

  public JbdPublicSuffixFilter(CommonCookieAttributeHandler parent) {
    this.parent = parent;
  }

  @Override
  public void parse(SetCookie cookie, String value) throws MalformedCookieException {
    parent.parse(cookie, value);
  }

  @Override
  public void validate(Cookie cookie, CookieOrigin origin) throws MalformedCookieException {
    parent.validate(cookie, origin);
  }

  @Override
  public boolean match(Cookie cookie, CookieOrigin origin) {
    if (cookie.getDomain() == null || cookie.getDomain().isEmpty()) {
      return true;
    }
    return parent.match(cookie, origin);
  }

  @Override
  public String getAttributeName() {
    return parent.getAttributeName();
  }

}
