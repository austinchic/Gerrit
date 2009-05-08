// Copyright (C) 2009 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.gerrit.client.data;


import org.spearce.jgit.diff.Edit;

import java.util.Iterator;
import java.util.List;

public class PatchScript {
  protected List<String> header;
  protected int context;
  protected SparseFileContent a;
  protected SparseFileContent b;
  protected List<Edit> edits;

  public PatchScript(final List<String> h, final int ctx,
      final SparseFileContent ca, final SparseFileContent cb, final List<Edit> e) {
    header = h;
    context = ctx;
    a = ca;
    b = cb;
    edits = e;
  }

  protected PatchScript() {
  }

  public List<String> getPatchHeader() {
    return header;
  }

  public int getContext() {
    return context;
  }

  public SparseFileContent getA() {
    return a;
  }

  public SparseFileContent getB() {
    return b;
  }

  public List<Edit> getEdits() {
    return edits;
  }

  public Iterable<Hunk> getHunks() {
    return new Iterable<Hunk>() {
      public Iterator<Hunk> iterator() {
        return new Iterator<Hunk>() {
          private int curIdx;

          public boolean hasNext() {
            return curIdx < edits.size();
          }

          public Hunk next() {
            final int c = curIdx;
            final int e = findCombinedEnd(c);
            curIdx = e + 1;
            return new Hunk(c, e);
          }

          public void remove() {
            throw new UnsupportedOperationException();
          }
        };
      }
    };
  }

  private int findCombinedEnd(final int i) {
    int end = i + 1;
    while (end < edits.size() && (combineA(end) || combineB(end)))
      end++;
    return end - 1;
  }

  private boolean combineA(final int i) {
    return edits.get(i).getBeginA() - edits.get(i - 1).getEndA() <= 2 * context;
  }

  private boolean combineB(final int i) {
    return edits.get(i).getBeginB() - edits.get(i - 1).getEndB() <= 2 * context;
  }

  private static boolean end(final Edit edit, final int a, final int b) {
    return edit.getEndA() <= a && edit.getEndB() <= b;
  }

  public class Hunk {
    private int curIdx;
    private Edit curEdit;
    private final int endIdx;
    private final Edit endEdit;

    private int aCur;
    private int bCur;
    private final int aEnd;
    private final int bEnd;

    private Hunk(final int ci, final int ei) {
      curIdx = ci;
      endIdx = ei;
      curEdit = edits.get(curIdx);
      endEdit = edits.get(endIdx);

      aCur = Math.max(0, curEdit.getBeginA() - context);
      bCur = Math.max(0, curEdit.getBeginB() - context);
      aEnd = Math.min(a.size(), endEdit.getEndA() + context);
      bEnd = Math.min(b.size(), endEdit.getEndB() + context);
    }

    public int getCurA() {
      return aCur;
    }

    public int getCurB() {
      return bCur;
    }

    public int getEndA() {
      return aEnd;
    }

    public int getEndB() {
      return bEnd;
    }

    public void incA() {
      aCur++;
    }

    public void incB() {
      bCur++;
    }

    public void incBoth() {
      incA();
      incB();
    }

    public boolean isStartOfFile() {
      return aCur == 0 && bCur == 0;
    }

    public boolean hasNextLine() {
      return aCur < aEnd || bCur < bEnd;
    }

    public boolean isContextLine() {
      return aCur < curEdit.getBeginA() || endIdx + 1 < curIdx;
    }

    public boolean isDeletedA() {
      return aCur < curEdit.getEndA();
    }

    public boolean isInsertedB() {
      return bCur < curEdit.getEndB();
    }

    public boolean isModifiedLine() {
      return isDeletedA() || isInsertedB();
    }

    public void next() {
      if (end(curEdit, aCur, bCur) && ++curIdx < edits.size()) {
        curEdit = edits.get(curIdx);
      }
    }
  }
}