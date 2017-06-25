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
 * Browser timezone and daylight savings settings.
 * Currently one limitation is that locale for formatting of date strings will always be en-US.
 */
public class Timezone {
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

  public static final Timezone UTC = new Timezone("UTC");
  public static final Timezone AFRICA_ABIDJAN = new Timezone("Africa/Abidjan");
  public static final Timezone AFRICA_ACCRA = new Timezone("Africa/Accra");
  public static final Timezone AFRICA_ADDISABABA = new Timezone("Africa/Addis_Ababa");
  public static final Timezone AFRICA_ALGIERS = new Timezone("Africa/Algiers");
  public static final Timezone AFRICA_CAIRO = new Timezone("Africa/Cairo");
  public static final Timezone AFRICA_CASABLANCA = new Timezone("Africa/Casablanca");
  public static final Timezone AFRICA_DARESSALAAM = new Timezone("Africa/Dar_es_Salaam");
  public static final Timezone AFRICA_FREETOWN = new Timezone("Africa/Freetown");
  public static final Timezone AFRICA_JOHANNESBURG = new Timezone("Africa/Johannesburg");
  public static final Timezone AFRICA_KHARTOUM = new Timezone("Africa/Khartoum");
  public static final Timezone AFRICA_KINSHASA = new Timezone("Africa/Kinshasa");
  public static final Timezone AFRICA_LAGOS = new Timezone("Africa/Lagos");
  public static final Timezone AFRICA_MOGADISHU = new Timezone("Africa/Mogadishu");
  public static final Timezone AFRICA_NAIROBI = new Timezone("Africa/Nairobi");
  public static final Timezone AFRICA_TRIPOLI = new Timezone("Africa/Tripoli");
  public static final Timezone AMERICA_ANCHORAGE = new Timezone("America/Anchorage");
  public static final Timezone AMERICA_BELIZE = new Timezone("America/Belize");
  public static final Timezone AMERICA_BOGOTA = new Timezone("America/Bogota");
  public static final Timezone AMERICA_CANCUN = new Timezone("America/Cancun");
  public static final Timezone AMERICA_CAYMAN = new Timezone("America/Cayman");
  public static final Timezone AMERICA_CHICAGO = new Timezone("America/Chicago");
  public static final Timezone AMERICA_COSTARICA = new Timezone("America/Costa_Rica");
  public static final Timezone AMERICA_DENVER = new Timezone("America/Denver");
  public static final Timezone AMERICA_GUATEMALA = new Timezone("America/Guatemala");
  public static final Timezone AMERICA_JAMAICA = new Timezone("America/Jamaica");
  public static final Timezone AMERICA_LIMA = new Timezone("America/Lima");
  public static final Timezone AMERICA_LOSANGELES = new Timezone("America/Los_Angeles");
  public static final Timezone AMERICA_MEXICOCITY = new Timezone("America/Mexico_City");
  public static final Timezone AMERICA_MONTERREY = new Timezone("America/Monterrey");
  public static final Timezone AMERICA_MONTREAL = new Timezone("America/Montreal");
  public static final Timezone AMERICA_NEWYORK = new Timezone("America/New_York");
  public static final Timezone AMERICA_PANAMA = new Timezone("America/Panama");
  public static final Timezone AMERICA_PHOENIX = new Timezone("America/Phoenix");
  public static final Timezone AMERICA_TIJUANA = new Timezone("America/Tijuana");
  public static final Timezone AMERICA_TORONTO = new Timezone("America/Toronto");
  public static final Timezone AMERICA_VANCOUVER = new Timezone("America/Vancouver");
  public static final Timezone AMERICA_WINNIPEG = new Timezone("America/Winnipeg");
  public static final Timezone ASIA_BEIRUT = new Timezone("Asia/Beirut");
  public static final Timezone ASIA_CALCUTTA = new Timezone("Asia/Calcutta");
  public static final Timezone ASIA_DAMASCUS = new Timezone("Asia/Damascus");
  public static final Timezone ASIA_DHAKA = new Timezone("Asia/Dhaka");
  public static final Timezone ASIA_ISTANBUL = new Timezone("Asia/Istanbul");
  public static final Timezone ASIA_NOVOSIBIRSK = new Timezone("Asia/Novosibirsk");
  public static final Timezone ASIA_QATAR = new Timezone("Asia/Qatar");
  public static final Timezone ASIA_SEOUL = new Timezone("Asia/Seoul");
  public static final Timezone ASIA_SHANGHAI = new Timezone("Asia/Shanghai");
  public static final Timezone ASIA_SINGAPORE = new Timezone("Asia/Singapore");
  public static final Timezone ASIA_TELAVIV = new Timezone("Asia/Tel_Aviv");
  public static final Timezone ASIA_TOKYO = new Timezone("Asia/Tokyo");
  public static final Timezone EUROPE_AMSTERDAM = new Timezone("Europe/Amsterdam");
  public static final Timezone EUROPE_ATHENS = new Timezone("Europe/Athens");
  public static final Timezone EUROPE_BERLIN = new Timezone("Europe/Berlin");
  public static final Timezone EUROPE_BRUSSELS = new Timezone("Europe/Brussels");
  public static final Timezone EUROPE_BUCHAREST = new Timezone("Europe/Bucharest");
  public static final Timezone EUROPE_BUDAPEST = new Timezone("Europe/Budapest");
  public static final Timezone EUROPE_COPENHAGEN = new Timezone("Europe/Copenhagen");
  public static final Timezone EUROPE_ISTANBUL = new Timezone("Europe/Istanbul");
  public static final Timezone EUROPE_KIEV = new Timezone("Europe/Kiev");
  public static final Timezone EUROPE_LONDON = new Timezone("Europe/London");
  public static final Timezone EUROPE_MADRID = new Timezone("Europe/Madrid");
  public static final Timezone EUROPE_MINSK = new Timezone("Europe/Minsk");
  public static final Timezone EUROPE_MOSCOW = new Timezone("Europe/Moscow");
  public static final Timezone EUROPE_PARIS = new Timezone("Europe/Paris");
  public static final Timezone EUROPE_PRAGUE = new Timezone("Europe/Prague");
  public static final Timezone EUROPE_ROME = new Timezone("Europe/Rome");
  public static final Timezone EUROPE_SOFIA = new Timezone("Europe/Sofia");
  public static final Timezone EUROPE_STOCKHOLM = new Timezone("Europe/Stockholm");
  public static final Timezone EUROPE_VIENNA = new Timezone("Europe/Vienna");
  public static final Timezone EUROPE_WARSAW = new Timezone("Europe/Warsaw");
  public static final Timezone EUROPE_ZURICH = new Timezone("Europe/Zurich");
  public static final Timezone PACIFIC_AUCKLAND = new Timezone("Pacific/Auckland");
  public static final Timezone PACIFIC_FIJI = new Timezone("Pacific/Fiji");
  public static final Timezone PACIFIC_HONOLULU = new Timezone("Pacific/Honolulu");
  private static final Map<String, Timezone> zonesByName;
  public static final Set<Timezone> ALL_ZONES;

