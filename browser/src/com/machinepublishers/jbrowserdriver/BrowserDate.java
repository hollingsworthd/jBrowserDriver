/*
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2015 Machine Publishers, LLC
 * ops@machinepublishers.com | machinepublishers.com
 * Cincinnati, Ohio, USA
 *
 * You can redistribute this program and/or modify it under the terms of the
 * GNU Affero General Public License version 3 as published by the Free
 * Software Foundation. Additional permissions or commercial licensing may be
 * available--see LICENSE file or contact Machine Publishers, LLC for details.
 *
 * For general details about how to investigate and report license violations,
 * please see: https://www.gnu.org/licenses/gpl-violation.html
 * and email the author: ops@machinepublishers.com
 * Keep in mind that paying customers have more rights than the AGPL alone offers.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License version 3
 * for more details.
 */
package com.machinepublishers.jbrowserdriver;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

/**
 * This class returns a script to override browser timezone.
 * Locale for formatting of date strings will always be en-US
 * until WebKit engine supports Intl classes.
 * Timezone and daylight savings offsets change according to locale.
 */
public class BrowserDate {
  private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-HH-mm");
  static {
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
  }
  private static final Map<Integer, String> offsets = new HashMap<Integer, String>();
  static {
    offsets.put(-36000000, "HAST");
    offsets.put(-32400000, "AKST");
    offsets.put(-28800000, "PST");
    offsets.put(-25200000, "MST");
    offsets.put(-21600000, "CST");
    offsets.put(-18000000, "EST");
    offsets.put(0, "UTC");
    offsets.put(3600000, "CET");
    offsets.put(7200000, "EET");
    offsets.put(10800000, "EAT");
    offsets.put(19800000, "IST");
    offsets.put(21600000, "BST");
    offsets.put(32400000, "JST");
    offsets.put(34200000, "ACST");
    offsets.put(39600000, "SST");
    offsets.put(43200000, "NZST");
    offsets.put(46800000, "MIT");
  }
  private static final Map<String, String> daylightTimzones = new HashMap<String, String>();
  static {
    daylightTimzones.put("HAST", "HADT");
    daylightTimzones.put("AKST", "AKDT");
    daylightTimzones.put("PST", "PDT");
    daylightTimzones.put("MST", "MDT");
    daylightTimzones.put("CST", "CDT");
    daylightTimzones.put("EST", "EDT");
    daylightTimzones.put("UTC", "UTC");
    daylightTimzones.put("CET", "CEST");
    daylightTimzones.put("EET", "EEST");
    daylightTimzones.put("EAT", "EAT");
    daylightTimzones.put("IST", "IST");
    daylightTimzones.put("BST", "BST");
    daylightTimzones.put("JST", "JST");
    daylightTimzones.put("ACST", "ACDT");
    daylightTimzones.put("SST", "SST");
    daylightTimzones.put("NZST", "NZDT");
    daylightTimzones.put("MIT", "MIT");
  }
  public static final BrowserDate UTC = new BrowserDate("UTC");
  public static final BrowserDate AFRICA_ABIDJAN = new BrowserDate("Africa/Abidjan");
  public static final BrowserDate AFRICA_ACCRA = new BrowserDate("Africa/Accra");
  public static final BrowserDate AFRICA_ADDISABABA = new BrowserDate("Africa/Addis_Ababa");
  public static final BrowserDate AFRICA_ALGIERS = new BrowserDate("Africa/Algiers");
  public static final BrowserDate AFRICA_CAIRO = new BrowserDate("Africa/Cairo");
  public static final BrowserDate AFRICA_CASABLANCA = new BrowserDate("Africa/Casablanca");
  public static final BrowserDate AFRICA_DARESSALAAM = new BrowserDate("Africa/Dar_es_Salaam");
  public static final BrowserDate AFRICA_FREETOWN = new BrowserDate("Africa/Freetown");
  public static final BrowserDate AFRICA_JOHANNESBURG = new BrowserDate("Africa/Johannesburg");
  public static final BrowserDate AFRICA_KHARTOUM = new BrowserDate("Africa/Khartoum");
  public static final BrowserDate AFRICA_KINSHASA = new BrowserDate("Africa/Kinshasa");
  public static final BrowserDate AFRICA_LAGOS = new BrowserDate("Africa/Lagos");
  public static final BrowserDate AFRICA_MOGADISHU = new BrowserDate("Africa/Mogadishu");
  public static final BrowserDate AFRICA_NAIROBI = new BrowserDate("Africa/Nairobi");
  public static final BrowserDate AFRICA_TRIPOLI = new BrowserDate("Africa/Tripoli");
  public static final BrowserDate AMERICA_ANCHORAGE = new BrowserDate("America/Anchorage");
  public static final BrowserDate AMERICA_BELIZE = new BrowserDate("America/Belize");
  public static final BrowserDate AMERICA_BOGOTA = new BrowserDate("America/Bogota");
  public static final BrowserDate AMERICA_CANCUN = new BrowserDate("America/Cancun");
  public static final BrowserDate AMERICA_CAYMAN = new BrowserDate("America/Cayman");
  public static final BrowserDate AMERICA_CHICAGO = new BrowserDate("America/Chicago");
  public static final BrowserDate AMERICA_COSTARICA = new BrowserDate("America/Costa_Rica");
  public static final BrowserDate AMERICA_DENVER = new BrowserDate("America/Denver");
  public static final BrowserDate AMERICA_GUATEMALA = new BrowserDate("America/Guatemala");
  public static final BrowserDate AMERICA_JAMAICA = new BrowserDate("America/Jamaica");
  public static final BrowserDate AMERICA_LIMA = new BrowserDate("America/Lima");
  public static final BrowserDate AMERICA_LOSANGELES = new BrowserDate("America/Los_Angeles");
  public static final BrowserDate AMERICA_MEXICOCITY = new BrowserDate("America/Mexico_City");
  public static final BrowserDate AMERICA_MONTERREY = new BrowserDate("America/Monterrey");
  public static final BrowserDate AMERICA_MONTREAL = new BrowserDate("America/Montreal");
  public static final BrowserDate AMERICA_NEWYORK = new BrowserDate("America/New_York");
  public static final BrowserDate AMERICA_PANAMA = new BrowserDate("America/Panama");
  public static final BrowserDate AMERICA_PHOENIX = new BrowserDate("America/Phoenix");
  public static final BrowserDate AMERICA_TIJUANA = new BrowserDate("America/Tijuana");
  public static final BrowserDate AMERICA_TORONTO = new BrowserDate("America/Toronto");
  public static final BrowserDate AMERICA_VANCOUVER = new BrowserDate("America/Vancouver");
  public static final BrowserDate AMERICA_WINNIPEG = new BrowserDate("America/Winnipeg");
  public static final BrowserDate ASIA_BEIRUT = new BrowserDate("Asia/Beirut");
  public static final BrowserDate ASIA_CALCUTTA = new BrowserDate("Asia/Calcutta");
  public static final BrowserDate ASIA_DAMASCUS = new BrowserDate("Asia/Damascus");
  public static final BrowserDate ASIA_DHAKA = new BrowserDate("Asia/Dhaka");
  public static final BrowserDate ASIA_ISTANBUL = new BrowserDate("Asia/Istanbul");
  public static final BrowserDate ASIA_NOVOSIBIRSK = new BrowserDate("Asia/Novosibirsk");
  public static final BrowserDate ASIA_QATAR = new BrowserDate("Asia/Qatar");
  public static final BrowserDate ASIA_SEOUL = new BrowserDate("Asia/Seoul");
  public static final BrowserDate ASIA_TELAVIV = new BrowserDate("Asia/Tel_Aviv");
  public static final BrowserDate ASIA_TOKYO = new BrowserDate("Asia/Tokyo");
  public static final BrowserDate EUROPE_AMSTERDAM = new BrowserDate("Europe/Amsterdam");
  public static final BrowserDate EUROPE_ATHENS = new BrowserDate("Europe/Athens");
  public static final BrowserDate EUROPE_BERLIN = new BrowserDate("Europe/Berlin");
  public static final BrowserDate EUROPE_BRUSSELS = new BrowserDate("Europe/Brussels");
  public static final BrowserDate EUROPE_BUCHAREST = new BrowserDate("Europe/Bucharest");
  public static final BrowserDate EUROPE_BUDAPEST = new BrowserDate("Europe/Budapest");
  public static final BrowserDate EUROPE_COPENHAGEN = new BrowserDate("Europe/Copenhagen");
  public static final BrowserDate EUROPE_ISTANBUL = new BrowserDate("Europe/Istanbul");
  public static final BrowserDate EUROPE_KIEV = new BrowserDate("Europe/Kiev");
  public static final BrowserDate EUROPE_LONDON = new BrowserDate("Europe/London");
  public static final BrowserDate EUROPE_MADRID = new BrowserDate("Europe/Madrid");
  public static final BrowserDate EUROPE_MINSK = new BrowserDate("Europe/Minsk");
  public static final BrowserDate EUROPE_MOSCOW = new BrowserDate("Europe/Moscow");
  public static final BrowserDate EUROPE_PARIS = new BrowserDate("Europe/Paris");
  public static final BrowserDate EUROPE_PRAGUE = new BrowserDate("Europe/Prague");
  public static final BrowserDate EUROPE_ROME = new BrowserDate("Europe/Rome");
  public static final BrowserDate EUROPE_SOFIA = new BrowserDate("Europe/Sofia");
  public static final BrowserDate EUROPE_STOCKHOLM = new BrowserDate("Europe/Stockholm");
  public static final BrowserDate EUROPE_VIENNA = new BrowserDate("Europe/Vienna");
  public static final BrowserDate EUROPE_WARSAW = new BrowserDate("Europe/Warsaw");
  public static final BrowserDate EUROPE_ZURICH = new BrowserDate("Europe/Zurich");
  public static final BrowserDate PACIFIC_AUCKLAND = new BrowserDate("Pacific/Auckland");
  public static final BrowserDate PACIFIC_FIJI = new BrowserDate("Pacific/Fiji");
  public static final BrowserDate PACIFIC_HONOLULU = new BrowserDate("Pacific/Honolulu");
  public static final Set<BrowserDate> ALL_ZONES;
  static {
    Set<BrowserDate> tmp = new HashSet<BrowserDate>();
    Field[] fields = BrowserDate.class.getDeclaredFields();
    for (int i = 0; i < fields.length; i++) {
      try {
        Object obj = fields[i].get(null);
        if (obj instanceof BrowserDate) {
          tmp.add((BrowserDate) fields[i].get(null));
        }
      } catch (Throwable t) {}
    }
    ALL_ZONES = Collections.unmodifiableSet(tmp);
  }

