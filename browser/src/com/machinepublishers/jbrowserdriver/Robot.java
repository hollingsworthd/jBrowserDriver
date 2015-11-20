/* 
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2015 Machine Publishers, LLC
 * ops@machinepublishers.com | screenslicer.com | machinepublishers.com
 * Cincinnati, Ohio, USA
 *
 * You can redistribute this program and/or modify it under the terms of the GNU Affero General Public
 * License version 3 as published by the Free Software Foundation.
 *
 * "ScreenSlicer", "jBrowserDriver", "Machine Publishers", and "automatic, zero-config web scraping"
 * are trademarks of Machine Publishers, LLC.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License version 3 for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License version 3 along with this
 * program. If not, see http://www.gnu.org/licenses/
 * 
 * For general details about how to investigate and report license violations, please see
 * https://www.gnu.org/licenses/gpl-violation.html and email the author, ops@machinepublishers.com
 */
package com.machinepublishers.jbrowserdriver;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.Keys;

import com.machinepublishers.jbrowserdriver.Util.Pause;
import com.machinepublishers.jbrowserdriver.Util.Sync;

class Robot {
  private static final Map<Keys, Integer> keyConvert = new HashMap<Keys, Integer>();

  private static int keyEvent(String name) {
    try {
      return (Integer) JavaFx.getStatic("java.awt.event.KeyEvent", 0l).field(name).unwrap();
    } catch (Throwable t) {
      Logs.exception(t);
      return -1;
    }
  }