  static {
    Map<String, Timezone> zonesByNameTmp = new HashMap<String, Timezone>();
    Field[] fields = Timezone.class.getDeclaredFields();
    for (Field field : fields) {
      try {
        Object obj = field.get(null);
        if (obj instanceof Timezone) {
          Timezone cur = (Timezone) field.get(null);
          zonesByNameTmp.put(cur.timeZoneName, cur);
        }
      } catch (Throwable t) {}
    }
    ALL_ZONES = Collections.unmodifiableSet(new HashSet<Timezone>(zonesByNameTmp.values()));
    zonesByName = Collections.unmodifiableMap(zonesByNameTmp);
  }

  private String script;
  private final String timeZoneName;

  private Timezone(String timeZoneName) {
    this.timeZoneName = timeZoneName;
  }

  /**
   * Get a Timezone according to Java's standard locale names.
   * The names are based on {@link TimeZone} IDs. E.g., <code>America/New_York</code>
   * 
   * @param locale
   *          TimeZone ID
   * @return Timezone
   */
  public static Timezone byName(String locale) {
    return zonesByName.get(locale);
  }

  /**
   * @return The locale name for this Timezone
   */
  public String name() {
    return timeZoneName;
  }

  private static String timeZoneDesc(boolean daylight, int rawOffset, int timeZoneMinutes, int daylightMinutes) {
    int totalOffsetMinutes = timeZoneMinutes - (daylight ? daylightMinutes : 0);
    int formattedOffsetHours = Math.abs(totalOffsetMinutes / 60);
    int formattedOffsetMinutes = Math.abs(totalOffsetMinutes) - (formattedOffsetHours * 60);
    String timeZoneDesc = (totalOffsetMinutes <= 0 ? "+" : "-")
        + (formattedOffsetHours < 10 ? "0" + formattedOffsetHours : "" + formattedOffsetHours)
        + (formattedOffsetMinutes == 0 ? "00"
            : (formattedOffsetMinutes < 10 ? "0" + formattedOffsetMinutes : formattedOffsetMinutes));
    return daylight ? new StringBuilder().append(timeZoneDesc).append(" (").append(daylightTimzones.get(rawOffset)).append(")").toString()
        : new StringBuilder().append(timeZoneDesc).append(" (").append(offsets.get(rawOffset)).append(")").toString();
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
      builder.append("var start = tmpDate.getUTCMonth() > ").append(daylightSavingsStart[0]).append("? 8");
      builder.append(": (tmpDate.getUTCMonth() < ").append(daylightSavingsStart[0]).append("? -8 : 0);");
      builder.append("start += tmpDate.getUTCDate() > ").append(daylightSavingsStart[1]).append("? 4");
      builder.append(": (tmpDate.getUTCDate() < ").append(daylightSavingsStart[1]).append("? -4 : 0);");
      builder.append("start += tmpDate.getUTCHours() > ").append(daylightSavingsStart[2]).append("? 2");
      builder.append(": (tmpDate.getUTCHours() < ").append(daylightSavingsStart[2]).append("? -2 : 0);");
      builder.append("start += tmpDate.getUTCMinutes() > ").append(daylightSavingsStart[3]).append("? 1");
      builder.append(": (tmpDate.getUTCMinutes() < ").append(daylightSavingsStart[3]).append("? -1 : 0);");
      builder.append("var end = tmpDate.getUTCMonth() < ").append(daylightSavingsEnd[0]).append("? 8");
      builder.append(": (tmpDate.getUTCMonth() > ").append(daylightSavingsEnd[0]).append("? -8 : 0);");
      builder.append("end += tmpDate.getUTCDate() < ").append(daylightSavingsEnd[1]).append("? 4");
      builder.append(": (tmpDate.getUTCDate() > ").append(daylightSavingsEnd[1]).append("? -4 : 0);");
      builder.append("end += tmpDate.getUTCHours() < ").append(daylightSavingsEnd[2]).append("? 2");
      builder.append(": (tmpDate.getUTCHours() > ").append(daylightSavingsEnd[2]).append("? -2 : 0);");
      builder.append("end += tmpDate.getUTCMinutes() < ").append(daylightSavingsEnd[3]).append("? 1");
      builder.append(": (tmpDate.getUTCMinutes() > ").append(daylightSavingsEnd[3]).append("? -1 : 0);");
      builder.append("var isDaylightSavings = start > 0 && end > 0;");
    }
    String isDaylightSavings = builder.toString();
    builder = new StringBuilder();

