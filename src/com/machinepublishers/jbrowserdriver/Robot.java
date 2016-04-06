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
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import javax.imageio.ImageIO;

import org.openqa.selenium.Keys;

import com.machinepublishers.jbrowserdriver.AppThread.Pause;
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
  private static final Map<Keys, Integer> keyConvert = new HashMap<Keys, Integer>();

  static {
    keyConvert.put(Keys.ADD, KeyEvent.VK_ADD);
    keyConvert.put(Keys.ALT, KeyEvent.VK_ALT);
    keyConvert.put(Keys.ARROW_DOWN, KeyEvent.VK_DOWN);
    keyConvert.put(Keys.ARROW_LEFT, KeyEvent.VK_LEFT);
    keyConvert.put(Keys.ARROW_RIGHT, KeyEvent.VK_RIGHT);
    keyConvert.put(Keys.ARROW_UP, KeyEvent.VK_UP);
    keyConvert.put(Keys.BACK_SPACE, KeyEvent.VK_BACK_SPACE);
    keyConvert.put(Keys.CANCEL, KeyEvent.VK_CANCEL);
    keyConvert.put(Keys.CLEAR, KeyEvent.VK_CLEAR);
    keyConvert.put(Keys.COMMAND, KeyEvent.VK_META);
    keyConvert.put(Keys.CONTROL, KeyEvent.VK_CONTROL);
    keyConvert.put(Keys.DECIMAL, KeyEvent.VK_DECIMAL);
    keyConvert.put(Keys.DELETE, KeyEvent.VK_DELETE);
    keyConvert.put(Keys.DIVIDE, KeyEvent.VK_DIVIDE);
    keyConvert.put(Keys.DOWN, KeyEvent.VK_DOWN);
    keyConvert.put(Keys.END, KeyEvent.VK_END);
    keyConvert.put(Keys.ENTER, KeyEvent.VK_ENTER);
    keyConvert.put(Keys.EQUALS, KeyEvent.VK_EQUALS);
    keyConvert.put(Keys.ESCAPE, KeyEvent.VK_ESCAPE);
    keyConvert.put(Keys.F1, KeyEvent.VK_F1);
    keyConvert.put(Keys.F10, KeyEvent.VK_F10);
    keyConvert.put(Keys.F11, KeyEvent.VK_F11);
    keyConvert.put(Keys.F12, KeyEvent.VK_F12);
    keyConvert.put(Keys.F2, KeyEvent.VK_F2);
    keyConvert.put(Keys.F3, KeyEvent.VK_F3);
    keyConvert.put(Keys.F4, KeyEvent.VK_F4);
    keyConvert.put(Keys.F5, KeyEvent.VK_F5);
    keyConvert.put(Keys.F6, KeyEvent.VK_F6);
    keyConvert.put(Keys.F7, KeyEvent.VK_F7);
    keyConvert.put(Keys.F8, KeyEvent.VK_F8);
    keyConvert.put(Keys.F9, KeyEvent.VK_F9);
    keyConvert.put(Keys.HELP, KeyEvent.VK_HELP);
    keyConvert.put(Keys.HOME, KeyEvent.VK_HOME);
    keyConvert.put(Keys.INSERT, KeyEvent.VK_INSERT);
    keyConvert.put(Keys.LEFT, KeyEvent.VK_LEFT);
    keyConvert.put(Keys.LEFT_ALT, KeyEvent.VK_ALT);
    keyConvert.put(Keys.LEFT_CONTROL, KeyEvent.VK_CONTROL);
    keyConvert.put(Keys.LEFT_SHIFT, KeyEvent.VK_SHIFT);
    keyConvert.put(Keys.META, KeyEvent.VK_META);
    keyConvert.put(Keys.MULTIPLY, KeyEvent.VK_MULTIPLY);
    keyConvert.put(Keys.NUMPAD0, KeyEvent.VK_NUMPAD0);
    keyConvert.put(Keys.NUMPAD1, KeyEvent.VK_NUMPAD1);
    keyConvert.put(Keys.NUMPAD2, KeyEvent.VK_NUMPAD2);
    keyConvert.put(Keys.NUMPAD3, KeyEvent.VK_NUMPAD3);
    keyConvert.put(Keys.NUMPAD4, KeyEvent.VK_NUMPAD4);
    keyConvert.put(Keys.NUMPAD5, KeyEvent.VK_NUMPAD5);
    keyConvert.put(Keys.NUMPAD6, KeyEvent.VK_NUMPAD6);
    keyConvert.put(Keys.NUMPAD7, KeyEvent.VK_NUMPAD7);
    keyConvert.put(Keys.NUMPAD8, KeyEvent.VK_NUMPAD8);
    keyConvert.put(Keys.NUMPAD9, KeyEvent.VK_NUMPAD9);
    keyConvert.put(Keys.PAGE_DOWN, KeyEvent.VK_PAGE_DOWN);
    keyConvert.put(Keys.PAGE_UP, KeyEvent.VK_PAGE_UP);
    keyConvert.put(Keys.PAUSE, KeyEvent.VK_PAUSE);
    keyConvert.put(Keys.RETURN, KeyEvent.VK_ENTER);
    keyConvert.put(Keys.RIGHT, KeyEvent.VK_RIGHT);
    keyConvert.put(Keys.SEMICOLON, KeyEvent.VK_SEMICOLON);
    keyConvert.put(Keys.SEPARATOR, KeyEvent.VK_SEPARATOR);
    keyConvert.put(Keys.SHIFT, KeyEvent.VK_SHIFT);
    keyConvert.put(Keys.SPACE, KeyEvent.VK_SPACE);
    keyConvert.put(Keys.SUBTRACT, KeyEvent.VK_SUBTRACT);
    keyConvert.put(Keys.TAB, KeyEvent.VK_TAB);
    keyConvert.put(Keys.UP, KeyEvent.VK_UP);
    keyConvert.put(Keys.NULL, -1);
  }

  private static final Map<String, int[]> keyMap = new HashMap<String, int[]>();

  static {
    keyMap.put("1", new int[] { KeyEvent.VK_1 });
    keyMap.put("2", new int[] { KeyEvent.VK_2 });
    keyMap.put("3", new int[] { KeyEvent.VK_3 });
    keyMap.put("4", new int[] { KeyEvent.VK_4 });
    keyMap.put("5", new int[] { KeyEvent.VK_5 });
    keyMap.put("6", new int[] { KeyEvent.VK_6 });
    keyMap.put("7", new int[] { KeyEvent.VK_7 });
    keyMap.put("8", new int[] { KeyEvent.VK_8 });
    keyMap.put("9", new int[] { KeyEvent.VK_9 });
    keyMap.put("0", new int[] { KeyEvent.VK_0 });
    keyMap.put("a", new int[] { KeyEvent.VK_A });
    keyMap.put("b", new int[] { KeyEvent.VK_B });
    keyMap.put("c", new int[] { KeyEvent.VK_C });
    keyMap.put("d", new int[] { KeyEvent.VK_D });
    keyMap.put("e", new int[] { KeyEvent.VK_E });
    keyMap.put("f", new int[] { KeyEvent.VK_F });
    keyMap.put("g", new int[] { KeyEvent.VK_G });
    keyMap.put("h", new int[] { KeyEvent.VK_H });
    keyMap.put("i", new int[] { KeyEvent.VK_I });
    keyMap.put("j", new int[] { KeyEvent.VK_J });
    keyMap.put("k", new int[] { KeyEvent.VK_K });
    keyMap.put("l", new int[] { KeyEvent.VK_L });
    keyMap.put("m", new int[] { KeyEvent.VK_M });
    keyMap.put("n", new int[] { KeyEvent.VK_N });
    keyMap.put("o", new int[] { KeyEvent.VK_O });
    keyMap.put("p", new int[] { KeyEvent.VK_P });
    keyMap.put("q", new int[] { KeyEvent.VK_Q });
    keyMap.put("r", new int[] { KeyEvent.VK_R });
    keyMap.put("s", new int[] { KeyEvent.VK_S });
    keyMap.put("t", new int[] { KeyEvent.VK_T });
    keyMap.put("u", new int[] { KeyEvent.VK_U });
    keyMap.put("v", new int[] { KeyEvent.VK_V });
    keyMap.put("w", new int[] { KeyEvent.VK_W });
    keyMap.put("x", new int[] { KeyEvent.VK_X });
    keyMap.put("y", new int[] { KeyEvent.VK_Y });
    keyMap.put("z", new int[] { KeyEvent.VK_Z });
    keyMap.put("A", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_A });
    keyMap.put("B", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_B });
    keyMap.put("C", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_C });
    keyMap.put("D", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_D });
    keyMap.put("E", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_E });
    keyMap.put("F", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_F });
    keyMap.put("G", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_G });
    keyMap.put("H", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_H });
    keyMap.put("I", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_I });
    keyMap.put("J", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_J });
    keyMap.put("K", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_K });
    keyMap.put("L", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_L });
    keyMap.put("M", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_M });
    keyMap.put("N", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_N });
    keyMap.put("O", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_O });
    keyMap.put("P", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_P });
    keyMap.put("Q", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_Q });
    keyMap.put("R", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_R });
    keyMap.put("S", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_S });
    keyMap.put("T", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_T });
    keyMap.put("U", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_U });
    keyMap.put("V", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_V });
    keyMap.put("W", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_W });
    keyMap.put("X", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_X });
    keyMap.put("Y", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_Y });
    keyMap.put("Z", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_Z });
    keyMap.put("`", new int[] { KeyEvent.VK_BACK_QUOTE });
    keyMap.put("-", new int[] { KeyEvent.VK_MINUS });
    keyMap.put("=", new int[] { KeyEvent.VK_EQUALS });
    keyMap.put("[", new int[] { KeyEvent.VK_OPEN_BRACKET });
    keyMap.put("]", new int[] { KeyEvent.VK_CLOSE_BRACKET });
    keyMap.put("\\", new int[] { KeyEvent.VK_BACK_SLASH });
    keyMap.put(";", new int[] { KeyEvent.VK_SEMICOLON });
    keyMap.put("'", new int[] { KeyEvent.VK_QUOTE });
    keyMap.put(",", new int[] { KeyEvent.VK_COMMA });
    keyMap.put(".", new int[] { KeyEvent.VK_PERIOD });
    keyMap.put("/", new int[] { KeyEvent.VK_SLASH });
    keyMap.put("~", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_BACK_QUOTE });
    keyMap.put("_", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_MINUS });
    keyMap.put("+", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_EQUALS });
    keyMap.put("{", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_OPEN_BRACKET });
    keyMap.put("}", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_CLOSE_BRACKET });
    keyMap.put("|", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_BACK_SLASH });
    keyMap.put(":", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_SEMICOLON });
    keyMap.put("\"", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_QUOTE });
    keyMap.put("<", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_COMMA });
    keyMap.put(">", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_PERIOD });
    keyMap.put("?", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_SLASH });
    keyMap.put("!", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_1 });
    keyMap.put("@", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_2 });
    keyMap.put("#", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_3 });
    keyMap.put("$", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_4 });
    keyMap.put("%", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_5 });
    keyMap.put("^", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_6 });
    keyMap.put("&", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_7 });
    keyMap.put("*", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_8 });
    keyMap.put("(", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_9 });
    keyMap.put(")", new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_0 });
    keyMap.put("\t", new int[] { KeyEvent.VK_TAB });
    keyMap.put("\n", new int[] { KeyEvent.VK_ENTER });
    keyMap.put(" ", new int[] { KeyEvent.VK_SPACE });
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
  private final AtomicReference<com.sun.glass.ui.Robot> robot = new AtomicReference<com.sun.glass.ui.Robot>();
  private final AtomicLong latestThread = new AtomicLong();
  private final AtomicLong curThread = new AtomicLong();
  private final Context context;
  private final AtomicInteger statusCode;
  private final KeyCode keyUndefined;

  Robot(final Context context) {
    robot.set(AppThread.exec(Pause.SHORT, context.statusCode, new Sync<com.sun.glass.ui.Robot>() {
      public com.sun.glass.ui.Robot perform() {
        return Application.GetApplication().createRobot();
      }
    }));
    this.context = context;
    this.statusCode = context.statusCode;
    this.keyUndefined = KeyCode.UNDEFINED;
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
    int keyCode = KeyEvent.getExtendedKeyCodeForChar(codePoint);
    if (keyCode != KeyEvent.VK_UNDEFINED) {
      return new int[] { keyCode };
    }
    return null;

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

  private void lock(boolean wait) {
    long myThread = latestThread.incrementAndGet();
    synchronized (curThread) {
      while (myThread != curThread.get() + 1) {
        try {
          curThread.wait();
        } catch (Exception e) {
          LogsServer.instance().exception(e);
        }
      }
    }
    if (wait) {
      long waitMS = Math.max(SettingsManager.settings().ajaxWait(), 2) / 2;
      try {
        Thread.sleep(waitMS);
      } catch (InterruptedException e) {}
      AppThread.exec(Pause.SHORT, statusCode, new Sync<Object>() {
        @Override
        public Object perform() {
          return null;
        }
      });
      try {
        Thread.sleep(waitMS);
      } catch (InterruptedException e) {}
    }
  }

  private void unlock() {
    curThread.incrementAndGet();
    synchronized (curThread) {
      curThread.notifyAll();
    }
  }

  void keysPress(final CharSequence chars) {
    lock(true);
    try {
      keysPressHelper(chars);
    } finally {
      unlock();
    }
  }

  private void keysPressHelper(final CharSequence chars) {
    final int[] ints = chars.codePoints().toArray();
    final Integer[] integers = new Integer[ints.length];
    for (int i = 0; i < ints.length; i++) {
      integers[i] = ints[i];
    }
    final AtomicReferenceArray<Integer> codePoints = new AtomicReferenceArray<Integer>(integers);
    for (int i = 0; i < codePoints.length(); i++) {
      final int cur = i;
      AppThread.exec(Pause.SHORT, statusCode, new Sync<Object>() {
        @Override
        public Object perform() {
          int[] converted = convertKey(codePoints.get(cur));
          for (int i = 0; converted != null && i < converted.length; i++) {
            if (converted[i] != -1) {
              robot.get().keyPress(converted[i]);
            }
          }
          return null;
        }
      });
    }
  }

  void keysRelease(final CharSequence chars) {
    lock(false);
    try {
      keysReleaseHelper(chars);
    } finally {
      unlock();
    }
  }

  private void keysReleaseHelper(final CharSequence chars) {
    final int[] ints = chars.codePoints().toArray();
    final Integer[] integers = new Integer[ints.length];
    for (int i = 0; i < ints.length; i++) {
      integers[i] = ints[i];
    }
    final AtomicReferenceArray<Integer> codePoints = new AtomicReferenceArray<Integer>(integers);
    for (int i = 0; i < codePoints.length(); i++) {
      final int cur = i;
      AppThread.exec(Pause.LONG, statusCode, new Sync<Object>() {
        @Override
        public Object perform() {
          int[] converted = convertKey(codePoints.get(cur));
          if (converted != null) {
            for (int i = converted.length - 1; i > -1; i--) {
              if (converted[i] != -1) {
                robot.get().keyRelease(converted[i]);
              }
            }
          }
          return null;
        }
      });
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
    lock(true);
    try {
      if (isChord(chars)) {
        keysPressHelper(chars);
        keysReleaseHelper(new StringBuilder(chars).reverse());
      } else {
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
            AppThread.exec(Pause.LONG, statusCode, new Sync<Object>() {
              @Override
              public Object perform() {
                if (reset) {
                  context.item().httpListener.get().resetStatusCode();
                }
                context.item().view.get().fireEvent(
                    new javafx.scene.input.KeyEvent(
                        javafx.scene.input.KeyEvent.KEY_TYPED, myChar, "", keyUndefined,
                        //TODO track meta keys
                        false, false, false, false));
                return null;
              }
            });
          } else {
            keysPressHelper(myChar);
            keysReleaseHelper(myChar);
          }
        }
      }
    } finally {
      unlock();
    }
  }

  void mouseMove(final double pageX, final double pageY) {
    lock(false);
    try {
      AppThread.exec(Pause.SHORT, statusCode, new Sync<Object>() {
        @Override
        public Object perform() {
          Stage stage = context.item().stage.get();
          double adjustedPageX = Math.max(0, Math.min(pageX, stage.getScene().getWidth() - 1));
          double adjustedPageY = Math.max(0, Math.min(pageY, stage.getScene().getHeight() - 1));
          robot.get().mouseMove(
              (int) Math.rint(adjustedPageX
                  + (Double) stage.getX()
                  + (Double) stage.getScene().getX()),
              (int) Math.rint(adjustedPageY
                  + (Double) stage.getY()
                  + (Double) stage.getScene().getY()));
          return null;
        }
      });
    } finally {
      unlock();
    }
  }

  void mouseMoveBy(final double pageX, final double pageY) {
    lock(false);
    try {
      AppThread.exec(Pause.SHORT, statusCode, new Sync<Object>() {
        @Override
        public Object perform() {
          Stage stage = context.item().stage.get();
          robot.get().mouseMove(
              (int) Math.rint(Math.max(0, Math.min(stage.getScene().getWidth() - 1,
                  pageX + new Double((Integer) robot.get().getMouseX())))),
              (int) Math.rint(Math.max(0, Math.min(stage.getScene().getHeight() - 1,
                  pageY + new Double((Integer) robot.get().getMouseY())))));
          return null;
        }
      });
    } finally {
      unlock();
    }
  }

  void mouseClick(final MouseButton button) {
    lock(true);
    try {
      mousePressHelper(button);
      mouseReleaseHelper(button);
    } finally {
      unlock();
    }
  }

  void mousePress(final MouseButton button) {
    lock(true);
    try {
      mousePressHelper(button);
    } finally {
      unlock();
    }
  }

  private void mousePressHelper(final MouseButton button) {
    AppThread.exec(Pause.SHORT, statusCode, new Sync<Object>() {
      @Override
      public Object perform() {
        robot.get().mousePress(button.getValue());
        return null;
      }
    });
  }

  void mouseRelease(final MouseButton button) {
    lock(false);
    try {
      mouseReleaseHelper(button);
    } finally {
      unlock();
    }
  }

  private void mouseReleaseHelper(final MouseButton button) {
    AppThread.exec(Pause.LONG, statusCode, new Sync<Object>() {
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
    lock(true);
    try {
      AppThread.exec(Pause.SHORT, statusCode, new Sync<Object>() {
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
    lock(true);
    try {
      return AppThread.exec(Pause.NONE, statusCode, new Sync<byte[]>() {
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
          if (image == null && SettingsManager.settings().headless()) {
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