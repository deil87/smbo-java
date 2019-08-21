package smbo;

import java.util.SortedMap;

public class GridEntry {
  SortedMap<String, Object> _item;
  int _hash;

  public SortedMap<String, Object> getEntry() {
    return _item;
  }

  public int getHash() {
    return _hash;
  }

  public GridEntry(SortedMap<String, Object> item, int hash) {
    _item = item;
    _hash = hash;

  }
}
