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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import netscape.javascript.JSObject;

class Frames {
  private Frame root;

  long rootId() {
    return root == null ? 0 : root.id;
  }

  void reset(long id) {
    Frame frame = findFrame(root, id);
    if (frame != null) {
      frame.children.clear();
    }
  }

  void add(long id, JSObject doc, JSObject owner, long parentId) {
    Frame frame = new Frame(id, doc, owner, parentId);
    Frame foundFrame = findFrame(root, id);
    if (foundFrame != null) {
      frame.children.addAll(foundFrame.children);
    }
    if (root == null || id == root.id) {
      root = frame;
    } else {
      Frame foundParent = findFrame(root, parentId);
      if (foundParent != null) {
        foundParent.children.remove(frame);
        foundParent.children.add(frame);
      }
    }
  }

  boolean conatins(JSObject doc) {
    return findId(root, doc) != 0;
  }

  long id(JSObject doc) {
    return findId(root, doc);
  }

  List<Long> ancestors(long frameId) {
    List<Long> ancestors = new ArrayList<Long>();
    while (true) {
      Frame frame = findFrame(root, frameId);
      if (frame == null || frame.parentId == 0) {
        break;
      }
      ancestors.add(frame.parentId);
      frameId = frame.parentId;
    }
    return ancestors;
  }

  private long findId(Frame cur, JSObject toFind) {
    if (cur == null) {
      return 0;
    }
    if (cur.doc.equals(toFind) || (cur.owner != null && cur.owner.equals(toFind))) {
      return cur.id;
    }
    for (Frame next : cur.children) {
      long id = findId(next, toFind);
      if (id != 0) {
        return id;
      }
    }
    return 0;
  }

  private Frame findFrame(Frame cur, long toFind) {
    if (cur == null) {
      return null;
    }
    if (cur.id == toFind) {
      return cur;
    }
    for (Frame next : cur.children) {
      Frame found = findFrame(next, toFind);
      if (found != null) {
        return found;
      }
    }
    return null;
  }

  private static final class Frame {
    final Long id;
    final JSObject doc;
    final JSObject owner;
    final Long parentId;
    final Set<Frame> children = new HashSet<Frame>();

    Frame(long id, JSObject doc, JSObject owner, long parentId) {
      this.id = id;
      this.doc = doc;
      this.owner = owner;
      this.parentId = parentId;
    }

    @Override
    public int hashCode() {
      return id.hashCode();
    }

    @Override
    public boolean equals(Object other) {
      if (other instanceof Frame) {
        return id.equals(((Frame) other).id);
      }
      return false;
    }

    @Override
    public String toString() {
      return id.toString();
    }
  }
}
