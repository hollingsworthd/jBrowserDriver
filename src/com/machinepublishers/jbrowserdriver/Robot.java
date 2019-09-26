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
import com.sun.glass.events.MouseEvent;
import com.sun.glass.ui.Application;
import com.sun.glass.ui.Pixels;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;

class Robot {
  private static final Map<Keys, KeyCode[]> keysMap;

  static {
    Map<Keys, KeyCode[]> keysMapTmp = new HashMap<Keys, KeyCode[]>();
    keysMapTmp.put(Keys.ADD, new KeyCode[] { KeyCode.ADD });
    keysMapTmp.put(Keys.ALT, new KeyCode[] { KeyCode.ALT });
    keysMapTmp.put(Keys.ARROW_DOWN, new KeyCode[] { KeyCode.DOWN });
    keysMapTmp.put(Keys.ARROW_LEFT, new KeyCode[] { KeyCode.LEFT });
    keysMapTmp.put(Keys.ARROW_RIGHT, new KeyCode[] { KeyCode.RIGHT });
    keysMapTmp.put(Keys.ARROW_UP, new KeyCode[] { KeyCode.UP });
    keysMapTmp.put(Keys.BACK_SPACE, new KeyCode[] { KeyCode.BACK_SPACE });
    keysMapTmp.put(Keys.CANCEL, new KeyCode[] { KeyCode.CANCEL });
    keysMapTmp.put(Keys.CLEAR, new KeyCode[] { KeyCode.CLEAR });
    keysMapTmp.put(Keys.COMMAND, new KeyCode[] { KeyCode.META });
    keysMapTmp.put(Keys.CONTROL, new KeyCode[] { KeyCode.CONTROL });
    keysMapTmp.put(Keys.DECIMAL, new KeyCode[] { KeyCode.DECIMAL });
    keysMapTmp.put(Keys.DELETE, new KeyCode[] { KeyCode.DELETE });
    keysMapTmp.put(Keys.DIVIDE, new KeyCode[] { KeyCode.DIVIDE });
    keysMapTmp.put(Keys.DOWN, new KeyCode[] { KeyCode.DOWN });
    keysMapTmp.put(Keys.END, new KeyCode[] { KeyCode.END });
    keysMapTmp.put(Keys.ENTER, new KeyCode[] { KeyCode.ENTER });
    keysMapTmp.put(Keys.EQUALS, new KeyCode[] { KeyCode.EQUALS });
    keysMapTmp.put(Keys.ESCAPE, new KeyCode[] { KeyCode.ESCAPE });
    keysMapTmp.put(Keys.F1, new KeyCode[] { KeyCode.F1 });
    keysMapTmp.put(Keys.F10, new KeyCode[] { KeyCode.F10 });
    keysMapTmp.put(Keys.F11, new KeyCode[] { KeyCode.F11 });
    keysMapTmp.put(Keys.F12, new KeyCode[] { KeyCode.F12 });
    keysMapTmp.put(Keys.F2, new KeyCode[] { KeyCode.F2 });
    keysMapTmp.put(Keys.F3, new KeyCode[] { KeyCode.F3 });
    keysMapTmp.put(Keys.F4, new KeyCode[] { KeyCode.F4 });
    keysMapTmp.put(Keys.F5, new KeyCode[] { KeyCode.F5 });
    keysMapTmp.put(Keys.F6, new KeyCode[] { KeyCode.F6 });
    keysMapTmp.put(Keys.F7, new KeyCode[] { KeyCode.F7 });
    keysMapTmp.put(Keys.F8, new KeyCode[] { KeyCode.F8 });
    keysMapTmp.put(Keys.F9, new KeyCode[] { KeyCode.F9 });
    keysMapTmp.put(Keys.HELP, new KeyCode[] { KeyCode.HELP });
    keysMapTmp.put(Keys.HOME, new KeyCode[] { KeyCode.HOME });
    keysMapTmp.put(Keys.INSERT, new KeyCode[] { KeyCode.INSERT });
    keysMapTmp.put(Keys.LEFT, new KeyCode[] { KeyCode.LEFT });
    keysMapTmp.put(Keys.LEFT_ALT, new KeyCode[] { KeyCode.ALT });
    keysMapTmp.put(Keys.LEFT_CONTROL, new KeyCode[] { KeyCode.CONTROL });
    keysMapTmp.put(Keys.LEFT_SHIFT, new KeyCode[] { KeyCode.SHIFT });
    keysMapTmp.put(Keys.META, new KeyCode[] { KeyCode.META });
    keysMapTmp.put(Keys.MULTIPLY, new KeyCode[] { KeyCode.MULTIPLY });
    keysMapTmp.put(Keys.NUMPAD0, new KeyCode[] { KeyCode.NUMPAD0 });
    keysMapTmp.put(Keys.NUMPAD1, new KeyCode[] { KeyCode.NUMPAD1 });
    keysMapTmp.put(Keys.NUMPAD2, new KeyCode[] { KeyCode.NUMPAD2 });
    keysMapTmp.put(Keys.NUMPAD3, new KeyCode[] { KeyCode.NUMPAD3 });
    keysMapTmp.put(Keys.NUMPAD4, new KeyCode[] { KeyCode.NUMPAD4 });
    keysMapTmp.put(Keys.NUMPAD5, new KeyCode[] { KeyCode.NUMPAD5 });
    keysMapTmp.put(Keys.NUMPAD6, new KeyCode[] { KeyCode.NUMPAD6 });
    keysMapTmp.put(Keys.NUMPAD7, new KeyCode[] { KeyCode.NUMPAD7 });
    keysMapTmp.put(Keys.NUMPAD8, new KeyCode[] { KeyCode.NUMPAD8 });
    keysMapTmp.put(Keys.NUMPAD9, new KeyCode[] { KeyCode.NUMPAD9 });
    keysMapTmp.put(Keys.PAGE_DOWN, new KeyCode[] { KeyCode.PAGE_DOWN });
    keysMapTmp.put(Keys.PAGE_UP, new KeyCode[] { KeyCode.PAGE_UP });
    keysMapTmp.put(Keys.PAUSE, new KeyCode[] { KeyCode.PAUSE });
    keysMapTmp.put(Keys.RETURN, new KeyCode[] { KeyCode.ENTER });
    keysMapTmp.put(Keys.RIGHT, new KeyCode[] { KeyCode.RIGHT });
    keysMapTmp.put(Keys.SEMICOLON, new KeyCode[] { KeyCode.SEMICOLON });
    keysMapTmp.put(Keys.SEPARATOR, new KeyCode[] { KeyCode.SEPARATOR });
    keysMapTmp.put(Keys.SHIFT, new KeyCode[] { KeyCode.SHIFT });
    keysMapTmp.put(Keys.SPACE, new KeyCode[] { KeyCode.SPACE });
    keysMapTmp.put(Keys.SUBTRACT, new KeyCode[] { KeyCode.SUBTRACT });
    keysMapTmp.put(Keys.TAB, new KeyCode[] { KeyCode.TAB });
    keysMapTmp.put(Keys.UP, new KeyCode[] { KeyCode.UP });
    keysMap = Collections.unmodifiableMap(keysMapTmp);
  }

