/* 
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2016 jBrowserDriver committers
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

/*
 * Note: this is a modified and renamed version of
 * org.apache.http.imple.cookie.DefaultCookieSpecProvider
 * which is Copyright 1999-2015 The Apache Software Foundation [http://www.apache.org/]
 * and licensed under the Apache License, Version 2.0 (the "License");
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

import java.lang.reflect.Constructor;

import org.apache.http.conn.util.PublicSuffixMatcherLoader;
import org.apache.http.cookie.CommonCookieAttributeHandler;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.CookieSpec;
import org.apache.http.cookie.CookieSpecProvider;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.cookie.SetCookie;
import org.apache.http.impl.cookie.BasicCommentHandler;
import org.apache.http.impl.cookie.BasicDomainHandler;
import org.apache.http.impl.cookie.BasicExpiresHandler;
import org.apache.http.impl.cookie.BasicMaxAgeHandler;
import org.apache.http.impl.cookie.BasicPathHandler;
import org.apache.http.impl.cookie.BasicSecureHandler;
import org.apache.http.impl.cookie.DefaultCookieSpec;
import org.apache.http.impl.cookie.NetscapeDraftSpec;
import org.apache.http.impl.cookie.PublicSuffixDomainFilter;
import org.apache.http.impl.cookie.RFC2109DomainHandler;
import org.apache.http.impl.cookie.RFC2109Spec;
import org.apache.http.impl.cookie.RFC2109VersionHandler;
import org.apache.http.impl.cookie.RFC2965CommentUrlAttributeHandler;
import org.apache.http.impl.cookie.RFC2965DiscardAttributeHandler;
import org.apache.http.impl.cookie.RFC2965DomainAttributeHandler;
import org.apache.http.impl.cookie.RFC2965PortAttributeHandler;
import org.apache.http.impl.cookie.RFC2965Spec;
import org.apache.http.impl.cookie.RFC2965VersionAttributeHandler;
import org.apache.http.protocol.HttpContext;

class LaxCookieSpecProvider implements CookieSpecProvider {
  private static final String[] DATE_PATTERNS = new String[] {
      "EEE, MMM dd HH:mm:ss yy zzz", "EEE MMM dd HH:mm:ss yy zzz", "EEE, MMM dd HH:mm:ss yy",
      "EEE MMM dd HH:mm:ss yy", "MMM dd HH:mm:ss yy zzz", "MMM dd HH:mm:ss yy",

      "EEE, dd MMM yy HH:mm:ss zzz", "EEE dd MMM yy HH:mm:ss zzz", "EEE, dd MMM yy HH:mm:ss",
      "EEE dd MMM yy HH:mm:ss", "dd MMM yy HH:mm:ss zzz", "dd MMM yy HH:mm:ss",

      "EEE, MMM-dd-yy HH:mm:ss zzz", "EEE MMM-dd-yy HH:mm:ss zzz", "EEE, MMM-dd-yy HH:mm:ss",
      "EEE MMM-dd-yy HH:mm:ss", "MMM-dd-yy HH:mm:ss zzz", "MMM-dd-yy HH:mm:ss",

      "EEE MMM dd yy HH:mm:ss zzz", "EEE MMM dd yy HH:mm:ss",
      "MMM dd yy HH:mm:ss zzz", "MMM dd yy HH:mm:ss",

      "EEE, dd-MMM-yy HH:mm:ss zzz", "EEE dd-MMM-yy HH:mm:ss zzz", "EEE, dd-MMM-yy HH:mm:ss",
      "EEE dd-MMM-yy HH:mm:ss", "dd-MMM-yy HH:mm:ss zzz", "dd-MMM-yy HH:mm:ss",

      "yy-MM-dd'T'HH:mm:ssz", "yy-MM-dd'T'HH:mm:ss", "yy-MM-dd HH:mm:ssz",
      "yy-MM-dd HH:mm:ss", "yy-MM-dd",
  };
  private volatile CookieSpec cookieSpec;

  @Override
  public CookieSpec create(final HttpContext context) {
    if (cookieSpec == null) {
      synchronized (this) {
        if (cookieSpec == null) {
          try {
            Constructor constructor = RFC2965Spec.class.getDeclaredConstructor(boolean.class, CommonCookieAttributeHandler[].class);
            constructor.setAccessible(true);
            final RFC2965Spec strict = (RFC2965Spec) constructor.newInstance(false,
                (Object) new CommonCookieAttributeHandler[] {
                    new RFC2965VersionAttributeHandler(),
                    new BasicPathHandler() {
                      @Override
                      public void validate(
                          final Cookie cookie,
                          final CookieOrigin origin) throws MalformedCookieException {
                        // No validation
                      }
                    },
                    new PublicSuffixFilter(PublicSuffixDomainFilter.decorate(
                        new RFC2965DomainAttributeHandler() {
                          @Override
                          public void validate(Cookie cookie, CookieOrigin origin) throws MalformedCookieException {
                            // No validation
                          }
                        }, PublicSuffixMatcherLoader.getDefault())),
                    new RFC2965PortAttributeHandler(),
                    new BasicMaxAgeHandler(),
                    new BasicSecureHandler(),
                    new BasicCommentHandler(),
                    new RFC2965CommentUrlAttributeHandler(),
                    new RFC2965DiscardAttributeHandler() });
            constructor = RFC2109Spec.class.getDeclaredConstructor(boolean.class, CommonCookieAttributeHandler[].class);
            constructor.setAccessible(true);
            final RFC2109Spec obsoleteStrict = (RFC2109Spec) constructor.newInstance(false,
                (Object) new CommonCookieAttributeHandler[] {
                    new RFC2109VersionHandler(),
                    new BasicPathHandler() {
                      @Override
                      public void validate(
                          final Cookie cookie,
                          final CookieOrigin origin) throws MalformedCookieException {
                        // No validation
                      }
                    },
                    new PublicSuffixFilter(PublicSuffixDomainFilter.decorate(
                        new RFC2109DomainHandler() {
                          @Override
                          public void validate(Cookie cookie, CookieOrigin origin) throws MalformedCookieException {
                            // No validation
                          }
                        }, PublicSuffixMatcherLoader.getDefault())),
                    new BasicMaxAgeHandler(),
                    new BasicSecureHandler(),
                    new BasicCommentHandler() });
            constructor = NetscapeDraftSpec.class.getDeclaredConstructor(CommonCookieAttributeHandler[].class);
            constructor.setAccessible(true);
            final NetscapeDraftSpec netscapeDraft = (NetscapeDraftSpec) constructor.newInstance(
                (Object) new CommonCookieAttributeHandler[] {
                    new PublicSuffixFilter(PublicSuffixDomainFilter.decorate(
                        new BasicDomainHandler() {
                          @Override
                          public void validate(Cookie cookie, CookieOrigin origin) throws MalformedCookieException {
                            // No validation 
                          }
                        }, PublicSuffixMatcherLoader.getDefault())),
                    new BasicPathHandler() {
                      @Override
                      public void validate(
                          final Cookie cookie,
                          final CookieOrigin origin) throws MalformedCookieException {
                        // No validation
                      }
                    },
                    new BasicSecureHandler(),
                    new BasicCommentHandler(),
                    new BasicExpiresHandler(DATE_PATTERNS) });
            constructor = DefaultCookieSpec.class.getDeclaredConstructor(RFC2965Spec.class, RFC2109Spec.class, NetscapeDraftSpec.class);
            constructor.setAccessible(true);
            this.cookieSpec = (DefaultCookieSpec) constructor.newInstance(strict, obsoleteStrict, netscapeDraft);
          } catch (Throwable t) {
            Util.handleException(t);
          }
        }
      }
    }
    return this.cookieSpec;
  }

  private static class PublicSuffixFilter implements CommonCookieAttributeHandler {
    private CommonCookieAttributeHandler parent;

    public PublicSuffixFilter(CommonCookieAttributeHandler parent) {
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
}