  static {
    keyConvert.put(Keys.ADD, keyEvent("VK_ADD"));
    keyConvert.put(Keys.ALT, keyEvent("VK_ALT"));
    keyConvert.put(Keys.ARROW_DOWN, keyEvent("VK_DOWN"));
    keyConvert.put(Keys.ARROW_LEFT, keyEvent("VK_LEFT"));
    keyConvert.put(Keys.ARROW_RIGHT, keyEvent("VK_RIGHT"));
    keyConvert.put(Keys.ARROW_UP, keyEvent("VK_UP"));
    keyConvert.put(Keys.BACK_SPACE, keyEvent("VK_BACK_SPACE"));
    keyConvert.put(Keys.CANCEL, keyEvent("VK_CANCEL"));
    keyConvert.put(Keys.CLEAR, keyEvent("VK_CLEAR"));
    keyConvert.put(Keys.COMMAND, keyEvent("VK_META"));
    keyConvert.put(Keys.CONTROL, keyEvent("VK_CONTROL"));
    keyConvert.put(Keys.DECIMAL, keyEvent("VK_DECIMAL"));
    keyConvert.put(Keys.DELETE, keyEvent("VK_DELETE"));
    keyConvert.put(Keys.DIVIDE, keyEvent("VK_DIVIDE"));
    keyConvert.put(Keys.DOWN, keyEvent("VK_DOWN"));
    keyConvert.put(Keys.END, keyEvent("VK_END"));
    keyConvert.put(Keys.ENTER, keyEvent("VK_ENTER"));
    keyConvert.put(Keys.EQUALS, keyEvent("VK_EQUALS"));
    keyConvert.put(Keys.ESCAPE, keyEvent("VK_ESCAPE"));
    keyConvert.put(Keys.F1, keyEvent("VK_F1"));
    keyConvert.put(Keys.F10, keyEvent("VK_F10"));
    keyConvert.put(Keys.F11, keyEvent("VK_F11"));
    keyConvert.put(Keys.F12, keyEvent("VK_F12"));
    keyConvert.put(Keys.F2, keyEvent("VK_F2"));
    keyConvert.put(Keys.F3, keyEvent("VK_F3"));
    keyConvert.put(Keys.F4, keyEvent("VK_F4"));
    keyConvert.put(Keys.F5, keyEvent("VK_F5"));
    keyConvert.put(Keys.F6, keyEvent("VK_F6"));
    keyConvert.put(Keys.F7, keyEvent("VK_F7"));
    keyConvert.put(Keys.F8, keyEvent("VK_F8"));
    keyConvert.put(Keys.F9, keyEvent("VK_F9"));
    keyConvert.put(Keys.HELP, keyEvent("VK_HELP"));
    keyConvert.put(Keys.HOME, keyEvent("VK_HOME"));
    keyConvert.put(Keys.INSERT, keyEvent("VK_INSERT"));
    keyConvert.put(Keys.LEFT, keyEvent("VK_LEFT"));
    keyConvert.put(Keys.LEFT_ALT, keyEvent("VK_ALT"));
    keyConvert.put(Keys.LEFT_CONTROL, keyEvent("VK_CONTROL"));
    keyConvert.put(Keys.LEFT_SHIFT, keyEvent("VK_SHIFT"));
    keyConvert.put(Keys.META, keyEvent("VK_META"));
    keyConvert.put(Keys.MULTIPLY, keyEvent("VK_MULTIPLY"));
    keyConvert.put(Keys.NUMPAD0, keyEvent("VK_NUMPAD0"));
    keyConvert.put(Keys.NUMPAD1, keyEvent("VK_NUMPAD1"));
    keyConvert.put(Keys.NUMPAD2, keyEvent("VK_NUMPAD2"));
    keyConvert.put(Keys.NUMPAD3, keyEvent("VK_NUMPAD3"));
    keyConvert.put(Keys.NUMPAD4, keyEvent("VK_NUMPAD4"));
    keyConvert.put(Keys.NUMPAD5, keyEvent("VK_NUMPAD5"));
    keyConvert.put(Keys.NUMPAD6, keyEvent("VK_NUMPAD6"));
    keyConvert.put(Keys.NUMPAD7, keyEvent("VK_NUMPAD7"));
    keyConvert.put(Keys.NUMPAD8, keyEvent("VK_NUMPAD8"));
    keyConvert.put(Keys.NUMPAD9, keyEvent("VK_NUMPAD9"));
    keyConvert.put(Keys.PAGE_DOWN, keyEvent("VK_PAGE_DOWN"));
    keyConvert.put(Keys.PAGE_UP, keyEvent("VK_PAGE_UP"));
    keyConvert.put(Keys.PAUSE, keyEvent("VK_PAUSE"));
    keyConvert.put(Keys.RETURN, keyEvent("VK_ENTER"));
    keyConvert.put(Keys.RIGHT, keyEvent("VK_RIGHT"));
    keyConvert.put(Keys.SEMICOLON, keyEvent("VK_SEMICOLON"));
    keyConvert.put(Keys.SEPARATOR, keyEvent("VK_SEPARATOR"));
    keyConvert.put(Keys.SHIFT, keyEvent("VK_SHIFT"));
    keyConvert.put(Keys.SPACE, keyEvent("VK_SPACE"));
    keyConvert.put(Keys.SUBTRACT, keyEvent("VK_SUBTRACT"));
    keyConvert.put(Keys.TAB, keyEvent("VK_TAB"));
    keyConvert.put(Keys.UP, keyEvent("VK_UP"));
    keyConvert.put(Keys.NULL, -1);
  }
  private static final Map<String, int[]> keyMap = new HashMap<String, int[]>();
  static {
    keyMap.put("1", new int[] { keyEvent("VK_1") });
    keyMap.put("2", new int[] { keyEvent("VK_2") });
    keyMap.put("3", new int[] { keyEvent("VK_3") });
    keyMap.put("4", new int[] { keyEvent("VK_4") });
    keyMap.put("5", new int[] { keyEvent("VK_5") });
    keyMap.put("6", new int[] { keyEvent("VK_6") });
    keyMap.put("7", new int[] { keyEvent("VK_7") });
    keyMap.put("8", new int[] { keyEvent("VK_8") });
    keyMap.put("9", new int[] { keyEvent("VK_9") });
    keyMap.put("0", new int[] { keyEvent("VK_0") });
    keyMap.put("a", new int[] { keyEvent("VK_A") });
    keyMap.put("b", new int[] { keyEvent("VK_B") });
    keyMap.put("c", new int[] { keyEvent("VK_C") });
    keyMap.put("d", new int[] { keyEvent("VK_D") });
    keyMap.put("e", new int[] { keyEvent("VK_E") });
    keyMap.put("f", new int[] { keyEvent("VK_F") });
    keyMap.put("g", new int[] { keyEvent("VK_G") });
    keyMap.put("h", new int[] { keyEvent("VK_H") });
    keyMap.put("i", new int[] { keyEvent("VK_I") });
    keyMap.put("j", new int[] { keyEvent("VK_J") });
    keyMap.put("k", new int[] { keyEvent("VK_K") });
    keyMap.put("l", new int[] { keyEvent("VK_L") });
    keyMap.put("m", new int[] { keyEvent("VK_M") });
    keyMap.put("n", new int[] { keyEvent("VK_N") });
    keyMap.put("o", new int[] { keyEvent("VK_O") });
    keyMap.put("p", new int[] { keyEvent("VK_P") });
    keyMap.put("q", new int[] { keyEvent("VK_Q") });
    keyMap.put("r", new int[] { keyEvent("VK_R") });
    keyMap.put("s", new int[] { keyEvent("VK_S") });
    keyMap.put("t", new int[] { keyEvent("VK_T") });
    keyMap.put("u", new int[] { keyEvent("VK_U") });
    keyMap.put("v", new int[] { keyEvent("VK_V") });
    keyMap.put("w", new int[] { keyEvent("VK_W") });
    keyMap.put("x", new int[] { keyEvent("VK_X") });
    keyMap.put("y", new int[] { keyEvent("VK_Y") });
    keyMap.put("z", new int[] { keyEvent("VK_Z") });
    keyMap.put("A", new int[] { keyEvent("VK_SHIFT"), keyEvent("VK_A") });
    keyMap.put("B", new int[] { keyEvent("VK_SHIFT"), keyEvent("VK_B") });
    keyMap.put("C", new int[] { keyEvent("VK_SHIFT"), keyEvent("VK_C") });
    keyMap.put("D", new int[] { keyEvent("VK_SHIFT"), keyEvent("VK_D") });
    keyMap.put("E", new int[] { keyEvent("VK_SHIFT"), keyEvent("VK_E") });
    keyMap.put("F", new int[] { keyEvent("VK_SHIFT"), keyEvent("VK_F") });
    keyMap.put("G", new int[] { keyEvent("VK_SHIFT"), keyEvent("VK_G") });
    keyMap.put("H", new int[] { keyEvent("VK_SHIFT"), keyEvent("VK_H") });
    keyMap.put("I", new int[] { keyEvent("VK_SHIFT"), keyEvent("VK_I") });
    keyMap.put("J", new int[] { keyEvent("VK_SHIFT"), keyEvent("VK_J") });
    keyMap.put("K", new int[] { keyEvent("VK_SHIFT"), keyEvent("VK_K") });
    keyMap.put("L", new int[] { keyEvent("VK_SHIFT"), keyEvent("VK_L") });
    keyMap.put("M", new int[] { keyEvent("VK_SHIFT"), keyEvent("VK_M") });
    keyMap.put("N", new int[] { keyEvent("VK_SHIFT"), keyEvent("VK_N") });
    keyMap.put("O", new int[] { keyEvent("VK_SHIFT"), keyEvent("VK_O") });
    keyMap.put("P", new int[] { keyEvent("VK_SHIFT"), keyEvent("VK_P") });
    keyMap.put("Q", new int[] { keyEvent("VK_SHIFT"), keyEvent("VK_Q") });
    keyMap.put("R", new int[] { keyEvent("VK_SHIFT"), keyEvent("VK_R") });
    keyMap.put("S", new int[] { keyEvent("VK_SHIFT"), keyEvent("VK_S") });
    keyMap.put("T", new int[] { keyEvent("VK_SHIFT"), keyEvent("VK_T") });
    keyMap.put("U", new int[] { keyEvent("VK_SHIFT"), keyEvent("VK_U") });
    keyMap.put("V", new int[] { keyEvent("VK_SHIFT"), keyEvent("VK_V") });
    keyMap.put("W", new int[] { keyEvent("VK_SHIFT"), keyEvent("VK_W") });
    keyMap.put("X", new int[] { keyEvent("VK_SHIFT"), keyEvent("VK_X") });
    keyMap.put("Y", new int[] { keyEvent("VK_SHIFT"), keyEvent("VK_Y") });
    keyMap.put("Z", new int[] { keyEvent("VK_SHIFT"), keyEvent("VK_Z") });
    keyMap.put("`", new int[] { keyEvent("VK_BACK_QUOTE") });
    keyMap.put("-", new int[] { keyEvent("VK_MINUS") });
    keyMap.put("=", new int[] { keyEvent("VK_EQUALS") });
    keyMap.put("[", new int[] { keyEvent("VK_OPEN_BRACKET") });
    keyMap.put("]", new int[] { keyEvent("VK_CLOSE_BRACKET") });
    keyMap.put("\\", new int[] { keyEvent("VK_BACK_SLASH") });
    keyMap.put(";", new int[] { keyEvent("VK_SEMICOLON") });
    keyMap.put("'", new int[] { keyEvent("VK_QUOTE") });
    keyMap.put(",", new int[] { keyEvent("VK_COMMA") });
    keyMap.put(".", new int[] { keyEvent("VK_PERIOD") });
    keyMap.put("/", new int[] { keyEvent("VK_SLASH") });
    keyMap.put("~", new int[] { keyEvent("VK_SHIFT"), keyEvent("VK_BACK_QUOTE") });
    keyMap.put("_", new int[] { keyEvent("VK_SHIFT"), keyEvent("VK_MINUS") });
    keyMap.put("+", new int[] { keyEvent("VK_SHIFT"), keyEvent("VK_EQUALS") });
    keyMap.put("{", new int[] { keyEvent("VK_SHIFT"), keyEvent("VK_OPEN_BRACKET") });
    keyMap.put("}", new int[] { keyEvent("VK_SHIFT"), keyEvent("VK_CLOSE_BRACKET") });
    keyMap.put("|", new int[] { keyEvent("VK_SHIFT"), keyEvent("VK_BACK_SLASH") });
    keyMap.put(":", new int[] { keyEvent("VK_SHIFT"), keyEvent("VK_SEMICOLON") });
    keyMap.put("\"", new int[] { keyEvent("VK_SHIFT"), keyEvent("VK_QUOTE") });
    keyMap.put("<", new int[] { keyEvent("VK_SHIFT"), keyEvent("VK_COMMA") });
    keyMap.put(">", new int[] { keyEvent("VK_SHIFT"), keyEvent("VK_PERIOD") });
    keyMap.put("?", new int[] { keyEvent("VK_SHIFT"), keyEvent("VK_SLASH") });
    keyMap.put("!", new int[] { keyEvent("VK_SHIFT"), keyEvent("VK_1") });
    keyMap.put("@", new int[] { keyEvent("VK_SHIFT"), keyEvent("VK_2") });
    keyMap.put("#", new int[] { keyEvent("VK_SHIFT"), keyEvent("VK_3") });
    keyMap.put("$", new int[] { keyEvent("VK_SHIFT"), keyEvent("VK_4") });
    keyMap.put("%", new int[] { keyEvent("VK_SHIFT"), keyEvent("VK_5") });
    keyMap.put("^", new int[] { keyEvent("VK_SHIFT"), keyEvent("VK_6") });
    keyMap.put("&", new int[] { keyEvent("VK_SHIFT"), keyEvent("VK_7") });
    keyMap.put("*", new int[] { keyEvent("VK_SHIFT"), keyEvent("VK_8") });
    keyMap.put("(", new int[] { keyEvent("VK_SHIFT"), keyEvent("VK_9") });
    keyMap.put(")", new int[] { keyEvent("VK_SHIFT"), keyEvent("VK_0") });
    keyMap.put("\t", new int[] { keyEvent("VK_TAB") });
    keyMap.put("\n", new int[] { keyEvent("VK_ENTER") });
    keyMap.put(" ", new int[] { keyEvent("VK_SPACE") });
  }