  private String script;
  private final String timeZoneName;

  private BrowserDate(String timeZoneName) {
    this.timeZoneName = timeZoneName;
  }

  private static String timeZoneDesc(boolean daylight, int rawOffset, int timeZoneMinutes, int daylightMinutes) {
    int totalOffsetMinutes = timeZoneMinutes - (daylight ? daylightMinutes : 0);
    int formattedOffsetHours = totalOffsetMinutes / 60;
    int formattedOffsetMinutes = (formattedOffsetHours * 60) - totalOffsetMinutes;
    String timeZoneDesc = (totalOffsetMinutes < 0 ? "+" : "-")
        + (formattedOffsetHours < 10 ? "0" + formattedOffsetHours : "" + formattedOffsetHours)
        + (formattedOffsetMinutes == 0 ? "00"
            : (formattedOffsetMinutes < 10 ? "0" + formattedOffsetMinutes : formattedOffsetMinutes));
    return daylight ? timeZoneDesc + " (" + daylightTimzones.get(offsets.get(rawOffset)) + ")"
        : timeZoneDesc + " (" + offsets.get(rawOffset) + ")";
  }

  private void init() {
    TimeZone timeZone = TimeZone.getTimeZone(timeZoneName);

    int[][] daylightSavings = daylightSavings(timeZone);
    int[] daylightSavingsStart = daylightSavings == null ? null : daylightSavings[0];
    int[] daylightSavingsEnd = daylightSavings == null ? null : daylightSavings[1];

    int timeZoneMinutes = -1 * timeZone.getRawOffset() / 1000 / 60;
    int daylightMinutes = timeZone.getDSTSavings() / 1000 / 60;

    String timeZoneDesc = timeZoneDesc(false, timeZone.getRawOffset(), timeZoneMinutes, daylightMinutes);
    String timeZoneDescDaylight = timeZoneDesc(true, timeZone.getRawOffset(), timeZoneMinutes, daylightMinutes);

    StringBuilder builder = new StringBuilder();
    if (daylightSavingsStart == null || daylightSavingsEnd == null) {
      builder.append("var isDaylightSavings = false;");
    } else {
      builder.append("var start = tmpDate.getUTCMonth() > " + daylightSavingsStart[0] + "? 8");
      builder.append(": (tmpDate.getUTCMonth() < " + daylightSavingsStart[0] + "? -8 : 0);");
      builder.append("start += tmpDate.getUTCDate() > " + daylightSavingsStart[1] + "? 4");
      builder.append(": (tmpDate.getUTCDate() < " + daylightSavingsStart[1] + "? -4 : 0);");
      builder.append("start += tmpDate.getUTCHours() > " + daylightSavingsStart[2] + "? 2");
      builder.append(": (tmpDate.getUTCHours() < " + daylightSavingsStart[2] + "? -2 : 0);");
      builder.append("start += tmpDate.getUTCMinutes() > " + daylightSavingsStart[3] + "? 1");
      builder.append(": (tmpDate.getUTCMinutes() < " + daylightSavingsStart[3] + "? -1 : 0);");
      builder.append("var end = tmpDate.getUTCMonth() < " + daylightSavingsEnd[0] + "? 8");
      builder.append(": (tmpDate.getUTCMonth() > " + daylightSavingsEnd[0] + "? -8 : 0);");
      builder.append("end += tmpDate.getUTCDate() < " + daylightSavingsEnd[1] + "? 4");
      builder.append(": (tmpDate.getUTCDate() > " + daylightSavingsEnd[1] + "? -4 : 0);");
      builder.append("end += tmpDate.getUTCHours() < " + daylightSavingsEnd[2] + "? 2");
      builder.append(": (tmpDate.getUTCHours() > " + daylightSavingsEnd[2] + "? -2 : 0);");
      builder.append("end += tmpDate.getUTCMinutes() < " + daylightSavingsEnd[3] + "? 1");
      builder.append(": (tmpDate.getUTCMinutes() > " + daylightSavingsEnd[3] + "? -1 : 0);");
      builder.append("var isDaylightSavings = start > 0 && end > 0;");
    }
    String isDaylightSavings = builder.toString();
    builder = new StringBuilder();

    builder.append("var timeZoneDesc = '" + timeZoneDesc + "';");
    builder.append("if(isDaylightSavings){");
    builder.append("timeZoneDesc = '" + timeZoneDescDaylight + "';");
    builder.append("}");
    String timeZoneDescExpr = builder.toString();
    builder = new StringBuilder();

    builder.append("var tmpDate = new Date(this.getTime() " + timeZone.getRawOffset() + ");");
    builder.append(isDaylightSavings);
    builder.append("if(isDaylightSavings){");
    builder.append("  tmpDate = new Date(tmpDate.getTime() + " + timeZone.getDSTSavings() + ");");
    builder.append("}");
    String tmpDate = builder.toString();
    builder = new StringBuilder();

    builder.append("var weekday = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];");
    builder.append("var month = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', "
        + "'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];");
    String weekdayAndMonthArrays = builder.toString();
    builder = new StringBuilder();

    builder.append("var minutes = tmpDate.getUTCMinutes();");
    builder.append("minutes = minutes < 10? '0'+minutes : minutes;");
    builder.append("var seconds = tmpDate.getUTCSeconds();");
    builder.append("seconds = seconds < 10? '0'+seconds : seconds;");
    builder.append("var hours = tmpDate.getUTCHours();");
    builder.append("var amPM = hours < 12? 'AM' : 'PM';");
    builder.append("hours = hours % 12;");
    builder.append("hours = hours == 0? 12 : hours;");
    String time12Hour = builder.toString();
    builder = new StringBuilder();

    builder.append("var minutes = tmpDate.getUTCMinutes();");
    builder.append("minutes = minutes < 10? '0'+minutes : minutes;");
    builder.append("var seconds = tmpDate.getUTCSeconds();");
    builder.append("seconds = seconds < 10? '0'+seconds : seconds;");
    builder.append("var hours = tmpDate.getUTCHours();");
    builder.append("hours = hours < 10? '0' + hours : hours;");
    String time24Hour = builder.toString();
    builder = new StringBuilder();

    builder.append("Date.prototype.getTimezoneOffset = function(){");
    builder.append(isDaylightSavings);
    builder.append("if(isDaylightSavings){");
    builder.append(" return " + timeZoneMinutes + " - " + daylightMinutes);
    builder.append("}");
    builder.append("return " + timeZoneMinutes + ";");
    builder.append("};");

    builder.append("Date.prototype.getFullYear = function(){");
    builder.append(tmpDate);
    builder.append("return tmpDate.getUTCFullYear();");
    builder.append("};");

    builder.append("Date.prototype.getYear = function(){");
    builder.append(tmpDate);
    builder.append("return tmpDate.getUTCYear();");
    builder.append("};");

    builder.append("Date.prototype.getMonth = function(){");
    builder.append(tmpDate);
    builder.append("return tmpDate.getUTCMonth();");
    builder.append("};");

    builder.append("Date.prototype.getDate = function(){");
    builder.append(tmpDate);
    builder.append("return tmpDate.getUTCDate();");
    builder.append("};");

    builder.append("Date.prototype.getDay = function(){");
    builder.append(tmpDate);
    builder.append("return tmpDate.getUTCDay();");
    builder.append("};");

    builder.append("Date.prototype.getHours = function(){");
    builder.append(tmpDate);
    builder.append("return tmpDate.getUTCHours();");
    builder.append("};");

    builder.append("Date.prototype.getMinutes = function(){");
    builder.append(tmpDate);
    builder.append("return tmpDate.getUTCMinutes();");
    builder.append("};");

    builder.append("Date.prototype.toDateString = function(){");
    builder.append(weekdayAndMonthArrays);
    builder.append(tmpDate);
    builder.append("return weekday[tmpDate.getUTCDay()] + ' ' + month[tmpDate.getUTCMonth()] "
        + "+ ' ' + tmpDate.getUTCDate() + ' ' + tmpDate.getUTCFullYear();");
    builder.append("};");

    //TODO update this when JS engine supports optional args: dateObj.toLocaleDateString([locales [, options]])
    builder.append("Date.prototype.toLocaleDateString = function(){");
    builder.append(tmpDate);
    builder.append("return (tmpDate.getUTCMonth() + 1) + '/' + tmpDate.getUTCDate() + '/' + tmpDate.getUTCFullYear();");
    builder.append("};");

    //TODO update this when JS engine supports optional args: dateObj.toLocaleString([locales[, options]])
    builder.append("Date.prototype.toLocaleString = function(){");
    builder.append(tmpDate);
    builder.append(time12Hour);
    builder.append("return (tmDatep.getUTCMonth() + 1) + '/' + tmpDate.getUTCDate() + '/' + tmpDate.getUTCFullYear() "
        + "+ ', ' + hours + ':' + minutes + ':' + seconds + ' ' + amPM;");
    builder.append("};");

    //TODO update this when JS engine supports optional args: dateObj.toLocaleTimeString([locales[, options]])
    builder.append("Date.prototype.toLocaleTimeString = function(){");
    builder.append(tmpDate);
    builder.append(time12Hour);
    builder.append("return hours + ':' + minutes + ':' + seconds + ' ' + amPM;");
    builder.append("};");

    builder.append("Date.prototype.toString = function(){");
    builder.append(weekdayAndMonthArrays);
    builder.append(tmpDate);
    builder.append(time24Hour);
    builder.append(timeZoneDescExpr);
    builder.append("return weekday[tmpDate.getUTCDay()] + ' ' + month[tmpDate.getUTCMonth()] + ' ' + tmpDate.getUTCDate() "
        + "+ ' ' + tmpDate.getUTCFullYear() + ' ' + hours + ':' + minutes + ':' + seconds + ' GMT'+timeZoneDesc;");
    builder.append("};");

    builder.append("Date.prototype.toTimeString = function(){");
    builder.append(tmpDate);
    builder.append(time24Hour);
    builder.append(timeZoneDescExpr);
    builder.append("return hours + ':' + minutes + ':' + seconds + ' GMT'+timeZoneDesc;");
    builder.append("};");
    this.script = builder.toString();
  }

