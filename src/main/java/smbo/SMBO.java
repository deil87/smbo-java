package smbo;

import org.jblas.DoubleMatrix;

import java.io.IOException;
import java.util.HashMap;

public abstract class SMBO<SM extends SurrogateModel, AF extends AcquisitionFunction> {

  protected SM _surrogateModel;
  protected AF _acquisitionFunction;
  protected boolean _theBiggerTheBetter;

  public HashMap<String, Object[]> getGrid() {
    return _grid;
  }

  private final HashMap<String, Object[]> _grid;

  public SMBO(HashMap<String, Object[]> grid) {
    _grid = grid;
  }

  public DoubleMatrix getObservedGridEntries() {
    return _observedGridEntries;
  }

  public DoubleMatrix getUnObservedGridEntries() {
    return _unObservedGridEntries;
  }

  /** Observed grid entries stored with means/responses
   *
   *   (  X1   X2   X3   ....   Xn  Y  )
   *   (  1   3.5   6    ....   2   42 )
   *   (  2   3     6.8  ....   5   46 )
   *
   */

  protected DoubleMatrix _observedGridEntries = new DoubleMatrix(); // maybe better to store it as matrix ( X1 X2 X3 Y);

  /** UnObserved grid entries sotred with means/responses
   *
   *   (  X1   X2   X3   ....   Xn  )
   *   (  1   3.5   6    ....   2   )
   *   (  2   3     6.8  ....   5   )
   *
   */
  protected DoubleMatrix _unObservedGridEntries = new DoubleMatrix();  // maybe better to store it as matrix ( X1 X2 X3)


  public DoubleMatrix prior() {
    return _observedGridEntries;
  };

  public boolean hasNoPrior() {
    return _observedGridEntries.rows == 0 ;
  }

  public abstract void updatePrior(DoubleMatrix newObservationFromObjectiveFun);

  public abstract SM surrogateModel();
  
  public abstract AF acquisitionFunction();

  public abstract EvaluatedGridEntry evaluateWithObjectiveFunction(GridEntry entry);

  public abstract DoubleMatrix getNextBestCandidateForEvaluation() throws SMBOSearchCompleted, IOException;

  public static class SMBOSearchCompleted extends Exception{ }

}
