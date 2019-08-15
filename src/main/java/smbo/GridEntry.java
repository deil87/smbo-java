package smbo;

import java.util.Map;

public class GridEntry {
  Map<String, Object> _item;
  int _hash;

  public Map<String, Object> getEntry() {
    return _item;
  }

  public int getHash() {
    return _hash;
  }

  public GridEntry(Map<String, Object> item, int hash) {
    _item = item;
    _hash = hash;

  }
}