    builder.append("var timeZoneDesc = '").append(timeZoneDesc).append("';");
    builder.append("if(isDaylightSavings){");
    builder.append("timeZoneDesc = '").append(timeZoneDescDaylight).append("';");
    builder.append("}");
    String timeZoneDescExpr = builder.toString();
    builder = new StringBuilder();

    builder.append("var tmpDate = new Date(this.getTime() + ").append(timeZone.getRawOffset()).append(");");
    builder.append(isDaylightSavings);
    builder.append("if(isDaylightSavings){");
    builder.append("  tmpDate = new Date(tmpDate.getTime() + ").append(timeZone.getDSTSavings()).append(");");
    builder.append("}");
    String tmpDate = builder.toString();
    builder = new StringBuilder();

    builder.append("var weekday = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];");
    builder.append("var month = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', ")
        .append("'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];");
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
    builder.append(tmpDate);
    builder.append("if(isDaylightSavings){");
    builder.append(" return ").append(timeZoneMinutes).append(" - ").append(daylightMinutes).append(";");
    builder.append("}");
    builder.append("return ").append(timeZoneMinutes).append(";");
    builder.append("};");

    builder.append("Date.prototype.getFullYear = function(){");
    builder.append(tmpDate);
    builder.append("return tmpDate.getUTCFullYear();");
    builder.append("};");

    builder.append("Date.prototype.getYear = function(){");
    builder.append(tmpDate);
    builder.append("return tmpDate.getUTCFullYear() % 100;");
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
    builder.append("return weekday[tmpDate.getUTCDay()] + ' ' + month[tmpDate.getUTCMonth()] ")
        .append("+ ' ' + tmpDate.getUTCDate() + ' ' + tmpDate.getUTCFullYear();");
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
    builder.append("return (tmpDate.getUTCMonth() + 1) + '/' + tmpDate.getUTCDate() + '/' + tmpDate.getUTCFullYear() ")
        .append("+ ', ' + hours + ':' + minutes + ':' + seconds + ' ' + amPM;");
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
    builder.append("return weekday[tmpDate.getUTCDay()] + ' ' + month[tmpDate.getUTCMonth()] + ' ' + tmpDate.getUTCDate() ")
        .append("+ ' ' + tmpDate.getUTCFullYear() + ' ' + hours + ':' + minutes + ':' + seconds + ' GMT'+timeZoneDesc;");
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
    final int[] pos = new int[] { 0, 30 };
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