  private static final Map<String, KeyCode[]> textMap;

  static {
    Map<String, KeyCode[]> textMapTmp = new HashMap<String, KeyCode[]>();
    textMapTmp.put("1", new KeyCode[] { KeyCode.DIGIT1 });
    textMapTmp.put("2", new KeyCode[] { KeyCode.DIGIT2 });
    textMapTmp.put("3", new KeyCode[] { KeyCode.DIGIT3 });
    textMapTmp.put("4", new KeyCode[] { KeyCode.DIGIT4 });
    textMapTmp.put("5", new KeyCode[] { KeyCode.DIGIT5 });
    textMapTmp.put("6", new KeyCode[] { KeyCode.DIGIT6 });
    textMapTmp.put("7", new KeyCode[] { KeyCode.DIGIT7 });
    textMapTmp.put("8", new KeyCode[] { KeyCode.DIGIT8 });
    textMapTmp.put("9", new KeyCode[] { KeyCode.DIGIT9 });
    textMapTmp.put("0", new KeyCode[] { KeyCode.DIGIT0 });
    textMapTmp.put("a", new KeyCode[] { KeyCode.A });
    textMapTmp.put("b", new KeyCode[] { KeyCode.B });
    textMapTmp.put("c", new KeyCode[] { KeyCode.C });
    textMapTmp.put("d", new KeyCode[] { KeyCode.D });
    textMapTmp.put("e", new KeyCode[] { KeyCode.E });
    textMapTmp.put("f", new KeyCode[] { KeyCode.F });
    textMapTmp.put("g", new KeyCode[] { KeyCode.G });
    textMapTmp.put("h", new KeyCode[] { KeyCode.H });
    textMapTmp.put("i", new KeyCode[] { KeyCode.I });
    textMapTmp.put("j", new KeyCode[] { KeyCode.J });
    textMapTmp.put("k", new KeyCode[] { KeyCode.K });
    textMapTmp.put("l", new KeyCode[] { KeyCode.L });
    textMapTmp.put("m", new KeyCode[] { KeyCode.M });
    textMapTmp.put("n", new KeyCode[] { KeyCode.N });
    textMapTmp.put("o", new KeyCode[] { KeyCode.O });
    textMapTmp.put("p", new KeyCode[] { KeyCode.P });
    textMapTmp.put("q", new KeyCode[] { KeyCode.Q });
    textMapTmp.put("r", new KeyCode[] { KeyCode.R });
    textMapTmp.put("s", new KeyCode[] { KeyCode.S });
    textMapTmp.put("t", new KeyCode[] { KeyCode.T });
    textMapTmp.put("u", new KeyCode[] { KeyCode.U });
    textMapTmp.put("v", new KeyCode[] { KeyCode.V });
    textMapTmp.put("w", new KeyCode[] { KeyCode.W });
    textMapTmp.put("x", new KeyCode[] { KeyCode.X });
    textMapTmp.put("y", new KeyCode[] { KeyCode.Y });
    textMapTmp.put("z", new KeyCode[] { KeyCode.Z });
    textMapTmp.put("A", new KeyCode[] { KeyCode.SHIFT, KeyCode.A });
    textMapTmp.put("B", new KeyCode[] { KeyCode.SHIFT, KeyCode.B });
    textMapTmp.put("C", new KeyCode[] { KeyCode.SHIFT, KeyCode.C });
    textMapTmp.put("D", new KeyCode[] { KeyCode.SHIFT, KeyCode.D });
    textMapTmp.put("E", new KeyCode[] { KeyCode.SHIFT, KeyCode.E });
    textMapTmp.put("F", new KeyCode[] { KeyCode.SHIFT, KeyCode.F });
    textMapTmp.put("G", new KeyCode[] { KeyCode.SHIFT, KeyCode.G });
    textMapTmp.put("H", new KeyCode[] { KeyCode.SHIFT, KeyCode.H });
    textMapTmp.put("I", new KeyCode[] { KeyCode.SHIFT, KeyCode.I });
    textMapTmp.put("J", new KeyCode[] { KeyCode.SHIFT, KeyCode.J });
    textMapTmp.put("K", new KeyCode[] { KeyCode.SHIFT, KeyCode.K });
    textMapTmp.put("L", new KeyCode[] { KeyCode.SHIFT, KeyCode.L });
    textMapTmp.put("M", new KeyCode[] { KeyCode.SHIFT, KeyCode.M });
    textMapTmp.put("N", new KeyCode[] { KeyCode.SHIFT, KeyCode.N });
    textMapTmp.put("O", new KeyCode[] { KeyCode.SHIFT, KeyCode.O });
    textMapTmp.put("P", new KeyCode[] { KeyCode.SHIFT, KeyCode.P });
    textMapTmp.put("Q", new KeyCode[] { KeyCode.SHIFT, KeyCode.Q });
    textMapTmp.put("R", new KeyCode[] { KeyCode.SHIFT, KeyCode.R });
    textMapTmp.put("S", new KeyCode[] { KeyCode.SHIFT, KeyCode.S });
    textMapTmp.put("T", new KeyCode[] { KeyCode.SHIFT, KeyCode.T });
    textMapTmp.put("U", new KeyCode[] { KeyCode.SHIFT, KeyCode.U });
    textMapTmp.put("V", new KeyCode[] { KeyCode.SHIFT, KeyCode.V });
    textMapTmp.put("W", new KeyCode[] { KeyCode.SHIFT, KeyCode.W });
    textMapTmp.put("X", new KeyCode[] { KeyCode.SHIFT, KeyCode.X });
    textMapTmp.put("Y", new KeyCode[] { KeyCode.SHIFT, KeyCode.Y });
    textMapTmp.put("Z", new KeyCode[] { KeyCode.SHIFT, KeyCode.Z });
    textMapTmp.put("`", new KeyCode[] { KeyCode.BACK_QUOTE });
    textMapTmp.put("-", new KeyCode[] { KeyCode.MINUS });
    textMapTmp.put("=", new KeyCode[] { KeyCode.EQUALS });
    textMapTmp.put("[", new KeyCode[] { KeyCode.OPEN_BRACKET });
    textMapTmp.put("]", new KeyCode[] { KeyCode.CLOSE_BRACKET });
    textMapTmp.put("\\", new KeyCode[] { KeyCode.BACK_SLASH });
    textMapTmp.put(";", new KeyCode[] { KeyCode.SEMICOLON });
    textMapTmp.put("'", new KeyCode[] { KeyCode.QUOTE });
    textMapTmp.put(",", new KeyCode[] { KeyCode.COMMA });
    textMapTmp.put(".", new KeyCode[] { KeyCode.PERIOD });
    textMapTmp.put("/", new KeyCode[] { KeyCode.SLASH });
    textMapTmp.put("~", new KeyCode[] { KeyCode.SHIFT, KeyCode.BACK_QUOTE });
    textMapTmp.put("_", new KeyCode[] { KeyCode.SHIFT, KeyCode.MINUS });
    textMapTmp.put("+", new KeyCode[] { KeyCode.SHIFT, KeyCode.EQUALS });
    textMapTmp.put("{", new KeyCode[] { KeyCode.SHIFT, KeyCode.OPEN_BRACKET });
    textMapTmp.put("}", new KeyCode[] { KeyCode.SHIFT, KeyCode.CLOSE_BRACKET });
    textMapTmp.put("|", new KeyCode[] { KeyCode.SHIFT, KeyCode.BACK_SLASH });
    textMapTmp.put(":", new KeyCode[] { KeyCode.SHIFT, KeyCode.SEMICOLON });
    textMapTmp.put("\"", new KeyCode[] { KeyCode.SHIFT, KeyCode.QUOTE });
    textMapTmp.put("<", new KeyCode[] { KeyCode.SHIFT, KeyCode.COMMA });
    textMapTmp.put(">", new KeyCode[] { KeyCode.SHIFT, KeyCode.PERIOD });
    textMapTmp.put("?", new KeyCode[] { KeyCode.SHIFT, KeyCode.SLASH });
    textMapTmp.put("!", new KeyCode[] { KeyCode.SHIFT, KeyCode.DIGIT1 });
    textMapTmp.put("@", new KeyCode[] { KeyCode.SHIFT, KeyCode.DIGIT2 });
    textMapTmp.put("#", new KeyCode[] { KeyCode.SHIFT, KeyCode.DIGIT3 });
    textMapTmp.put("$", new KeyCode[] { KeyCode.SHIFT, KeyCode.DIGIT4 });
    textMapTmp.put("%", new KeyCode[] { KeyCode.SHIFT, KeyCode.DIGIT5 });
    textMapTmp.put("^", new KeyCode[] { KeyCode.SHIFT, KeyCode.DIGIT6 });
    textMapTmp.put("&", new KeyCode[] { KeyCode.SHIFT, KeyCode.DIGIT7 });
    textMapTmp.put("*", new KeyCode[] { KeyCode.SHIFT, KeyCode.DIGIT8 });
    textMapTmp.put("(", new KeyCode[] { KeyCode.SHIFT, KeyCode.DIGIT9 });
    textMapTmp.put(")", new KeyCode[] { KeyCode.SHIFT, KeyCode.DIGIT0 });
    textMapTmp.put("\t", new KeyCode[] { KeyCode.TAB });
    textMapTmp.put("\n", new KeyCode[] { KeyCode.ENTER });
    textMapTmp.put(" ", new KeyCode[] { KeyCode.SPACE });
    textMap = Collections.unmodifiableMap(textMapTmp);
  }

