package smbo;

import org.jblas.DoubleMatrix;
import org.jblas.MatrixFunctions;
import org.jblas.ranges.IntervalRange;
import smbo.of.ObjectiveFunction;

import java.io.IOException;
import java.util.*;

/**
 * This class aggregates logic that selects best suggestion from Surrogate model based on acquisition function
 */
public class GPSMBO extends SMBO<GPSurrogateModel, AcquisitionFunction> { // TODO maybe we don't need generic type AcquisitionFunction

  ObjectiveFunction _of;
  // Defaults
  int priorSize = 5;
  //Move to SMBO
  RandomSelector _randomSelector;

  List<GPSurrogateModel.MeanVariance> meanVariancesHistory = new ArrayList<>();

  private boolean theBiggerTheBetter;

  public GPSMBO(ObjectiveFunction of, HashMap<String, Object[]> grid, boolean theBiggerTheBetter, long seed) {
    this(of, grid, new MaxImprovementAF(theBiggerTheBetter), theBiggerTheBetter, seed);
  }

  public GPSMBO(ObjectiveFunction of, HashMap<String, Object[]> grid, AcquisitionFunction af, boolean theBiggerTheBetter, long seed) {
    super(grid);
    _of = of;
    _randomSelector = new RandomSelector(grid, seed);
//    _surrogateModel = new GPSurrogateModel(0.6, 0.5);
    _surrogateModel = new GPSurrogateModel(0.6, 10);  // TODO default sigma and ell are being used
    _acquisitionFunction = af;
    _theBiggerTheBetter = theBiggerTheBetter;
  }


  /**
   * In general only SMBO should handle prior knowledge and Surrogate model can be oblivious about prior. We just need API to update SM with new evaluation from OF.
   *  newObservationFromObjectiveFun    is a row matrix    (   X       Y  )
   * */
  public void updatePrior(DoubleMatrix newObservationFromObjectiveFun){
    assert newObservationFromObjectiveFun.rows == 1;

    // Step 1 Update Surrogate model underneath SMBO. Order does matter. Now _observedGridEntries does not have newObservationFromObjectiveFun in it,
    surrogateModel().updateCovariancePrior(_observedGridEntries, newObservationFromObjectiveFun);

    // Step 2
    if(_observedGridEntries.rows == 0) {
      _observedGridEntries = newObservationFromObjectiveFun;
    } else {
      _observedGridEntries = DoubleMatrix.concatVertically(_observedGridEntries, newObservationFromObjectiveFun);
    }

    // Step 3 Update Acquisition function with fresh incumbent candidate
    AcquisitionFunction acquisitionFunction = acquisitionFunction();
    if(!acquisitionFunction.isIncumbentColdStartSetupHappened()) {
      DoubleMatrix bestFromPrior = selectBestBasedOnResponse(_observedGridEntries);

      double newIncumbent = bestFromPrior.get(0, bestFromPrior.columns - 1);
      acquisitionFunction.setIncumbent(newIncumbent);

    } else {
      acquisitionFunction.updateIncumbent(newObservationFromObjectiveFun.get(0, newObservationFromObjectiveFun.columns -1));
    }
  }

  DoubleMatrix selectBestBasedOnResponse(DoubleMatrix observedGridEntries) {
    int indexOfTheRowWithBestResponse = observedGridEntries.getColumn(observedGridEntries.columns - 1).columnArgmaxs()[0];
    return observedGridEntries.getRow(indexOfTheRowWithBestResponse);
  }

  public EvaluatedGridEntry evaluateWithObjectiveFunction(GridEntry entry) {
    return _of.evaluate(entry);
  }

  public EvaluatedGridEntry objectiveFunction(DoubleMatrix entry) {
    assert entry.rows == 1;
    Map<String, Object> map = new HashMap<>();
    for(double value : entry.toArray()) {
      map.put("X1", value);
    }
    return evaluateWithObjectiveFunction(new GridEntry(map, 0));
  }


