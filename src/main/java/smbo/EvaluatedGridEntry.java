package smbo;

import org.jblas.DoubleMatrix;

public class EvaluatedGridEntry {
  GridEntry _entry;

  double evaluatedRes = Double.MIN_VALUE;


  public EvaluatedGridEntry(GridEntry item, double value) {
    _entry = item;
    evaluatedRes = value;

  }

  public GridEntry getEntry() {
    return _entry;
  }

  public DoubleMatrix getEvaluatedEntryAsMtx() {
    assert getEntry()._item.size() == 1 : "We do not support multi-variable cases yet. For that we will need to guarantee order of the keys in the Map.";
    assert evaluatedRes != Double.MIN_VALUE : "EvaluatedGridEntry was not evaluated";
    return new DoubleMatrix(1, 2, (double) getEntry()._item.get("X1"), evaluatedRes);
  }
}