  public String script() {
    if (script == null) {
      init();
    }
    return script;
  }

  private static int[][] daylightSavings(TimeZone timeZone) {
    final int curYear = Calendar.getInstance().get(Calendar.YEAR);
    final Calendar calendar = Calendar.getInstance(timeZone);
    calendar.setLenient(false);
    calendar.setTime(new Date(0));
    Date prevDate = null;
    boolean foundStart = false;
    boolean foundEnd = false;
    final int[][] span = new int[2][4];
    final int[] pos = new int[] { 0, 1, 14, 15, 16, 29, 30, 31, 44, 45, 46, 59 };
    for (int month = 0; month < 12; month++) {
      for (int day = 1; day < 32; day++) {
        for (int hour = 0; hour < 24; hour++) {
          for (int minutePos = 0; minutePos < pos.length; minutePos++) {
            calendar.set(curYear, month, day, hour, pos[minutePos], 0);
            try {
              calendar.getTime().getTime();
            } catch (Throwable t) {
              continue;
            }
            if (prevDate == null) {
              prevDate = calendar.getTime();
            } else {
              if (!foundStart && timeZone.inDaylightTime(calendar.getTime()) && !timeZone.inDaylightTime(prevDate)) {
                span[0] = toInts(dateFormat.format(calendar.getTime()).split("-"));
                --span[0][0];
                foundStart = true;
              }
              if (!foundEnd && !timeZone.inDaylightTime(calendar.getTime()) && timeZone.inDaylightTime(prevDate)) {
                span[1] = toInts(dateFormat.format(prevDate).split("-"));
                --span[1][0];
                foundEnd = true;
              }
              if (foundStart && foundEnd) {
                return span;
              }
              prevDate = calendar.getTime();
            }
          }
        }
      }
    }
    return null;
  }

  private static int[] toInts(String[] strings) {
    int[] ints = new int[strings.length];
    for (int i = 0; i < ints.length; i++) {
      ints[i] = Integer.parseInt(strings[i]);
    }
    return ints;
  }
}