  static enum MouseButton {
    LEFT(com.sun.glass.ui.Robot.MOUSE_LEFT_BTN),
    MIDDLE(com.sun.glass.ui.Robot.MOUSE_MIDDLE_BTN),
    RIGHT(com.sun.glass.ui.Robot.MOUSE_RIGHT_BTN);
    private final int value;

    MouseButton(int value) {
      this.value = value;
    }

    int getValue() {
      return value;
    }
  }

  private static final int LINE_FEED = "\n".codePointAt(0);
  private static final int CARRIAGE_RETURN = "\r".codePointAt(0);
  private final AtomicReference<JavaFxObject> robot = new AtomicReference<JavaFxObject>();
  private final AtomicLong latestThread = new AtomicLong();
  private final AtomicLong curThread = new AtomicLong();
  private final BrowserContext context;
  private final long settingsId;
  private final AtomicInteger statusCode;
  private final Object keyTyped;
  private final Object keyUndefined;

  Robot(final BrowserContext context) {
    robot.set(Util.exec(Pause.SHORT, context.statusCode, new Sync<JavaFxObject>() {
      public JavaFxObject perform() {
        return JavaFx.getStatic(
            "com.sun.glass.ui.Application", context.settingsId.get()).call("GetApplication").call("createRobot");
      }
    }, context.settingsId.get()));
    this.context = context;
    this.statusCode = context.statusCode;
    this.settingsId = context.settingsId.get();
    this.keyTyped = JavaFx.getStatic("javafx.scene.input.KeyEvent", settingsId).
        field("KEY_TYPED").unwrap();
    this.keyUndefined = JavaFx.getStatic("javafx.scene.input.KeyCode", settingsId).
        field("UNDEFINED").unwrap();
  }

