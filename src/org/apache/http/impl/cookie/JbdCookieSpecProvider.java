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
package org.apache.http.impl.cookie;

import org.apache.http.conn.util.PublicSuffixMatcherLoader;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.CookieSpec;
import org.apache.http.cookie.CookieSpecProvider;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.protocol.HttpContext;

public class JbdCookieSpecProvider implements CookieSpecProvider {
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
          final RFC2965Spec strict = new RFC2965Spec(false,
              new RFC2965VersionAttributeHandler(),
              new BasicPathHandler() {
                @Override
                public void validate(
                    final Cookie cookie,
                    final CookieOrigin origin) throws MalformedCookieException {
                  // No validation
                }
              },
              new JbdPublicSuffixFilter(PublicSuffixDomainFilter.decorate(
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
              new RFC2965DiscardAttributeHandler());
          final RFC2109Spec obsoleteStrict = new RFC2109Spec(false,
              new RFC2109VersionHandler(),
              new BasicPathHandler() {
                @Override
                public void validate(
                    final Cookie cookie,
                    final CookieOrigin origin) throws MalformedCookieException {
                  // No validation
                }
              },
              new JbdPublicSuffixFilter(PublicSuffixDomainFilter.decorate(
                  new RFC2109DomainHandler() {
                    @Override
                    public void validate(Cookie cookie, CookieOrigin origin) throws MalformedCookieException {
                      // No validation
                    }
                  }, PublicSuffixMatcherLoader.getDefault())),
              new BasicMaxAgeHandler(),
              new BasicSecureHandler(),
              new BasicCommentHandler());
          final NetscapeDraftSpec netscapeDraft = new NetscapeDraftSpec(
              new JbdPublicSuffixFilter(PublicSuffixDomainFilter.decorate(
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
              new BasicExpiresHandler(DATE_PATTERNS));
          this.cookieSpec = new DefaultCookieSpec(strict, obsoleteStrict, netscapeDraft);
        }
      }
    }
    return this.cookieSpec;
  }
}
