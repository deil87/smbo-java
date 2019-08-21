package smbo.of;

import smbo.EvaluatedGridEntry;
import smbo.GridEntry;

public class SqrtOF extends ObjectiveFunction{

  public EvaluatedGridEntry evaluate(GridEntry entry) {
    Double xVal = (Double)entry.getEntry().get("X");
    double result = Math.sqrt(xVal / 2.5) * 5 ;
    return new EvaluatedGridEntry(entry, result);
  }
}