  private int[] convertKey(int codePoint) {
    char[] chars = Character.toChars(codePoint);
    if (chars.length == 1) {
      Keys key = Keys.getKeyFromUnicode(chars[0]);
      if (key != null) {
        Integer mapping = keyConvert.get(key);
        if (mapping != null) {
          return new int[] { mapping };
        }
      }
    }
    String str = new String(new int[] { codePoint }, 0, 1);
    int[] mapping = keyMap.get(str);
    if (mapping != null) {
      return mapping;
    }
    int keyCode = (Integer) JavaFx.getStatic("java.awt.event.KeyEvent", context.settingsId.get()).call("getExtendedKeyCodeForChar", codePoint).unwrap();
    if (keyCode != keyEvent("VK_UNDEFINED")) {
      return new int[] { keyCode };
    }
    return null;
  }

  private static boolean isChord(CharSequence charSequence) {
    int[] codePoints = charSequence.codePoints().toArray();
    char[] chars = Character.toChars(codePoints[codePoints.length - 1]);
    if (chars.length == 1) {
      return Keys.NULL.equals(Keys.getKeyFromUnicode(chars[0]));
    }
    return false;
  }

  private void lock() {
    long myThread = latestThread.incrementAndGet();
    synchronized (curThread) {
      while (myThread != curThread.get() + 1) {
        try {
          curThread.wait();
        } catch (Exception e) {
          Logs.exception(e);
        }
      }
    }
  }

