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

import java.awt.Transparency;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;

import org.openqa.selenium.Keys;

import com.machinepublishers.jbrowserdriver.AppThread.Sync;
import com.sun.glass.ui.Application;
import com.sun.glass.ui.Pixels;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

class Robot {
  private static final Map<Keys, int[]> keysMap;

  static {
    Map<Keys, int[]> keysMapTmp = new HashMap<Keys, int[]>();
    keysMapTmp.put(Keys.ADD, new int[] { KeyEvent.VK_ADD });
    keysMapTmp.put(Keys.ALT, new int[] { KeyEvent.VK_ALT });
    keysMapTmp.put(Keys.ARROW_DOWN, new int[] { KeyEvent.VK_DOWN });
    keysMapTmp.put(Keys.ARROW_LEFT, new int[] { KeyEvent.VK_LEFT });
    keysMapTmp.put(Keys.ARROW_RIGHT, new int[] { KeyEvent.VK_RIGHT });
    keysMapTmp.put(Keys.ARROW_UP, new int[] { KeyEvent.VK_UP });
    keysMapTmp.put(Keys.BACK_SPACE, new int[] { KeyEvent.VK_BACK_SPACE });
    keysMapTmp.put(Keys.CANCEL, new int[] { KeyEvent.VK_CANCEL });
    keysMapTmp.put(Keys.CLEAR, new int[] { KeyEvent.VK_CLEAR });
    keysMapTmp.put(Keys.COMMAND, new int[] { KeyEvent.VK_META });
    keysMapTmp.put(Keys.CONTROL, new int[] { KeyEvent.VK_CONTROL });
    keysMapTmp.put(Keys.DECIMAL, new int[] { KeyEvent.VK_DECIMAL });
    keysMapTmp.put(Keys.DELETE, new int[] { KeyEvent.VK_DELETE });
    keysMapTmp.put(Keys.DIVIDE, new int[] { KeyEvent.VK_DIVIDE });
    keysMapTmp.put(Keys.DOWN, new int[] { KeyEvent.VK_DOWN });
    keysMapTmp.put(Keys.END, new int[] { KeyEvent.VK_END });
    keysMapTmp.put(Keys.ENTER, new int[] { KeyEvent.VK_ENTER });
    keysMapTmp.put(Keys.EQUALS, new int[] { KeyEvent.VK_EQUALS });
    keysMapTmp.put(Keys.ESCAPE, new int[] { KeyEvent.VK_ESCAPE });
    keysMapTmp.put(Keys.F1, new int[] { KeyEvent.VK_F1 });
    keysMapTmp.put(Keys.F10, new int[] { KeyEvent.VK_F10 });
    keysMapTmp.put(Keys.F11, new int[] { KeyEvent.VK_F11 });
    keysMapTmp.put(Keys.F12, new int[] { KeyEvent.VK_F12 });
    keysMapTmp.put(Keys.F2, new int[] { KeyEvent.VK_F2 });
    keysMapTmp.put(Keys.F3, new int[] { KeyEvent.VK_F3 });
    keysMapTmp.put(Keys.F4, new int[] { KeyEvent.VK_F4 });
    keysMapTmp.put(Keys.F5, new int[] { KeyEvent.VK_F5 });
    keysMapTmp.put(Keys.F6, new int[] { KeyEvent.VK_F6 });
    keysMapTmp.put(Keys.F7, new int[] { KeyEvent.VK_F7 });
    keysMapTmp.put(Keys.F8, new int[] { KeyEvent.VK_F8 });
    keysMapTmp.put(Keys.F9, new int[] { KeyEvent.VK_F9 });
    keysMapTmp.put(Keys.HELP, new int[] { KeyEvent.VK_HELP });
    keysMapTmp.put(Keys.HOME, new int[] { KeyEvent.VK_HOME });
    keysMapTmp.put(Keys.INSERT, new int[] { KeyEvent.VK_INSERT });
    keysMapTmp.put(Keys.LEFT, new int[] { KeyEvent.VK_LEFT });
    keysMapTmp.put(Keys.LEFT_ALT, new int[] { KeyEvent.VK_ALT });
    keysMapTmp.put(Keys.LEFT_CONTROL, new int[] { KeyEvent.VK_CONTROL });
    keysMapTmp.put(Keys.LEFT_SHIFT, new int[] { KeyEvent.VK_SHIFT });
    keysMapTmp.put(Keys.META, new int[] { KeyEvent.VK_META });
    keysMapTmp.put(Keys.MULTIPLY, new int[] { KeyEvent.VK_MULTIPLY });
    keysMapTmp.put(Keys.NUMPAD0, new int[] { KeyEvent.VK_NUMPAD0 });
    keysMapTmp.put(Keys.NUMPAD1, new int[] { KeyEvent.VK_NUMPAD1 });
    keysMapTmp.put(Keys.NUMPAD2, new int[] { KeyEvent.VK_NUMPAD2 });
    keysMapTmp.put(Keys.NUMPAD3, new int[] { KeyEvent.VK_NUMPAD3 });
    keysMapTmp.put(Keys.NUMPAD4, new int[] { KeyEvent.VK_NUMPAD4 });
    keysMapTmp.put(Keys.NUMPAD5, new int[] { KeyEvent.VK_NUMPAD5 });
    keysMapTmp.put(Keys.NUMPAD6, new int[] { KeyEvent.VK_NUMPAD6 });
    keysMapTmp.put(Keys.NUMPAD7, new int[] { KeyEvent.VK_NUMPAD7 });
    keysMapTmp.put(Keys.NUMPAD8, new int[] { KeyEvent.VK_NUMPAD8 });
    keysMapTmp.put(Keys.NUMPAD9, new int[] { KeyEvent.VK_NUMPAD9 });
    keysMapTmp.put(Keys.PAGE_DOWN, new int[] { KeyEvent.VK_PAGE_DOWN });
    keysMapTmp.put(Keys.PAGE_UP, new int[] { KeyEvent.VK_PAGE_UP });
    keysMapTmp.put(Keys.PAUSE, new int[] { KeyEvent.VK_PAUSE });
    keysMapTmp.put(Keys.RETURN, new int[] { KeyEvent.VK_ENTER });
    keysMapTmp.put(Keys.RIGHT, new int[] { KeyEvent.VK_RIGHT });
    keysMapTmp.put(Keys.SEMICOLON, new int[] { KeyEvent.VK_SEMICOLON });
    keysMapTmp.put(Keys.SEPARATOR, new int[] { KeyEvent.VK_SEPARATOR });
    keysMapTmp.put(Keys.SHIFT, new int[] { KeyEvent.VK_SHIFT });
    keysMapTmp.put(Keys.SPACE, new int[] { KeyEvent.VK_SPACE });
    keysMapTmp.put(Keys.SUBTRACT, new int[] { KeyEvent.VK_SUBTRACT });
    keysMapTmp.put(Keys.TAB, new int[] { KeyEvent.VK_TAB });
    keysMapTmp.put(Keys.UP, new int[] { KeyEvent.VK_UP });
    keysMap = Collections.unmodifiableMap(keysMapTmp);
  }

