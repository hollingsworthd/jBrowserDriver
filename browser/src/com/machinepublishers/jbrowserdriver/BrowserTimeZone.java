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
 * Browser timezone and daylight savings offsets change according to this locale.
 * Regardless, locale for formatting of date strings will be en-US.
 */
public class BrowserTimeZone {
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
    offsets.put(28800000, "CST");
    offsets.put(32400000, "JST");
    offsets.put(34200000, "ACST");
    offsets.put(39600000, "SST");
    offsets.put(43200000, "NZST");
    offsets.put(46800000, "MIT");
  }
  private static final Map<Integer, String> daylightTimzones = new HashMap<Integer, String>();
  static {
    daylightTimzones.put(-36000000, "HADT");
    daylightTimzones.put(-32400000, "AKDT");
    daylightTimzones.put(-28800000, "PDT");
    daylightTimzones.put(-25200000, "MDT");
    daylightTimzones.put(-21600000, "CDT");
    daylightTimzones.put(-18000000, "EDT");
    daylightTimzones.put(0, "UTC");
    daylightTimzones.put(3600000, "CEST");
    daylightTimzones.put(7200000, "EEST");
    daylightTimzones.put(10800000, "EAT");
    daylightTimzones.put(19800000, "IST");
    daylightTimzones.put(21600000, "BST");
    daylightTimzones.put(28800000, "CST");
    daylightTimzones.put(32400000, "JST");
    daylightTimzones.put(34200000, "ACDT");
    daylightTimzones.put(39600000, "SST");
    daylightTimzones.put(43200000, "NZDT");
    daylightTimzones.put(46800000, "MIT");
  }
  public static final BrowserTimeZone UTC = new BrowserTimeZone("UTC");
  public static final BrowserTimeZone AFRICA_ABIDJAN = new BrowserTimeZone("Africa/Abidjan");
  public static final BrowserTimeZone AFRICA_ACCRA = new BrowserTimeZone("Africa/Accra");
  public static final BrowserTimeZone AFRICA_ADDISABABA = new BrowserTimeZone("Africa/Addis_Ababa");
  public static final BrowserTimeZone AFRICA_ALGIERS = new BrowserTimeZone("Africa/Algiers");
  public static final BrowserTimeZone AFRICA_CAIRO = new BrowserTimeZone("Africa/Cairo");
  public static final BrowserTimeZone AFRICA_CASABLANCA = new BrowserTimeZone("Africa/Casablanca");
  public static final BrowserTimeZone AFRICA_DARESSALAAM = new BrowserTimeZone("Africa/Dar_es_Salaam");
  public static final BrowserTimeZone AFRICA_FREETOWN = new BrowserTimeZone("Africa/Freetown");
  public static final BrowserTimeZone AFRICA_JOHANNESBURG = new BrowserTimeZone("Africa/Johannesburg");
  public static final BrowserTimeZone AFRICA_KHARTOUM = new BrowserTimeZone("Africa/Khartoum");
  public static final BrowserTimeZone AFRICA_KINSHASA = new BrowserTimeZone("Africa/Kinshasa");
  public static final BrowserTimeZone AFRICA_LAGOS = new BrowserTimeZone("Africa/Lagos");
  public static final BrowserTimeZone AFRICA_MOGADISHU = new BrowserTimeZone("Africa/Mogadishu");
  public static final BrowserTimeZone AFRICA_NAIROBI = new BrowserTimeZone("Africa/Nairobi");
  public static final BrowserTimeZone AFRICA_TRIPOLI = new BrowserTimeZone("Africa/Tripoli");
  public static final BrowserTimeZone AMERICA_ANCHORAGE = new BrowserTimeZone("America/Anchorage");
  public static final BrowserTimeZone AMERICA_BELIZE = new BrowserTimeZone("America/Belize");
  public static final BrowserTimeZone AMERICA_BOGOTA = new BrowserTimeZone("America/Bogota");
  public static final BrowserTimeZone AMERICA_CANCUN = new BrowserTimeZone("America/Cancun");
  public static final BrowserTimeZone AMERICA_CAYMAN = new BrowserTimeZone("America/Cayman");
  public static final BrowserTimeZone AMERICA_CHICAGO = new BrowserTimeZone("America/Chicago");
  public static final BrowserTimeZone AMERICA_COSTARICA = new BrowserTimeZone("America/Costa_Rica");
  public static final BrowserTimeZone AMERICA_DENVER = new BrowserTimeZone("America/Denver");
  public static final BrowserTimeZone AMERICA_GUATEMALA = new BrowserTimeZone("America/Guatemala");
  public static final BrowserTimeZone AMERICA_JAMAICA = new BrowserTimeZone("America/Jamaica");
  public static final BrowserTimeZone AMERICA_LIMA = new BrowserTimeZone("America/Lima");
  public static final BrowserTimeZone AMERICA_LOSANGELES = new BrowserTimeZone("America/Los_Angeles");
  public static final BrowserTimeZone AMERICA_MEXICOCITY = new BrowserTimeZone("America/Mexico_City");
  public static final BrowserTimeZone AMERICA_MONTERREY = new BrowserTimeZone("America/Monterrey");
  public static final BrowserTimeZone AMERICA_MONTREAL = new BrowserTimeZone("America/Montreal");
  public static final BrowserTimeZone AMERICA_NEWYORK = new BrowserTimeZone("America/New_York");
  public static final BrowserTimeZone AMERICA_PANAMA = new BrowserTimeZone("America/Panama");
  public static final BrowserTimeZone AMERICA_PHOENIX = new BrowserTimeZone("America/Phoenix");
  public static final BrowserTimeZone AMERICA_TIJUANA = new BrowserTimeZone("America/Tijuana");
  public static final BrowserTimeZone AMERICA_TORONTO = new BrowserTimeZone("America/Toronto");
  public static final BrowserTimeZone AMERICA_VANCOUVER = new BrowserTimeZone("America/Vancouver");
  public static final BrowserTimeZone AMERICA_WINNIPEG = new BrowserTimeZone("America/Winnipeg");
  public static final BrowserTimeZone ASIA_BEIRUT = new BrowserTimeZone("Asia/Beirut");
  public static final BrowserTimeZone ASIA_CALCUTTA = new BrowserTimeZone("Asia/Calcutta");
  public static final BrowserTimeZone ASIA_DAMASCUS = new BrowserTimeZone("Asia/Damascus");
  public static final BrowserTimeZone ASIA_DHAKA = new BrowserTimeZone("Asia/Dhaka");
  public static final BrowserTimeZone ASIA_ISTANBUL = new BrowserTimeZone("Asia/Istanbul");
  public static final BrowserTimeZone ASIA_NOVOSIBIRSK = new BrowserTimeZone("Asia/Novosibirsk");
  public static final BrowserTimeZone ASIA_QATAR = new BrowserTimeZone("Asia/Qatar");
  public static final BrowserTimeZone ASIA_SEOUL = new BrowserTimeZone("Asia/Seoul");
  public static final BrowserTimeZone ASIA_SHANGHAI = new BrowserTimeZone("Asia/Shanghai");
  public static final BrowserTimeZone ASIA_SINGAPORE = new BrowserTimeZone("Asia/Singapore");
  public static final BrowserTimeZone ASIA_TELAVIV = new BrowserTimeZone("Asia/Tel_Aviv");
  public static final BrowserTimeZone ASIA_TOKYO = new BrowserTimeZone("Asia/Tokyo");
  public static final BrowserTimeZone EUROPE_AMSTERDAM = new BrowserTimeZone("Europe/Amsterdam");
  public static final BrowserTimeZone EUROPE_ATHENS = new BrowserTimeZone("Europe/Athens");
  public static final BrowserTimeZone EUROPE_BERLIN = new BrowserTimeZone("Europe/Berlin");
  public static final BrowserTimeZone EUROPE_BRUSSELS = new BrowserTimeZone("Europe/Brussels");
  public static final BrowserTimeZone EUROPE_BUCHAREST = new BrowserTimeZone("Europe/Bucharest");
  public static final BrowserTimeZone EUROPE_BUDAPEST = new BrowserTimeZone("Europe/Budapest");
  public static final BrowserTimeZone EUROPE_COPENHAGEN = new BrowserTimeZone("Europe/Copenhagen");
  public static final BrowserTimeZone EUROPE_ISTANBUL = new BrowserTimeZone("Europe/Istanbul");
  public static final BrowserTimeZone EUROPE_KIEV = new BrowserTimeZone("Europe/Kiev");
  public static final BrowserTimeZone EUROPE_LONDON = new BrowserTimeZone("Europe/London");
  public static final BrowserTimeZone EUROPE_MADRID = new BrowserTimeZone("Europe/Madrid");
  public static final BrowserTimeZone EUROPE_MINSK = new BrowserTimeZone("Europe/Minsk");
  public static final BrowserTimeZone EUROPE_MOSCOW = new BrowserTimeZone("Europe/Moscow");
  public static final BrowserTimeZone EUROPE_PARIS = new BrowserTimeZone("Europe/Paris");
  public static final BrowserTimeZone EUROPE_PRAGUE = new BrowserTimeZone("Europe/Prague");
  public static final BrowserTimeZone EUROPE_ROME = new BrowserTimeZone("Europe/Rome");
  public static final BrowserTimeZone EUROPE_SOFIA = new BrowserTimeZone("Europe/Sofia");
  public static final BrowserTimeZone EUROPE_STOCKHOLM = new BrowserTimeZone("Europe/Stockholm");
  public static final BrowserTimeZone EUROPE_VIENNA = new BrowserTimeZone("Europe/Vienna");
  public static final BrowserTimeZone EUROPE_WARSAW = new BrowserTimeZone("Europe/Warsaw");
  public static final BrowserTimeZone EUROPE_ZURICH = new BrowserTimeZone("Europe/Zurich");
  public static final BrowserTimeZone PACIFIC_AUCKLAND = new BrowserTimeZone("Pacific/Auckland");
  public static final BrowserTimeZone PACIFIC_FIJI = new BrowserTimeZone("Pacific/Fiji");
  public static final BrowserTimeZone PACIFIC_HONOLULU = new BrowserTimeZone("Pacific/Honolulu");
  public static final Set<BrowserTimeZone> ALL_ZONES;
  static {
    Set<BrowserTimeZone> tmp = new HashSet<BrowserTimeZone>();
    Field[] fields = BrowserTimeZone.class.getDeclaredFields();
    for (int i = 0; i < fields.length; i++) {
      try {
        Object obj = fields[i].get(null);
        if (obj instanceof BrowserTimeZone) {
          tmp.add((BrowserTimeZone) fields[i].get(null));
        }
      } catch (Throwable t) {}
    }
    ALL_ZONES = Collections.unmodifiableSet(tmp);
  }

  private String script;
  private final String timeZoneName;

  private BrowserTimeZone(String timeZoneName) {
    this.timeZoneName = timeZoneName;
  }

  private static String timeZoneDesc(boolean daylight, int rawOffset, int timeZoneMinutes, int daylightMinutes) {
    int totalOffsetMinutes = timeZoneMinutes - (daylight ? daylightMinutes : 0);
    int formattedOffsetHours = Math.abs(totalOffsetMinutes / 60);
    int formattedOffsetMinutes = Math.abs(totalOffsetMinutes) - (formattedOffsetHours * 60);
    String timeZoneDesc = (totalOffsetMinutes <= 0 ? "+" : "-")
        + (formattedOffsetHours < 10 ? "0" + formattedOffsetHours : "" + formattedOffsetHours)
        + (formattedOffsetMinutes == 0 ? "00"
            : (formattedOffsetMinutes < 10 ? "0" + formattedOffsetMinutes : formattedOffsetMinutes));
    return daylight ? timeZoneDesc + " (" + daylightTimzones.get(rawOffset) + ")"
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

    builder.append("var tmpDate = new Date(this.getTime() + " + timeZone.getRawOffset() + ");");
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

  String script() {
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