  private void unlock() {
    curThread.incrementAndGet();
    synchronized (curThread) {
      curThread.notifyAll();
    }
  }

  void keysPress(final CharSequence chars) {
    keysPress(chars, true, true);
  }

  private void keysPress(final CharSequence chars, boolean doLocking, boolean delay) {
    if (doLocking) {
      lock();
    }
    try {
      final int[] ints = chars.codePoints().toArray();
      final Integer[] integers = new Integer[ints.length];
      for (int i = 0; i < ints.length; i++) {
        integers[i] = ints[i];
      }
      final AtomicReferenceArray<Integer> codePoints = new AtomicReferenceArray<Integer>(integers);
      for (int i = 0; i < codePoints.length(); i++) {
        final int cur = i;
        Util.exec(delay ? Pause.LONG : Pause.SHORT, statusCode, new Sync<Object>() {
          @Override
          public Object perform() {
            int[] converted = convertKey(codePoints.get(cur));
            for (int i = 0; converted != null && i < converted.length; i++) {
              if (converted[i] != -1) {
                robot.get().call("keyPress", converted[i]);
              }
            }
            return null;
          }
        }, settingsId);
      }
    } finally {
      if (doLocking) {
        unlock();
      }
    }
  }

  void keysRelease(final CharSequence chars) {
    keysRelease(chars, true, true);
  }

