package smbo.of;

import smbo.EvaluatedGridEntry;
import smbo.GridEntry;

import java.util.Map;

public class SinOF_2D extends ObjectiveFunction{

  public EvaluatedGridEntry evaluate(GridEntry entry) {
//    double[] row = new double[entry.getEntry().size() + 1]; // TODO why plus one?
//    int colIdx = 0;
//
//    for(Map.Entry<String, Object> ge: entry.getEntry().entrySet()) {
//
//      row[colIdx] = (double) ge.getValue();
//      colIdx++;
//    }
//    DoubleMatrix features = new DoubleMatrix(1, row.length, row);

    Map<String, Object> gridMap = entry.getEntry();
    Double xVal = (Double) gridMap.get("X");
    Double yVal = (Double) gridMap.get("Y");
    double result = Math.sin(xVal / 2.5) * 5 + Math.sin(yVal * 3);
    return new EvaluatedGridEntry(entry, result);
  }
}