  private static final int LINE_FEED = "\n".codePointAt(0);
  private static final int CARRIAGE_RETURN = "\r".codePointAt(0);
  private static final int ENTER = Keys.ENTER.toString().codePointAt(0);
  private final AtomicReference<com.sun.glass.ui.GlassRobot> robot = new AtomicReference<>();
  private final AtomicLong latestThread = new AtomicLong();
  private final AtomicLong curThread = new AtomicLong();
  private final Context context;

  Robot(final Context context) {
    robot.set(AppThread.exec(context.item().statusCode, () -> Application.GetApplication().createRobot()));
    this.context = context;
  }

  private static KeyCode[] convertKey(int codePoint) {
    char[] chars = Character.toChars(codePoint);
    if (chars.length == 1) {
      Keys key = Keys.getKeyFromUnicode(chars[0]);
      if (key != null) {
        return keysMap.get(key);
      }
    }
    String str = new String(new int[] { codePoint }, 0, 1);
    KeyCode[] mapping = textMap.get(str);
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
    AppThread.exec(context.item().statusCode, () -> null);
  }

  private void unlock() {
    AppThread.exec(context.item().statusCode, () -> null);
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
        final KeyCode[] converted = convertKey(ints[0]);
        if (converted != null) {
          AppThread.exec(context.item().statusCode, () -> {
            for (int j = 0; j < converted.length; j++) {
              robot.get().keyPress(converted[j]);
            }
            return null;
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
        final KeyCode[] converted = convertKey(ints[0]);
        if (converted != null) {
          AppThread.exec(context.item().statusCode, () -> {
            for (int j = converted.length - 1; j > -1; j--) {
              if (j == 0) {
                context.item().httpListener.get().resetStatusCode();
              }
              robot.get().keyRelease(converted[j]);
            }
            return null;
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
          final KeyCode[] converted = convertKey(ints[i]);
          if (converted != null) {
            AppThread.exec(true, context.item().statusCode, () -> {
              for (int j = 0; j < converted.length; j++) {
                robot.get().keyPress(converted[j]);
              }
              return null;
            });
          }
        }
        for (int i = ints.length - 1; i > -1; i--) {
          final boolean lastKey = i == 0;
          final KeyCode[] converted = convertKey(ints[i]);
          if (converted != null) {
            AppThread.exec(false, context.item().statusCode, () -> {
              for (int j = converted.length - 1; j > -1; j--) {
                if (lastKey && j == 0) {
                  context.item().httpListener.get().resetStatusCode();
                }
                robot.get().keyRelease(converted[j]);
              }
              return null;
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
          AppThread.exec(!lastKey, context.item().statusCode, () -> {
            KeyCode[] converted = convertKey(codePoint);
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
      AppThread.exec(context.item().statusCode, () -> {
        robot.get().keyPress(KeyCode.ENTER);
        robot.get().keyRelease(KeyCode.ENTER);
        return null;
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
      AppThread.exec(context.item().statusCode, () -> {
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
      });
    } finally {
      unlock();
    }
  }

  void mouseMoveBy(final double viewportX, final double viewportY) {
    lock();
    try {
      AppThread.exec(context.item().statusCode, () -> {
        Stage stage = context.item().stage.get();
        robot.get().mouseMove(
            (int) Math.rint(Math.max(0, Math.min(stage.getScene().getWidth() - 1,
                viewportX + robot.get().getMouseX()))),
            (int) Math.rint(Math.max(0, Math.min(stage.getScene().getHeight() - 1,
                viewportY + robot.get().getMouseY()))));
        return null;
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
    AppThread.exec(context.item().statusCode, () -> {
      robot.get().mousePress(button);
      return null;
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
    AppThread.exec(true, context.item().statusCode, () -> {
      if (button == MouseButton.PRIMARY) {
        context.item().httpListener.get().resetStatusCode();
      }
      robot.get().mouseRelease(button);
      return null;
    });
  }

  void mouseWheel(final int wheelAmt) {
    lock();
    try {
      AppThread.exec(context.item().statusCode, () -> {
        robot.get().mouseWheel(wheelAmt);
        return null;
      });
    } finally {
      unlock();
    }
  }

  byte[] screenshot() {
    lock();
    try {
      return AppThread.exec(context.item().statusCode, () -> {
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
            SwingFXUtils.fromFXImage(robot.get().getScreenCapture(null,
                (int) Math.rint(stage.getX() + scene.getX()),
                (int) Math.rint(stage.getY() + scene.getY()),
                (int) Math.rint(scene.getWidth()),
                (int) Math.rint(scene.getHeight()),
                false), null);
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
      });
    } finally {
      unlock();
    }
  }
}
