package smbo.of;

import smbo.EvaluatedGridEntry;
import smbo.GridEntry;

public class ComplexPeriodicOF extends ObjectiveFunction{

  public EvaluatedGridEntry evaluate(GridEntry entry) {
    Double xVal = (Double)entry.getEntry().get("X");
    double secondFun = xVal > 2 ? Math.sqrt(xVal) * 1.25 : Math.sin(xVal + 2) * 2;

    double result = 3 * Math.exp( -xVal)* Math.cos(2 * Math.PI * xVal) + secondFun ;
    return new EvaluatedGridEntry(entry, result);
  }
}
