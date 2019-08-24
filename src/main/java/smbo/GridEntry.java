package smbo;

import org.jblas.DoubleMatrix;

import java.util.Map;
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

  //TODO no test
  public DoubleMatrix getEntryAsMtx() {

    int numberOfFeatures = _item.size();
    double[] row = new double[numberOfFeatures];
    int colIdx = 0;
    for(Map.Entry<String, Object> ge: _item.entrySet()) {
      row[colIdx] = (double) ge.getValue();
      colIdx++;
    }
    return new DoubleMatrix(1, row.length, row);
  }
}
