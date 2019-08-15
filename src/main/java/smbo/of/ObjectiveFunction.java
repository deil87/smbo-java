package smbo.of;


import smbo.EvaluatedGridEntry;
import smbo.GridEntry;

public abstract class ObjectiveFunction {

  public abstract EvaluatedGridEntry evaluate(GridEntry entry);
}
