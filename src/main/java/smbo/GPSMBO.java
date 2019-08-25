package smbo;

import org.jblas.DoubleMatrix;
import org.jblas.ranges.IntervalRange;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.internal.chartpart.Chart;
import smbo.kernel.RationalQuadraticKernel;
import smbo.of.ObjectiveFunction;
import utils.DoubleMatrixUtils;

import java.io.IOException;
import java.util.*;

/**
 * This class aggregates logic that selects best suggestion from Surrogate model based on acquisition function
 */
public class GPSMBO extends SMBO<GPSurrogateModel, AcquisitionFunction> { // TODO maybe we don't need generic type AcquisitionFunction

  private ObjectiveFunction _of;
  // Defaults
  private final int priorSize;

  public RandomSelector getRandomSelector() {
    return _randomSelector;
  }

  private RandomSelector _randomSelector;   //TODO Move to SMBO

  final private String[] gridKeysOriginalOrder; // TODO Maybe move it to SMBO? it does not belong to SMBO abtraction but we need it in all implementations of SMBO.

  public List<Chart> getMeanVarianceCharts() {
    return meanVarianceCharts;
  }

  List<GPSurrogateModel.MeanVariance> meanVariancesHistory = new ArrayList<>();

  private boolean theBiggerTheBetter;

  boolean keepMeanHistory = false;

  List<Chart> meanVarianceCharts = new ArrayList<Chart>();

  /**
   *  Default constructor with MaxImprovementAF acquisition function
   * @param of
   * @param grid
   * @param theBiggerTheBetter
   * @param seed
   */
  public GPSMBO(ObjectiveFunction of, SortedMap<String, Object[]> grid, boolean theBiggerTheBetter, int priorSize, long seed) {
    this(of, grid, new MaxImprovementAF(theBiggerTheBetter), theBiggerTheBetter, priorSize, seed);
  }

  public GPSMBO(ObjectiveFunction of, SortedMap<String, Object[]> grid, boolean theBiggerTheBetter, long seed) {
    this(of, grid, new MaxImprovementAF(theBiggerTheBetter), theBiggerTheBetter, 5, seed);
  }

  public GPSMBO(ObjectiveFunction of, SortedMap<String, Object[]> grid, AcquisitionFunction af, boolean theBiggerTheBetter, long seed) {
    this(of, grid, af, theBiggerTheBetter, 5, seed);
  }

  public GPSMBO(ObjectiveFunction of, SortedMap<String, Object[]> grid, AcquisitionFunction af, boolean theBiggerTheBetter, int priorSize, long seed) {
    super(grid);

    this.priorSize = priorSize;

    gridKeysOriginalOrder = grid.keySet().toArray(new String[]{});
    if(gridKeysOriginalOrder.length > 1) System.out.println("XChart does not support 3D plots and we can't display more then one feature");

    _of = of;
    _randomSelector = new RandomSelector(grid, seed);
//    _surrogateModel = new GPSurrogateModel(0.6, 0.5);
//    _surrogateModel = new GPSurrogateModel(0.6, 10);  // TODO default sigma and ell are being used
    _surrogateModel = new GPSurrogateModel(new RationalQuadraticKernel(12),0.2, 6.4, 0.01);  // TODO default sigma and ell are being used
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
    updateIncumbentBasedOnNewObservation(newObservationFromObjectiveFun);
  }

  public void updateIncumbentBasedOnNewObservation(DoubleMatrix newObservationFromObjectiveFun) {
    AcquisitionFunction acquisitionFunction = acquisitionFunction();
    if(!acquisitionFunction.isIncumbentColdStartSetupHappened()) {
      updateIncumbentBasedOnObserved();

    } else {
      acquisitionFunction.updateIncumbent(newObservationFromObjectiveFun.get(0, newObservationFromObjectiveFun.columns -1));
    }
  }

  private void updateIncumbentBasedOnObserved() {
    AcquisitionFunction acquisitionFunction = acquisitionFunction();
    DoubleMatrix bestFromPrior = selectBestBasedOnResponse(_observedGridEntries);

    double newIncumbent = bestFromPrior.get(0, bestFromPrior.columns - 1);
    acquisitionFunction.setIncumbent(newIncumbent);
  }

  // TODO we probably don't need `observedGridEntries` parameter
  DoubleMatrix selectBestBasedOnResponse(DoubleMatrix observedGridEntries) {
    int indexOfTheRowWithBestResponse = observedGridEntries.getColumn(observedGridEntries.columns - 1).columnArgmaxs()[0];
    return observedGridEntries.getRow(indexOfTheRowWithBestResponse);
  }