  private void keysRelease(final CharSequence chars, boolean doLocking, boolean delay) {
    if (doLocking) {
      lock();
    }
    try {
      final int[] ints = chars.codePoints().toArray();
      final Integer[] integers = new Integer[ints.length];
      for (int i = 0; i < ints.length; i++) {
        integers[i] = ints[i];
      }
      final AtomicReferenceArray<Integer> codePoints = new AtomicReferenceArray<Integer>(integers);
      for (int i = 0; i < codePoints.length(); i++) {
        final int cur = i;
        Util.exec(delay ? Pause.LONG : Pause.SHORT, statusCode, new Sync<Object>() {
          @Override
          public Object perform() {
            int[] converted = convertKey(codePoints.get(cur));
            if (converted != null) {
              for (int i = converted.length - 1; i > -1; i--) {
                if (converted[i] != -1) {
                  robot.get().call("keyRelease", converted[i]);
                }
              }
            }
            return null;
          }
        }, settingsId);
      }
    } finally {
      if (doLocking) {
        unlock();
      }
    }
  }

  void keysType(final CharSequence... charsList) {
    StringJoiner joiner = new StringJoiner("");
    for (CharSequence chars : charsList) {
      joiner.add(chars);
    }
    final String toSend = joiner.toString();
    keysType(toSend);
  }

  void keysType(final CharSequence chars) {
    lock();
    try {
      if (isChord(chars)) {
        keysPress(chars, false, true);
        keysRelease(new StringBuilder(chars).reverse(), false, true);
      } else {
        final boolean delay = !chars.toString().equals(JBrowserDriver.KEYBOARD_DELETE);
        int[] ints = chars.codePoints().toArray();
        for (int i = 0; i < ints.length; i++) {
          final int codePoint = ints[i];
          String myChar;
          boolean fireEvent;
          final boolean reset;
          if (codePoint == LINE_FEED || codePoint == CARRIAGE_RETURN) {
            //replace linefeed with carriage returns due to idiosyncrasy of WebView
            myChar = "\r";
            fireEvent = true;
            reset = true;
          } else {
            myChar = new String(new int[] { codePoint }, 0, 1);
            fireEvent = convertKey(codePoint) == null;
            reset = false;
          }
          if (fireEvent) {
            Util.exec(delay ? Pause.LONG : Pause.SHORT, statusCode, new Sync<Object>() {
              @Override
              public Object perform() {
                if (reset) {
                  context.item().httpListener.get().call("resetStatusCode");
                }
                context.item().view.get().call("fireEvent",
                    JavaFx.getNew("javafx.scene.input.KeyEvent", settingsId,
                        keyTyped, myChar, "", keyUndefined,
                        //TODO track meta keys
                        false, false, false, false));
                return null;
              }
            }, settingsId);
          } else {
            keysPress(myChar, false, false);
            keysRelease(myChar, false, delay);
          }
        }
      }
    } finally {
      unlock();
    }
  }

