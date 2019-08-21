package smbo;

import org.jblas.DoubleMatrix;

import java.util.Map;

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
    assert evaluatedRes != Double.MIN_VALUE : "EvaluatedGridEntry was not evaluated";

    int numberOfFeatures = getEntry()._item.size();
    double[] evaluatedRow = new double[numberOfFeatures + 1];
    int colIdx = 0;
    for(Map.Entry<String, Object> ge: getEntry()._item.entrySet()) {
      evaluatedRow[colIdx] = (double) ge.getValue();
      colIdx++;
    }
    evaluatedRow[numberOfFeatures] = evaluatedRes;
    return new DoubleMatrix(1, evaluatedRow.length, evaluatedRow);
  }
}