  // TODO we need to support `the bigger the better`
  // TODO we probably don't need `observedGridEntries` parameter
  public int selectBestIndexBasedOnResponse(DoubleMatrix observedGridEntries) {
    int[] argMaxes = observedGridEntries.getColumn(observedGridEntries.columns - 1).columnArgmaxs();
    int indexOfTheRowWithBestResponse = argMaxes[0];
    return indexOfTheRowWithBestResponse;
  }

  public EvaluatedGridEntry evaluateWithObjectiveFunction(GridEntry entry) {
    return _of.evaluate(entry);
  }

  // Note: it is assumed that order of columns in {@code entry} was not changed and match order from SortedMap representation of grid entry
  public EvaluatedGridEntry objectiveFunction(DoubleMatrix entry) {

    SortedMap<String, Object> gridEntryAsMap = Collections.synchronizedSortedMap(new TreeMap());
    int columnIdx = 0;
    for(double value : entry.toArray()) {
      String originalFeatureName = gridKeysOriginalOrder[columnIdx];
      gridEntryAsMap.put(originalFeatureName, value);
      columnIdx++;
    }

    return evaluateWithObjectiveFunction(new GridEntry(gridEntryAsMap, 0));
  }


  public DoubleMatrix getNextBestCandidateForEvaluation() throws SMBOSearchCompleted, IOException {

    GPSurrogateModel gpSurrogateModel = _surrogateModel;

    // Calculation of prior batch.
    if(_observedGridEntries.rows == 0) {
      initializePriorWithBatchEvaluation();
//      initializeDiversePriorOfSMBOWithBatchEvaluation(); //TODO consider to remove commented

      DoubleMatrix onlyFeatures = _observedGridEntries.getColumns(new IntervalRange(0, _observedGridEntries.columns - 1));
      DoubleMatrix onlyMeans = _observedGridEntries.getColumn(_observedGridEntries.columns - 1);
      gpSurrogateModel.performHpsGridSearchAndUpdateHps(onlyFeatures,onlyMeans);
      updateIncumbentBasedOnObserved();
      MaterialisedGrid materialisedGrid = materializeGrid(_randomSelector, _observedGridEntries.rows);
      _unObservedGridEntries = materialisedGrid.unObservedGridEntries;
    }

    if(_unObservedGridEntries.rows == 0) throw new SMBOSearchCompleted();


    // We should evaluate only ones as it is up to the user how many times he needs to get best next suggestion. Be lazy.
    //TODO we can probably reuse suggestions from all previous iterations and do better on our predictions. Maybe not for GP implementation.
    GPSurrogateModel.MeanVariance meanVariance = gpSurrogateModel.predictMeansAndVariances(_observedGridEntries, _unObservedGridEntries.transpose()); // TODO should we always keep it transposed?

    //Saving this to be able to draw confidence intervals and see predictions
    if(keepMeanHistory) meanVariancesHistory.add(meanVariance);

    // Getting acquisition function evaluations for all suggestions from surrogate model
    DoubleMatrix afEvaluations = acquisitionFunction().compute(meanVariance.getMean(), meanVariance.getVariance());

    if(gridKeysOriginalOrder.length == 1) {
      XYChart meanVarianceChart = MeanVariancePlotHelper.plotWithVarianceIntervals(_unObservedGridEntries, _observedGridEntries, null, meanVariance, this);
      meanVarianceCharts.add(meanVarianceChart);
      //TODO we probably want to draw acquisition function as well
      DoubleMatrix meanAndVariance = DoubleMatrix.concatHorizontally(meanVariance.getMean(), meanVariance.getVariance());
      DoubleMatrix combinedForDisplayingMtx = DoubleMatrix.concatHorizontally(DoubleMatrix.concatHorizontally(_unObservedGridEntries, meanAndVariance), afEvaluations);
      DoubleMatrixUtils.multilinePrint("Features [0...N-2], Mean and Variance ( observed = " + _observedGridEntries.rows + " )", combinedForDisplayingMtx);
    }
    else {
      //TODO duplicate 3rows ^^^
      DoubleMatrix meanAndVariance = DoubleMatrix.concatHorizontally(meanVariance.getMean(), meanVariance.getVariance());
      DoubleMatrix combinedForDisplayingMtx = DoubleMatrix.concatHorizontally(DoubleMatrix.concatHorizontally(_unObservedGridEntries, meanAndVariance), afEvaluations);
      DoubleMatrixUtils.multilinePrint("Features [0...N-2], Mean and Variance ( observed = " + _observedGridEntries.rows + " )", combinedForDisplayingMtx);
    }

    int bestIndex = selectBestIndex( afEvaluations);

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
  public MaterialisedGrid materializeGrid(RandomSelector randomSelector, int numberOfObserved) {
    System.out.println("Starting to materialize grid...  ( happens as an init phase during first call of `getNextBestCandidateForEvaluation`)");
    DoubleMatrix unObservedGridEntries = null;
    int[] hashesForUnObservedGridEntries = new int[randomSelector.spaceSize() - numberOfObserved];
    try {
      int entriesCount = 0;
      while (true) {
        GridEntry next = randomSelector.getNext();
        hashesForUnObservedGridEntries[entriesCount] = next.getHash();
        entriesCount++;

        double[] nextRowToAppend = new double[next.getEntry().size()];
        int colIdx = 0;

        for(Map.Entry<String, Object> entry: next.getEntry().entrySet()) {

          nextRowToAppend[colIdx] = (double) entry.getValue();
          colIdx++;
        }
        DoubleMatrix newMaterializedGridEntry = new DoubleMatrix(1, nextRowToAppend.length, nextRowToAppend);
        unObservedGridEntries = unObservedGridEntries == null ? newMaterializedGridEntry : DoubleMatrix.concatVertically(unObservedGridEntries, newMaterializedGridEntry);
      }
    } catch (RandomSelector.NoUnexploredGridEntitiesLeft ex) {
      System.out.println("Random selector stopped with NoUnexploredGridEntitiesLeft. Total number of explored grid items: " + ex.exploredCount);
    }
    return new MaterialisedGrid(unObservedGridEntries, hashesForUnObservedGridEntries);
  }

  public static class MaterialisedGrid {
    DoubleMatrix unObservedGridEntries;

    int[] hashesForUnObservedGridEntries;

    public MaterialisedGrid(DoubleMatrix unObservedGridEntries, int[] hashesForUnObservedGridEntries) {
      this.unObservedGridEntries = unObservedGridEntries;
      this.hashesForUnObservedGridEntries = hashesForUnObservedGridEntries;
    }
  }

  void initializePriorWithBatchEvaluation() throws SMBOSearchCompleted {
    try {
      for (int j = 0; j < priorSize; j++) {
        // Select randomly and get evaluations from our objective function
        GridEntry next = _randomSelector.getNext();
        double[] nextRowToAppend = new double[next.getEntry().size() + 1]; // TODO why plus one?
        int colIdx = 0;

        for(Map.Entry<String, Object> entry: next.getEntry().entrySet()) {

          nextRowToAppend[colIdx] = (double) entry.getValue();
          colIdx++;
        }
        // Filling last element with evaluated result
        nextRowToAppend[colIdx] = evaluateWithObjectiveFunction(next).evaluatedRes;
        DoubleMatrix newObservedGridEntry = new DoubleMatrix(1, nextRowToAppend.length, nextRowToAppend);

        // TODO we are doing it inside updatePrior as well but here we don't want to create prior covariance matrix with `sigma` and `ell`
        //  wich are not based on grid search (based on prior evaluations) - so we just combine evaluated rows
        _observedGridEntries = _observedGridEntries.rows == 0 ? newObservedGridEntry : DoubleMatrix.concatVertically(_observedGridEntries, newObservedGridEntry);

      }
    } catch (RandomSelector.NoUnexploredGridEntitiesLeft ex) {
      throw new SMBOSearchCompleted(); // suggest user to set smaller prior or not to use SMBO
    }
  }

  // This approach seems not to be helpful at all. Probably sampling diverse entries based on genotypic distance just wastes attempts and it does not help with estimating variance in hyperspace
  void initializeDiversePriorOfSMBOWithBatchEvaluation() throws SMBOSearchCompleted {
    DoubleMatrix baseEntries = null;
    DiverseSelector diverseSelector = new DiverseSelector();
    try {
      for (int j = 0; j < priorSize; j++) {
        if(baseEntries == null) {
          // Select randomly and get evaluations from our objective function
          GridEntry next = _randomSelector.getNext();
          double[] nextRowToAppend = new double[next.getEntry().size() + 1]; // TODO why plus one?
          int colIdx = 0;

          for (Map.Entry<String, Object> entry : next.getEntry().entrySet()) {

            nextRowToAppend[colIdx] = (double) entry.getValue();
            colIdx++;
          }
          DoubleMatrix nextAsMtx = next.getEntryAsMtx();
          baseEntries = baseEntries == null ? nextAsMtx : DoubleMatrix.concatVertically(baseEntries, nextAsMtx);
          // Filling last element with evaluated result
          nextRowToAppend[colIdx] = evaluateWithObjectiveFunction(next).evaluatedRes;
          DoubleMatrix newObservedGridEntry = new DoubleMatrix(1, nextRowToAppend.length, nextRowToAppend);

          // TODO we are doing it inside updatePrior as well but here we don't want to create prior covariance matrix with `sigma` and `ell`
          //  wich are not based on grid search (based on prior evaluations) - so we just combine evaluated rows
          _observedGridEntries = _observedGridEntries.rows == 0 ? newObservedGridEntry : DoubleMatrix.concatVertically(_observedGridEntries, newObservedGridEntry);
        } else {
          // Select diverse entry // TODO refactor, as we materialise on every iteration for prior
          MaterialisedGrid virtuallyMaterialisedGrid = materializeGrid(_randomSelector.cloneTyped(), _observedGridEntries.rows);//we should materialise dedicated separeate grid, otherwise space is explored
          DoubleMatrix unObservedGridEntries = virtuallyMaterialisedGrid.unObservedGridEntries;
          DiverseSelector.MostDistantEntry nextMostDistant = diverseSelector.manyToManySelectMostDistant(baseEntries, unObservedGridEntries);
          baseEntries = DoubleMatrix.concatVertically(baseEntries, nextMostDistant.entry);
          DoubleMatrix nextMostDistantEvaluated = DoubleMatrix.concatHorizontally(nextMostDistant.entry, evaluateRowsWithOF(nextMostDistant.entry));
          int indexForMostDistantUnobserved = nextMostDistant.index;
          assert unObservedGridEntries.rows == virtuallyMaterialisedGrid.hashesForUnObservedGridEntries.length;

          int hashForMostDistant = virtuallyMaterialisedGrid.hashesForUnObservedGridEntries[indexForMostDistantUnobserved];
          _randomSelector.markAsVisitedByValue(hashForMostDistant);
          _observedGridEntries = _observedGridEntries.rows == 0 ? nextMostDistantEvaluated : DoubleMatrix.concatVertically(_observedGridEntries, nextMostDistantEvaluated);
        }
      }
    } catch (RandomSelector.NoUnexploredGridEntitiesLeft ex) {
      throw new SMBOSearchCompleted(); // suggest user to set smaller prior or not to use SMBO
    }
  }

  int selectBestIndex(DoubleMatrix afEvaluations) {
    int indexOfTheBiggestMeanImprovement = afEvaluations.argmax();
    if(!(indexOfTheBiggestMeanImprovement >= 0 && indexOfTheBiggestMeanImprovement < afEvaluations.rows)) {
      throw new IllegalStateException("Index for best suggestion from af evaluations is out of range");
    }
    return indexOfTheBiggestMeanImprovement;
//    return means.get(indexOfTheBiggestMeanImprovement);
  }

  // Helper method that will evaluate multiple rows with OF
  public DoubleMatrix evaluateRowsWithOF(DoubleMatrix unObservedGridEntries) {
    DoubleMatrix YValDM = null;
    for(DoubleMatrix row :unObservedGridEntries.rowsAsList()) {
      EvaluatedGridEntry evaluatedGridEntry = this.objectiveFunction(row);
      DoubleMatrix evaluationDM = evaluatedGridEntry.getEvaluatedEntryAsMtx().getColumn(row.columns);
      if(YValDM == null) {
        YValDM = evaluationDM;
      } else {
        YValDM = DoubleMatrix.concatVertically(YValDM, evaluationDM);
      }
    }
    return YValDM;
  }

  @Override
  public GPSurrogateModel surrogateModel() {
    return _surrogateModel;
  }

  @Override
  public AcquisitionFunction acquisitionFunction(){
//    try {
//      return _acquisitionFunction.cloneTyped();
//    } catch (CloneNotSupportedException ex) {
//      throw new IllegalStateException("Cloning of AF failed");
//    }
    return _acquisitionFunction;
  }
}
