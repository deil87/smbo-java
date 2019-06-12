package smbo;

import org.jblas.DoubleMatrix;
import org.jblas.ranges.IntervalRange;
import utils.TestUtils;

import java.util.*;

/**
 * This class aggregates logic that selects best suggestion from Surrogate model based on acquisition function
 */
public class GPSMBO extends SMBO<GPSurrogateModel, MaxImprovementAF> {

  private final GPSurrogateModel _surrogateModel;
  private final MaxImprovementAF _acquisitionFunction;
  private final boolean _theBiggerTheBetter;

  // Defaults
  int priorSize = 10;
  //Move to SMBO
  RandomSelector _randomSelector;
  //Move to SMBO

  private boolean theBiggerTheBetter;

  public GPSMBO(HashMap<String, Object[]> grid, boolean theBiggerTheBetter, long seed) {
    super(grid);
    _randomSelector = new RandomSelector(grid, seed);

    _surrogateModel = new GPSurrogateModel();
    _acquisitionFunction = new MaxImprovementAF(theBiggerTheBetter);
    _theBiggerTheBetter = theBiggerTheBetter;
  }


  /**
   * In general only SMBO should handle prior knowledge and Surrogate model can be oblivious about prior. We just need API to update SM with new evaluation from OF.
   *  newObservationFromObjectiveFun    is a row matrix    (   X       Y  )
   * */
  public void updatePrior(DoubleMatrix newObservationFromObjectiveFun){
    assert newObservationFromObjectiveFun.rows == 1;

    if(_observedGridEntries.rows == 0) {
      _observedGridEntries = newObservationFromObjectiveFun;
    } else {
      _observedGridEntries = DoubleMatrix.concatVertically(_observedGridEntries, newObservationFromObjectiveFun);
    }

//    DoubleMatrix updatedPrior = surrogateModel().updateCovariancePrior(_observedGridEntries, newObservationFromObjectiveFun);

    AcquisitionFunction acquisitionFunction = acquisitionFunction();
    if(!acquisitionFunction.isIncumbentColdStartSetupHappened()) {
      int indexOfTheRowWithBestResponse = _observedGridEntries.getColumn(_observedGridEntries.columns - 1).columnArgmaxs()[0];
      DoubleMatrix bestFromPrior = _observedGridEntries.getRow(indexOfTheRowWithBestResponse);

      acquisitionFunction.setIncumbent(bestFromPrior.get(0, bestFromPrior.columns -1));

    } else {
      acquisitionFunction.updateIncumbent(newObservationFromObjectiveFun.get(0, newObservationFromObjectiveFun.columns));
    }
  }

  // Hardcoded OF for now. Should be defined by user. Make abstract class to force user.
  public EvaluatedGridEntry objectiveFunction(GridEntry entry) {
    Double xVal = (Double)entry.getEntry().get("X");
    double result = Math.sin(xVal / 2.5) * 5 ;
    return new EvaluatedGridEntry(entry, result);
  }


  public DoubleMatrix getNextBestCandidateForEvaluation() throws SMBOSearchCompleted {

    // Calculation of prior batch. Default size is 10
    if(_observedGridEntries.rows == 0) {
      initializePriorOfSMBOWIthBatchEvaluation();
      materializeGrid();
    }

    if(_unObservedGridEntries.rows == 0) throw new SMBOSearchCompleted();

    GPSurrogateModel gpSurrogateModel = new GPSurrogateModel(); // TODO default sigma and ell are being used

    // We should evaluate only ones as it is up to the user how many times he needs to get best next suggestion. Be lazy.
    //TODO we can probably reuse suggestions from all previous iterations and do better on our predictions. Maybe not for GP implementation.
    GPSurrogateModel.MeanVariance meanVariance = gpSurrogateModel.evaluate(_observedGridEntries, _unObservedGridEntries.transpose()); // TODO should we always keep it transposed?


    // Getting acquisition function evaluations for all suggestions from surrogate model
    DoubleMatrix afEvaluations = acquisitionFunction().compute(meanVariance.getMean(), meanVariance.getVariance());


    int bestIndex = selectBest(meanVariance.getMean(), afEvaluations);

    DoubleMatrix bestCandidateGridEntry = _unObservedGridEntries.getRow(bestIndex);
    dropSuggestionFromUnObservedGridEntries(bestIndex);
    return bestCandidateGridEntry;
  }

