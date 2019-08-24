## Sequential Model-based Optimisation

This project represents an implementation of the SMBO concept on top of JBlas linear algebra library.

Usage:

```
    double tradeoff = 0.0;
    EI ei = new EI(tradeoff, true);

    int size = 10;
    Double[] gridEntries = new Double[size*10];
    for (int i = 0; i < size*10; i++) {
      gridEntries[i] = (double) i / 10;
    }

    SortedMap<String, Object[]> grid = Collections.synchronizedSortedMap(new TreeMap());
    grid.put("X", gridEntries);

    int seed = 12345;

    ObjectiveFunction sinOF = new SinOFDefault();

    GPSMBO gpsmboForSuggestions = new GPSMBO(sinOF, grid, ei,true, seed);
    DoubleMatrix suggestions = null;

    DoubleMatrix onlyPriorEvaluation = null;
    try{
      while (true) {
        DoubleMatrix nextBestCandidate = gpsmboForSuggestions.getNextBestCandidateForEvaluation();
        if(onlyPriorEvaluation == null) {
          onlyPriorEvaluation = gpsmboForSuggestions.getObservedGridEntries().getRows(new IntervalRange(0, 5));
        }

        suggestions =  suggestions == null ? nextBestCandidate : DoubleMatrix.concatVertically(suggestions, nextBestCandidate);

        DoubleMatrix observedSuggestion = evaluateRowsWithOF(gpsmboForSuggestions, nextBestCandidate);
        gpsmboForSuggestions.updatePrior(DoubleMatrix.concatHorizontally(nextBestCandidate, observedSuggestion));
      }
    } catch (SMBO.SMBOSearchCompleted ex) {
      List<Chart> meanVarianceCharts = gpsmboForSuggestions.getMeanVarianceCharts();
      BitmapEncoder.saveBitmap(meanVarianceCharts, meanVarianceCharts.size() / 2 , 2, "MeanVariance_sin_" + size + "_" + seed, BitmapEncoder.BitmapFormat.PNG);

    }

```

## Example:
### Sin objective function:

Acquisition function: Expected Improvement

```
public class SinOF extends ObjectiveFunction{

  public EvaluatedGridEntry evaluate(GridEntry entry) {
    Double xVal = (Double)entry.getEntry().get("X");
    double result = Math.sin(xVal * 2.5) * 5 ;
    return new EvaluatedGridEntry(entry, result);
  }
}

```


Grid of charts show predicted means and varianceS on every iteration of the learning process.

![Per iteration predictions for Means and Variances](MeanVariance_sin_10_12345.png?raw=true "Learning sin function")	