  void mouseMove(final double pageX, final double pageY) {
    lock();
    try {
      Util.exec(Pause.LONG, statusCode, new Sync<Object>() {
        @Override
        public Object perform() {
          JavaFxObject stage = context.item().stage.get();
          robot.get().call("mouseMove",
              (int) Math.rint(pageX
                  + (Double) stage.call("getX").unwrap()
                  + (Double) stage.call("getScene").call("getX").unwrap()),
              (int) Math.rint(pageY
                  + (Double) stage.call("getY").unwrap()
                  + (Double) stage.call("getScene").call("getY").unwrap()));
          return null;
        }
      }, settingsId);
    } finally {
      unlock();
    }
  }

  void mouseMoveBy(final double pageX, final double pageY) {
    lock();
    try {
      Util.exec(Pause.LONG, statusCode, new Sync<Object>() {
        @Override
        public Object perform() {
          robot.get().call("mouseMove",
              (int) Math.rint(pageX
                  + new Double((Integer) robot.get().call("getMouseX").unwrap())),
              (int) Math.rint(pageY
                  + new Double((Integer) robot.get().call("getMouseY").unwrap())));
          return null;
        }
      }, settingsId);
    } finally {
      unlock();
    }
  }

  void mouseClick(final MouseButton button) {
    lock();
    try {
      mousePress(button, false, false);
      mouseRelease(button, false, true);
    } finally {
      unlock();
    }
  }

  void mousePress(final MouseButton button) {
    mousePress(button, true, true);
  }

  private void mousePress(final MouseButton button, boolean doLocking, boolean delay) {
    if (doLocking) {
      lock();
    }
    try {
      Util.exec(delay ? Pause.LONG : Pause.SHORT, statusCode, new Sync<Object>() {
        @Override
        public Object perform() {
          robot.get().call("mousePress", button.getValue());
          return null;
        }
      }, settingsId);
    } finally {
      if (doLocking) {
        unlock();
      }
    }
  }

  void mouseRelease(final MouseButton button) {
    mouseRelease(button, true, true);
  }

  private void mouseRelease(final MouseButton button, boolean doLocking, boolean delay) {
    if (doLocking) {
      lock();
    }
    try {
      Util.exec(delay ? Pause.LONG : Pause.SHORT, statusCode, new Sync<Object>() {
        @Override
        public Object perform() {
          if (button == MouseButton.LEFT) {
            context.item().httpListener.get().call("resetStatusCode");
          }
          robot.get().call("mouseRelease", button.getValue());
          return null;
        }
      }, settingsId);
    } finally {
      if (doLocking) {
        unlock();
      }
    }
  }

  void mouseWheel(final int wheelAmt) {
    lock();
    try {
      Util.exec(Pause.LONG, statusCode, new Sync<Object>() {
        @Override
        public Object perform() {
          robot.get().call("mouseWheel", wheelAmt);
          return null;
        }
      }, settingsId);
    } finally {
      unlock();
    }
  }

  JavaFxObject screenshot() {
    lock();
    try {
      return Util.exec(Pause.LONG, statusCode, new Sync<JavaFxObject>() {
        @Override
        public JavaFxObject perform() {
          int x = (int) Math.rint(
              (Double) context.item().stage.get().call("getX").unwrap()
                  + (Double) context.item().stage.get().call("getScene").call("getX").unwrap());
          int y = (int) Math.rint(
              (Double) context.item().stage.get().call("getY").unwrap()
                  + (Double) context.item().stage.get().call("getScene").call("getY").unwrap());
          Dimension screen = context.settings.get().screen();
          return robot.get().call("getScreenCapture",
              x, y, screen.getWidth(), screen.getHeight(), false);
        }
      }, settingsId);
    } finally {
      unlock();
    }
  }
}