  void dropSuggestionFromUnObservedGridEntries(int bestIndex) {
    DoubleMatrix rowsBeforeBestIndex = _unObservedGridEntries.getRows(new IntervalRange(0, bestIndex));
    DoubleMatrix rowsAfterBestIndex = _unObservedGridEntries.getRows(new IntervalRange(bestIndex + 1, _unObservedGridEntries.rows));
    _unObservedGridEntries = DoubleMatrix.concatVertically(rowsBeforeBestIndex, rowsAfterBestIndex);
  }

  //materialise rest of the grid
  void materializeGrid() {
    try {
      while (true) {
        GridEntry next = _randomSelector.getNext();
        double[] nextRowToAppend = new double[next.getEntry().size()];
        int colIdx = 0;

        assert next.getEntry().size() == 1 : "order of the entries of the map is not guaranteed. Consider to change representation of the GridEntry or control the way how we fill double[]";
        for(Map.Entry<String, Object> entry: next.getEntry().entrySet()) {

          nextRowToAppend[colIdx] = (double) entry.getValue();
          colIdx++;
        }
        DoubleMatrix newMaterializedGridEntry = new DoubleMatrix(1, nextRowToAppend.length, nextRowToAppend);
        _unObservedGridEntries = _unObservedGridEntries.rows == 0 ? newMaterializedGridEntry : DoubleMatrix.concatVertically(_unObservedGridEntries, newMaterializedGridEntry);
      }
    } catch (RandomSelector.NoUnexploredGridEntitiesLeft ex) {
      System.out.println("Random selector stopped with NoUnexploredGridEntitiesLeft");
    }
  }

  void initializePriorOfSMBOWIthBatchEvaluation() throws SMBOSearchCompleted {
    try {
      for (int j = 0; j < priorSize; j++) {
        // Select randomly and get evaluations from our objective function
        GridEntry next = _randomSelector.getNext();
        double[] nextRowToAppend = new double[next.getEntry().size() + 1];
        int colIdx = 0;

        assert next.getEntry().size() == 1 : "order of the entries of the map is not guaranteed. Consider to change representation of the GridEntry or control the way how we fill double[]";
        for(Map.Entry<String, Object> entry: next.getEntry().entrySet()) {

          nextRowToAppend[colIdx] = (double) entry.getValue();
          colIdx++;
        }
        nextRowToAppend[colIdx] = objectiveFunction(next).evaluatedRes;
        DoubleMatrix newObservedGridEntry = new DoubleMatrix(1, nextRowToAppend.length, nextRowToAppend);
        _observedGridEntries = _observedGridEntries.rows == 0 ? newObservedGridEntry : DoubleMatrix.concatVertically(_observedGridEntries, newObservedGridEntry);
      }
    } catch (RandomSelector.NoUnexploredGridEntitiesLeft ex) {
      throw new SMBOSearchCompleted(); // suggest user to set smaller prior or not to use SMBO
    }
  }

  int selectBest(DoubleMatrix means, DoubleMatrix afEvaluations) {
    int indexOfTheBiggestMeanImprovement = afEvaluations.argmax();
    return indexOfTheBiggestMeanImprovement;
//    return means.get(indexOfTheBiggestMeanImprovement);
  }

  @Override
  public GPSurrogateModel surrogateModel() {
    return _surrogateModel;
  }

  @Override
  public MaxImprovementAF acquisitionFunction() {
    return _acquisitionFunction;
  }
}