  public DoubleMatrix getNextBestCandidateForEvaluation() throws SMBOSearchCompleted, IOException {

    // Calculation of prior batch. Default size is 10
    if(_observedGridEntries.rows == 0) {
      initializePriorOfSMBOWithBatchEvaluation();
      materializeGrid();
    }

    if(_unObservedGridEntries.rows == 0) throw new SMBOSearchCompleted();

    GPSurrogateModel gpSurrogateModel = _surrogateModel;

    // We should evaluate only ones as it is up to the user how many times he needs to get best next suggestion. Be lazy.
    //TODO we can probably reuse suggestions from all previous iterations and do better on our predictions. Maybe not for GP implementation.
    GPSurrogateModel.MeanVariance meanVariance = gpSurrogateModel.evaluate(_observedGridEntries, _unObservedGridEntries.transpose()); // TODO should we always keep it transposed?

    //Saving this to be able to draw confidence intervals and see predictions
    meanVariancesHistory.add(meanVariance);

    MeanVariancePlotHelper.plotWithVarianceIntervals(_unObservedGridEntries, _observedGridEntries, null, meanVariance,  this);

    // Getting acquisition function evaluations for all suggestions from surrogate model
    DoubleMatrix afEvaluations = acquisitionFunction().compute(meanVariance.getMean(), meanVariance.getVariance());


    int bestIndex = selectBest( afEvaluations);

    DoubleMatrix bestCandidateGridEntry = null;
    try {
      bestCandidateGridEntry = _unObservedGridEntries.getRow(bestIndex);
    } catch (Exception ex) {
      System.out.println();
    }
    dropSuggestionFromUnObservedGridEntries(bestIndex); // this is done for optimisation reasons. As soon we suggested item it is considered to be observed by caller.
    return bestCandidateGridEntry;
  }

  void dropSuggestionFromUnObservedGridEntries(int bestIndex) {
    DoubleMatrix rowsBeforeBestIndex = _unObservedGridEntries.getRows(new IntervalRange(0, bestIndex));
    DoubleMatrix rowsAfterBestIndex = _unObservedGridEntries.getRows(new IntervalRange(bestIndex + 1, _unObservedGridEntries.rows));
    _unObservedGridEntries = DoubleMatrix.concatVertically(rowsBeforeBestIndex, rowsAfterBestIndex);
  }

  //materialise rest of the grid
  void materializeGrid() {
    System.out.println("Starting to materialize grid...  ( happens as an init phase during first call of `getNextBestCandidateForEvaluation`)");
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
      System.out.println("Random selector stopped with NoUnexploredGridEntitiesLeft.");
    }
  }

  void initializePriorOfSMBOWithBatchEvaluation() throws SMBOSearchCompleted {
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
        nextRowToAppend[colIdx] = evaluateWithObjectiveFunction(next).evaluatedRes;
        DoubleMatrix newObservedGridEntry = new DoubleMatrix(1, nextRowToAppend.length, nextRowToAppend);
        _observedGridEntries = _observedGridEntries.rows == 0 ? newObservedGridEntry : DoubleMatrix.concatVertically(_observedGridEntries, newObservedGridEntry);
      }
    } catch (RandomSelector.NoUnexploredGridEntitiesLeft ex) {
      throw new SMBOSearchCompleted(); // suggest user to set smaller prior or not to use SMBO
    }
  }

  int selectBest(DoubleMatrix afEvaluations) {
    int indexOfTheBiggestMeanImprovement = afEvaluations.argmax();
    if(!(indexOfTheBiggestMeanImprovement >= 0 && indexOfTheBiggestMeanImprovement < afEvaluations.rows)) {
      throw new IllegalStateException("Index for best suggestion from af evaluations is out of range");
    }
    return indexOfTheBiggestMeanImprovement;
//    return means.get(indexOfTheBiggestMeanImprovement);
  }

  // Helper method that will evaluate multiple rows with OF
  public static DoubleMatrix evaluateRowsWithOF(GPSMBO gpsmbo, DoubleMatrix unObservedGridEntries) {
    DoubleMatrix YValDM = null;
    for(DoubleMatrix row :unObservedGridEntries.rowsAsList()) {
      EvaluatedGridEntry evaluatedGridEntry = gpsmbo.objectiveFunction(row);
      DoubleMatrix evaluationDM = evaluatedGridEntry.getEvaluatedEntryAsMtx().getColumn(row.columns);
      if(YValDM == null) {
        YValDM = evaluationDM;
      } else {
        YValDM = DoubleMatrix.concatVertically(YValDM, evaluationDM);
      }
    }
//    YValDM.print();
    return YValDM;
  }

  @Override
  public GPSurrogateModel surrogateModel() {
    return _surrogateModel;
  }

  @Override
  public AcquisitionFunction acquisitionFunction() {
    return _acquisitionFunction;
  }
}
