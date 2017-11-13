package com.machinepublishers.jbrowserdriver;

import java.util.Arrays;
import java.util.Map;

import com.google.common.base.CaseFormat;

import org.openqa.selenium.interactions.Keyboard;
import org.openqa.selenium.interactions.Mouse;

public enum W3CActions {
  PAUSE {
    @Override
    Element perform(Object actionExecutor, Element lastProcessedElement, Map<String, Object> descriptor) {
      try {
        Thread.sleep((long) descriptor.get("duration"));
        return lastProcessedElement;
      } catch (InterruptedException e) {
        throw new IllegalStateException(e);
      }
    }
  },
  KEY_DOWN {
  @Override
    Element perform(Object actionExecutor, Element lastProcessedElement, Map<String, Object> descriptor) {
      ((Keyboard) actionExecutor).pressKey((CharSequence) descriptor.get("value"));
      return lastProcessedElement;
    }
  },
  KEY_UP {
    @Override
    Element perform(Object actionExecutor, Element lastProcessedElement, Map<String, Object> descriptor) {
      ((Keyboard) actionExecutor).releaseKey((CharSequence) descriptor.get("value"));
      return lastProcessedElement;
    }
  },
  POINTER_DOWN {
    @Override
    Element perform(Object actionExecutor, Element lastProcessedElement, Map<String, Object> descriptor) {
      ((Mouse) actionExecutor).mouseDown(getCoordinetes(lastProcessedElement));
      return lastProcessedElement;
    }
  },
  POINTER_UP {
    @Override
    Element perform(Object actionExecutor, Element lastProcessedElement, Map<String, Object> descriptor) {
      ((Mouse) actionExecutor).mouseUp(getCoordinetes(lastProcessedElement));
      return lastProcessedElement;
    }
  },
  POINTER_MOVE {
    @Override
    Element perform(Object actionExecutor, Element lastProcessedElement, Map<String, Object> descriptor) {
      int x = (int) descriptor.get("x");
      int y = (int) descriptor.get("y");
      Element currentElement = (Element) descriptor.get("origin");
      ((Mouse) actionExecutor).mouseMove(currentElement.getCoordinates(), x, y);
      return currentElement;
    }
  },
  POINTER_CANCEL {
    @Override
    Element perform(Object actionExecutor, Element lastProcessedElement, Map<String, Object> descriptor) {
      throw new UnsupportedOperationException("Action 'pointerCancel' is not supported");
    }
  };

  private final String type;

  private W3CActions() {
    this.type = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name());
  }

  abstract Element perform(Object actionExecutor, Element lastProcessedElement, Map<String, Object> descriptor);

  Coordinates getCoordinetes(Element element)
  {
    return element == null ? null : element.getCoordinates();
  }
  
  static W3CActions findActionByType(String type) {
    return Arrays.stream(W3CActions.values()).filter(v -> v.type.equals(type)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("There is no action with type " + type));
  }
}