  private static final Map<String, int[]> textMap;

  static {
    Map<String, int[]> textMapTmp = new HashMap<String, int[]>();
    textMapTmp.put("1", new int[] { KeyEvent.VK_1 });
    textMapTmp.put("2", new int[] { KeyEvent.VK_2 });
    textMapTmp.put("3", new int[] { KeyEvent.VK_3 });
    textMapTmp.put("4", new int[] { KeyEvent.VK_4 });
    textMapTmp.put("5", new int[] { KeyEvent.VK_5 });
    textMapTmp.put("6", new int[] { KeyEvent.VK_6 });
    textMapTmp.put("7", new int[] { KeyEvent.VK_7 });
    textMapTmp.put("8", new int[] { KeyEvent.VK_8 });
    textMapTmp.put("9", new int[] { KeyEvent.VK_9 });
    textMapTmp.put("0", new int[] { KeyEvent.VK_0 });
    textMapTmp.put("a", new int[] { KeyEvent.VK_A });
    textMapTmp.put("b", new int[] { KeyEvent.VK_B });
    textMapTmp.put("c", new int[] { KeyEvent.VK_C });
    textMapTmp.put("d", new int[] { KeyEvent.VK_D });
    textMapTmp.put("e", new int[] { KeyEvent.VK_E });
    textMapTmp.put("f", new int[] { KeyEvent.VK_F });
    textMapTmp.put("g", new int[] { KeyEvent.VK_G });
    textMapTmp.put("h", new int[] { KeyEvent.VK_H });
    textMapTmp.put("i", new int[] { KeyEvent.VK_I });
    textMapTmp.put("j", new int[] { KeyEvent.VK_J });
    textMapTmp.put("k", new int[] { KeyEvent.VK_K });
    textMapTmp.put("l", new int[] { KeyEvent.VK_L });
    textMapTmp.put("m", new int[] { KeyEvent.VK_M });
    textMapTmp.put("n", new int[] { KeyEvent.VK_N });
    textMapTmp.put("o", new int[] { KeyEvent.VK_O });
    textMapTmp.put("p", new int[] { KeyEvent.VK_P });
    textMapTmp.put("q", new int[] { KeyEvent.VK_Q });
    textMapTmp.put("r", new int[] { KeyEvent.VK_R });
    textMapTmp.put("s", new int[] { KeyEvent.VK_S });
    textMapTmp.put("t", new int[] { KeyEvent.VK_T });
    textMapTmp.put("u", new int[] { KeyEvent.VK_U });
    textMapTmp.put("v", new int[] { KeyEvent.VK_V });
    textMapTmp.put("w", new int[] { KeyEvent.VK_W });
    textMapTmp.put("x", new int[] { KeyEvent.VK_X });
    textMapTmp.put("y", new int[] { KeyEvent.VK_Y });
    textMapTmp.put("z", new int[] { KeyEvent.VK_Z });
    textMapTmp.put("A", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_A });
    textMapTmp.put("B", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_B });
    textMapTmp.put("C", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_C });
    textMapTmp.put("D", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_D });
    textMapTmp.put("E", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_E });
    textMapTmp.put("F", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_F });
    textMapTmp.put("G", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_G });
    textMapTmp.put("H", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_H });
    textMapTmp.put("I", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_I });
    textMapTmp.put("J", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_J });
    textMapTmp.put("K", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_K });
    textMapTmp.put("L", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_L });
    textMapTmp.put("M", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_M });
    textMapTmp.put("N", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_N });
    textMapTmp.put("O", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_O });
    textMapTmp.put("P", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_P });
    textMapTmp.put("Q", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_Q });
    textMapTmp.put("R", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_R });
    textMapTmp.put("S", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_S });
    textMapTmp.put("T", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_T });
    textMapTmp.put("U", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_U });
    textMapTmp.put("V", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_V });
    textMapTmp.put("W", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_W });
    textMapTmp.put("X", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_X });
    textMapTmp.put("Y", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_Y });
    textMapTmp.put("Z", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_Z });
    textMapTmp.put("`", new int[] { KeyEvent.VK_BACK_QUOTE });
    textMapTmp.put("-", new int[] { KeyEvent.VK_MINUS });
    textMapTmp.put("=", new int[] { KeyEvent.VK_EQUALS });
    textMapTmp.put("[", new int[] { KeyEvent.VK_OPEN_BRACKET });
    textMapTmp.put("]", new int[] { KeyEvent.VK_CLOSE_BRACKET });
    textMapTmp.put("\\", new int[] { KeyEvent.VK_BACK_SLASH });
    textMapTmp.put(";", new int[] { KeyEvent.VK_SEMICOLON });
    textMapTmp.put("'", new int[] { KeyEvent.VK_QUOTE });
    textMapTmp.put(",", new int[] { KeyEvent.VK_COMMA });
    textMapTmp.put(".", new int[] { KeyEvent.VK_PERIOD });
    textMapTmp.put("/", new int[] { KeyEvent.VK_SLASH });
    textMapTmp.put("~", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_BACK_QUOTE });
    textMapTmp.put("_", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_MINUS });
    textMapTmp.put("+", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_EQUALS });
    textMapTmp.put("{", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_OPEN_BRACKET });
    textMapTmp.put("}", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_CLOSE_BRACKET });
    textMapTmp.put("|", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_BACK_SLASH });
    textMapTmp.put(":", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_SEMICOLON });
    textMapTmp.put("\"", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_QUOTE });
    textMapTmp.put("<", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_COMMA });
    textMapTmp.put(">", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_PERIOD });
    textMapTmp.put("?", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_SLASH });
    textMapTmp.put("!", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_1 });
    textMapTmp.put("@", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_2 });
    textMapTmp.put("#", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_3 });
    textMapTmp.put("$", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_4 });
    textMapTmp.put("%", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_5 });
    textMapTmp.put("^", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_6 });
    textMapTmp.put("&", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_7 });
    textMapTmp.put("*", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_8 });
    textMapTmp.put("(", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_9 });
    textMapTmp.put(")", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_0 });
    textMapTmp.put("\t", new int[] { KeyEvent.VK_TAB });
    textMapTmp.put("\n", new int[] { KeyEvent.VK_ENTER });
    textMapTmp.put(" ", new int[] { KeyEvent.VK_SPACE });
    textMap = Collections.unmodifiableMap(textMapTmp);
  }

  static enum MouseButton {
    LEFT(com.sun.glass.ui.Robot.MOUSE_LEFT_BTN), MIDDLE(com.sun.glass.ui.Robot.MOUSE_MIDDLE_BTN), RIGHT(com.sun.glass.ui.Robot.MOUSE_RIGHT_BTN);
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
  private static final int ENTER = Keys.ENTER.toString().codePointAt(0);
  private final AtomicReference<com.sun.glass.ui.Robot> robot = new AtomicReference<com.sun.glass.ui.Robot>();
  private final AtomicLong latestThread = new AtomicLong();
  private final AtomicLong curThread = new AtomicLong();
  private final Context context;

  Robot(final Context context) {
    robot.set(AppThread.exec(context.item().statusCode, new Sync<com.sun.glass.ui.Robot>() {
      public com.sun.glass.ui.Robot perform() {
        return Application.GetApplication().createRobot();
      }
    }));
    this.context = context;
  }

  private static int[] convertKey(int codePoint) {
    char[] chars = Character.toChars(codePoint);
    if (chars.length == 1) {
      Keys key = Keys.getKeyFromUnicode(chars[0]);
      if (key != null) {
        return keysMap.get(key);
      }
    }
    String str = new String(new int[] { codePoint }, 0, 1);
    int[] mapping = textMap.get(str);
    return mapping;
  }

  private static boolean isChord(CharSequence charSequence) {
    int[] codePoints = charSequence.codePoints().toArray();
    if (codePoints.length > 0) {
      char[] chars = Character.toChars(codePoints[codePoints.length - 1]);
      if (chars.length == 1) {
        return Keys.NULL.equals(Keys.getKeyFromUnicode(chars[0]));
      }
    }
    return false;
  }

  private void lock() {
    long myThread = latestThread.incrementAndGet();
    synchronized (curThread) {
      while (myThread != curThread.get() + 1) {
        try {
          curThread.wait();
        } catch (InterruptedException e) {}
      }
    }
    AppThread.exec(context.item().statusCode, new Sync<Object>() {
      @Override
      public Object perform() {
        return null;
      }
    });
  }

  private void unlock() {
    AppThread.exec(context.item().statusCode, new Sync<Object>() {
      @Override
      public Object perform() {
        return null;
      }
    });
    curThread.incrementAndGet();
    synchronized (curThread) {
      curThread.notifyAll();
    }
  }

  void keysPress(final CharSequence chars) {
    lock();
    try {
      final int[] ints = chars.chars().toArray();
      if (ints.length > 0) {
        final int[] converted = convertKey(ints[0]);
        if (converted != null) {
          AppThread.exec(context.item().statusCode, new Sync<Object>() {
            @Override
            public Object perform() {
              for (int j = 0; j < converted.length; j++) {
                robot.get().keyPress(converted[j]);
              }
              return null;
            }
          });
        }
      }
    } finally {
      unlock();
    }
  }

  void keysRelease(final CharSequence chars) {
    lock();
    try {
      final int[] ints = chars.chars().toArray();
      if (ints.length > 0) {
        final int[] converted = convertKey(ints[0]);
        if (converted != null) {
          AppThread.exec(context.item().statusCode, new Sync<Object>() {
            @Override
            public Object perform() {
              for (int j = converted.length - 1; j > -1; j--) {
                if (j == 0) {
                  context.item().httpListener.get().resetStatusCode();
                }
                robot.get().keyRelease(converted[j]);
              }
              return null;
            }
          });
        }
      }
    } finally {
      unlock();
    }
  }

  void keysType(final CharSequence... charsList) {
    for (CharSequence chars : charsList) {
      if (Util.KEYBOARD_DELETE.equals(chars.toString())) {
        keysTypeHelper(Keys.chord(Keys.CONTROL, "a"));
        keysTypeHelper(Keys.BACK_SPACE.toString());
      } else {
        keysTypeHelper(chars);
      }
    }
  }

  private void keysTypeHelper(final CharSequence chars) {
    lock();
    try {
      if (isChord(chars)) {
        //TODO handle non-ascii chords

        int[] ints = chars.chars().toArray();
        for (int i = 0; i < ints.length; i++) {
          final int[] converted = convertKey(ints[i]);
          if (converted != null) {
            AppThread.exec(true, context.item().statusCode, new Sync<Object>() {
              @Override
              public Object perform() {
                for (int j = 0; j < converted.length; j++) {
                  robot.get().keyPress(converted[j]);
                }
                return null;
              }
            });
          }
        }
        for (int i = ints.length - 1; i > -1; i--) {
          final boolean lastKey = i == 0;
          final int[] converted = convertKey(ints[i]);
          if (converted != null) {
            AppThread.exec(false, context.item().statusCode, new Sync<Object>() {
              @Override
              public Object perform() {
                for (int j = converted.length - 1; j > -1; j--) {
                  if (lastKey && j == 0) {
                    context.item().httpListener.get().resetStatusCode();
                  }
                  robot.get().keyRelease(converted[j]);
                }
                return null;
              }
            });
          }
        }
      } else {
        int[] ints = chars.chars().toArray();
        for (int i = 0; i < ints.length; i++) {
          final int codePoint;
          final boolean lastKey = i == ints.length - 1;
          if (ints[i] == LINE_FEED || ints[i] == CARRIAGE_RETURN || ints[i] == ENTER) {
            //replace linefeed with carriage returns due to idiosyncrasy of WebView
            codePoint = '\r';
          } else {
            codePoint = ints[i];
          }
          AppThread.exec(!lastKey, context.item().statusCode, new Sync<Object>() {
            @Override
            public Object perform() {
              int[] converted = convertKey(codePoint);
              if (converted == null) {
                if (lastKey) {
                  context.item().httpListener.get().resetStatusCode();
                }
                context.item().view.get().fireEvent(
                    new javafx.scene.input.KeyEvent(
                        javafx.scene.input.KeyEvent.KEY_TYPED, new String(new int[] { codePoint }, 0, 1), "", KeyCode.UNDEFINED,
                        false, false, false, false));
              } else {
                for (int j = 0; j < converted.length; j++) {
                  robot.get().keyPress(converted[j]);
                }
                for (int j = converted.length - 1; j > -1; j--) {
                  if (lastKey && j == 0) {
                    context.item().httpListener.get().resetStatusCode();
                  }
                  robot.get().keyRelease(converted[j]);
                }
              }
              return null;
            }
          });
        }
      }
    } finally {
      unlock();
    }
  }

  void typeEnter() {
    lock();
    try {
      AppThread.exec(context.item().statusCode, new Sync<Object>() {
        @Override
        public Object perform() {
          robot.get().keyPress(KeyEvent.VK_ENTER);
          robot.get().keyRelease(KeyEvent.VK_ENTER);
          return null;
        }
      });
    } finally {
      unlock();
    }
  }

  void mouseMove(final org.openqa.selenium.Point point) {
    mouseMove(point.getX(), point.getY());
  }

  void mouseMove(final double viewportX, final double viewportY) {
    lock();
    try {
      AppThread.exec(context.item().statusCode, new Sync<Object>() {
        @Override
        public Object perform() {
          Stage stage = context.item().stage.get();
          double adjustedX = Math.max(0, Math.min(viewportX, stage.getScene().getWidth() - 1));
          double adjustedY = Math.max(0, Math.min(viewportY, stage.getScene().getHeight() - 1));
          robot.get().mouseMove(
              (int) Math.rint(adjustedX
                  + (Double) stage.getX()
                  + (Double) stage.getScene().getX()),
              (int) Math.rint(adjustedY
                  + (Double) stage.getY()
                  + (Double) stage.getScene().getY()));
          return null;
        }
      });
    } finally {
      unlock();
    }
  }

  void mouseMoveBy(final double viewportX, final double viewportY) {
    lock();
    try {
      AppThread.exec(context.item().statusCode, new Sync<Object>() {
        @Override
        public Object perform() {
          Stage stage = context.item().stage.get();
          robot.get().mouseMove(
              (int) Math.rint(Math.max(0, Math.min(stage.getScene().getWidth() - 1,
                  viewportX + new Double((Integer) robot.get().getMouseX())))),
              (int) Math.rint(Math.max(0, Math.min(stage.getScene().getHeight() - 1,
                  viewportY + new Double((Integer) robot.get().getMouseY())))));
          return null;
        }
      });
    } finally {
      unlock();
    }
  }

  void mouseClick(final MouseButton button) {
    lock();
    try {
      mousePressHelper(button);
      mouseReleaseHelper(button);
    } finally {
      unlock();
    }
  }

  void mousePress(final MouseButton button) {
    lock();
    try {
      mousePressHelper(button);
    } finally {
      unlock();
    }
  }

  private void mousePressHelper(final MouseButton button) {
    AppThread.exec(context.item().statusCode, new Sync<Object>() {
      @Override
      public Object perform() {
        robot.get().mousePress(button.getValue());
        return null;
      }
    });
  }

  void mouseRelease(final MouseButton button) {
    lock();
    try {
      mouseReleaseHelper(button);
    } finally {
      unlock();
    }
  }

  private void mouseReleaseHelper(final MouseButton button) {
    AppThread.exec(true, context.item().statusCode, new Sync<Object>() {
      @Override
      public Object perform() {
        if (button == MouseButton.LEFT) {
          context.item().httpListener.get().resetStatusCode();
        }
        robot.get().mouseRelease(button.getValue());
        return null;
      }
    });
  }

  void mouseWheel(final int wheelAmt) {
    lock();
    try {
      AppThread.exec(context.item().statusCode, new Sync<Object>() {
        @Override
        public Object perform() {
          robot.get().mouseWheel(wheelAmt);
          return null;
        }
      });
    } finally {
      unlock();
    }
  }

  byte[] screenshot() {
    lock();
    try {
      return AppThread.exec(context.item().statusCode, new Sync<byte[]>() {
        @Override
        public byte[] perform() {
          BufferedImage image = null;

          Throwable attempt1 = null;
          try {
            image = SwingFXUtils.fromFXImage(
                context.item().view.get().snapshot(
                    new SnapshotParameters(),
                    new WritableImage(
                        (int) Math.rint((Double) context.item().view.get().getWidth()),
                        (int) Math.rint((Double) context.item().view.get().getHeight()))),
                null);
          } catch (Throwable t) {
            attempt1 = t;
          }

          Throwable attempt2 = null;
          if (image == null && SettingsManager.isMonocle()) {
            try {
              final Stage stage = context.item().stage.get();
              final Scene scene = stage.getScene();
              final Pixels pixels = robot.get().getScreenCapture(
                  (int) Math.rint(stage.getX() + scene.getX()),
                  (int) Math.rint(stage.getY() + scene.getY()),
                  (int) Math.rint(scene.getWidth()),
                  (int) Math.rint(scene.getHeight()),
                  false);
              final ByteBuffer pixelBuffer = pixels.asByteBuffer();
              final byte[] bytes = new byte[pixelBuffer.remaining()];
              pixelBuffer.get(bytes);
              final int bytesPerComponent = pixels.getBytesPerComponent();
              final int width = pixels.getWidth();
              final int height = pixels.getHeight();
              final DataBuffer buffer = new DataBufferByte(bytes, bytes.length);
              final WritableRaster raster = Raster.createInterleavedRaster(buffer, width, height,
                  bytesPerComponent * width, bytesPerComponent, new int[] { 2, 1, 0 }, null);
              final ColorModel colorModel = new ComponentColorModel(ColorModel.getRGBdefault().getColorSpace(),
                  false, true, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
              image = new BufferedImage(colorModel, raster, true, null);
            } catch (Throwable t) {
              attempt2 = t;
            }
          }
          if (image != null) {
            ByteArrayOutputStream out = null;
            try {
              out = new ByteArrayOutputStream();
              ImageIO.write(image, "png", out);
              return out.toByteArray();
            } catch (Throwable t) {
              LogsServer.instance().exception(t);
            } finally {
              Util.close(out);
            }
          }
          LogsServer.instance().exception(attempt1);
          LogsServer.instance().exception(attempt2);
          return null;
        }
      });
    } finally {
      unlock();
    }
  }
}