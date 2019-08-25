package benchmark;

import org.jblas.DoubleMatrix;
import org.jblas.ranges.IntervalRange;
import org.junit.Ignore;
import org.junit.Test;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.internal.chartpart.Chart;
import smbo.EI;
import smbo.GPSMBO;
import smbo.SMBO;
import smbo.of.*;

import java.io.IOException;
import java.util.*;

@Ignore
public class GPSMBO_Basic_Benchmark {


  @Test
  public void learn_sinOF_with_Max_AF() throws SMBO.SMBOSearchCompleted, IOException {
    int size = 25;
    Double[] gridEntries = new Double[size*10];
    int i;
    for (i = 0; i < size*10; i++) {
      gridEntries[i] = (double) i / 10;
    }

    SortedMap<String, Object[]> grid = Collections.synchronizedSortedMap(new TreeMap());
    grid.put("X", gridEntries);

    ObjectiveFunction sinOF = new SinOFDefault();

    GPSMBO gpsmboSuggestions = new GPSMBO(sinOF, grid, true, 1234);
    DoubleMatrix suggestions = null;
    try{
      while (true) {
        DoubleMatrix nextBestCandidate = gpsmboSuggestions.getNextBestCandidateForEvaluation();
        if(suggestions == null) {
          suggestions = nextBestCandidate;
        } else {
          suggestions = DoubleMatrix.concatVertically(suggestions, nextBestCandidate);
        }
      }
    } catch (SMBO.SMBOSearchCompleted ex) {

    }
  }

  @Test
  public void learn_sinOF_with_EI_acquisition() throws IOException {

    // Trade-off between exploration and exploitation
    double tradeoff = 0.0;
    EI acquisitionFun = new EI(tradeoff, true);

    int size = 10;
    Double[] gridEntriesForX = new Double[size*10];
    for (int i = 0; i < size*10; i++) {
      gridEntriesForX[i] = (double) i / 10;
    }

    SortedMap<String, Object[]> grid = Collections.synchronizedSortedMap(new TreeMap());
    grid.put("X", gridEntriesForX);

    int seed = 12345;

    ObjectiveFunction sinOF = new SinOFDefault();

    GPSMBO gpsmbo = new GPSMBO(sinOF, grid, acquisitionFun,true, seed);

    try{
      while (true) {
        DoubleMatrix nextBestCandidate = gpsmbo.getNextBestCandidateForEvaluation();

        DoubleMatrix observedOFValue = gpsmbo.evaluateRowsWithOF( nextBestCandidate);
        DoubleMatrix observedSuggestion = DoubleMatrix.concatHorizontally(nextBestCandidate, observedOFValue);
        gpsmbo.updatePrior(observedSuggestion);
      }
    } catch (SMBO.SMBOSearchCompleted ex) {
      List<Chart> meanVarianceCharts = gpsmbo.getMeanVarianceCharts();
      BitmapEncoder.saveBitmap(meanVarianceCharts, meanVarianceCharts.size() / 2 , 2, "MeanVariance_sin_" + size + "_" + seed, BitmapEncoder.BitmapFormat.PNG);
    }
  }

  @Test
  public void learn_dumped_sin_wave_OF_with_EI_acquisition() throws SMBO.SMBOSearchCompleted, IOException {

    double tradeoff = 0.0;
    EI ei = new EI(tradeoff, true);

    int size = 5;
    Double[] gridEntries = new Double[size*10];
    int i;
    for (i = 0; i < size*10; i++) {
      gridEntries[i] = (double) i / 10;
    }

    SortedMap<String, Object[]> grid = Collections.synchronizedSortedMap(new TreeMap());
    grid.put("X", gridEntries);

    int seed = 12345;

    ObjectiveFunction sinOF = new ComplexPeriodicOF();

    GPSMBO gpsmboForSuggestions = new GPSMBO(sinOF, grid, ei,true, seed);
    DoubleMatrix suggestions = null;

    DoubleMatrix onlyPriorEvaluation = null;
    try{
      while (true) {
        DoubleMatrix nextBestCandidate = gpsmboForSuggestions.getNextBestCandidateForEvaluation();
        if(onlyPriorEvaluation == null) {
          onlyPriorEvaluation = gpsmboForSuggestions.getObservedGridEntries().getRows(new IntervalRange(0, 5)); // 10 - size of a prior
        }

        if(suggestions == null) {
          suggestions = nextBestCandidate;
        } else {
          suggestions = DoubleMatrix.concatVertically(suggestions, nextBestCandidate);
        }
        DoubleMatrix observedSuggestion = gpsmboForSuggestions.evaluateRowsWithOF( nextBestCandidate);
        gpsmboForSuggestions.updatePrior(DoubleMatrix.concatHorizontally(nextBestCandidate, observedSuggestion));
      }
    } catch (SMBO.SMBOSearchCompleted ex) {
      List<Chart> meanVarianceCharts = gpsmboForSuggestions.getMeanVarianceCharts();
      BitmapEncoder.saveBitmap(meanVarianceCharts, meanVarianceCharts.size() / 2 , 2, "MeanVariance_dumped_sin_wave_" + size + "_" + seed, BitmapEncoder.BitmapFormat.PNG);

    }
  }

  @Test
  public void learn_sqrtOF_with_EI_acquisition() throws SMBO.SMBOSearchCompleted, IOException {

    double tradeoff = 0.0;
    EI ei = new EI(tradeoff, true);

    int size = 3;
    Double[] gridEntries = new Double[size*10];
    int i;
    for (i = 0; i < size*10; i++) {
      gridEntries[i] = (double) i / 10;
    }

    SortedMap<String, Object[]> grid = Collections.synchronizedSortedMap(new TreeMap());
    grid.put("X", gridEntries);

    int seed = 12346;

    ObjectiveFunction sqrtOF = new SqrtOF();

    // Evaluate whole grid to draw original OF

    GPSMBO gpsmboForSuggestions = new GPSMBO(sqrtOF, grid, ei,true, seed);
    DoubleMatrix suggestionsAll = null;

    DoubleMatrix onlyPriorEvaluation = null;
    try{
      while (true) {
        DoubleMatrix nextBestCandidate = gpsmboForSuggestions.getNextBestCandidateForEvaluation(); // this triggers prior evaluation
        if(onlyPriorEvaluation == null) {
          onlyPriorEvaluation = gpsmboForSuggestions.getObservedGridEntries().getRows(new IntervalRange(0, 5));//TODO don't want to do this on every iteration
        }

        if(suggestionsAll == null) {
          suggestionsAll = nextBestCandidate;
        } else {
          suggestionsAll = DoubleMatrix.concatVertically(suggestionsAll, nextBestCandidate);
        }
        DoubleMatrix observedSuggestion = gpsmboForSuggestions.evaluateRowsWithOF(nextBestCandidate);

        DoubleMatrix newKnowledge = DoubleMatrix.concatHorizontally(nextBestCandidate, observedSuggestion);
        System.out.println("New observed point:");
        newKnowledge.print();

        gpsmboForSuggestions.updatePrior(newKnowledge);
      }
    } catch (SMBO.SMBOSearchCompleted ex) {
      List<Chart> meanVarianceCharts = gpsmboForSuggestions.getMeanVarianceCharts();
      BitmapEncoder.saveBitmap(meanVarianceCharts, meanVarianceCharts.size() , 1, "MeanVariance_" + seed, BitmapEncoder.BitmapFormat.PNG);

    }
  }

  @Test
  public void learn_sinOF_2D_with_EI_acquisition() throws SMBO.SMBOSearchCompleted, IOException {

    double tradeoff = 0.0;
    EI ei = new EI(tradeoff, true);

    int size = 2;
    Double[] gridEntries = new Double[size*10 +1];
    int i;
    for (i = 0; i <= size*10; i++) {
      gridEntries[i] = (double) i / 10;
    }

    SortedMap<String, Object[]> grid = Collections.synchronizedSortedMap(new TreeMap());
    grid.put("X", gridEntries);
    grid.put("Y", gridEntries);

    int seed = 12345;

    ObjectiveFunction sqrtOF = new SinOF_2D();

    GPSMBO gpsmboForSuggestions = new GPSMBO(sqrtOF, grid, ei,true, seed);
    DoubleMatrix suggestionsAll = null;

    DoubleMatrix onlyPriorEvaluation = null;
    try{
      while (true) {
        DoubleMatrix nextBestCandidate = gpsmboForSuggestions.getNextBestCandidateForEvaluation(); // this triggers prior evaluation
        if(onlyPriorEvaluation == null) {
          onlyPriorEvaluation = gpsmboForSuggestions.getObservedGridEntries().getRows(new IntervalRange(0, 5));//TODO don't want to do this on every iteration
        }

        if(suggestionsAll == null) {
          suggestionsAll = nextBestCandidate;
        } else {
          suggestionsAll = DoubleMatrix.concatVertically(suggestionsAll, nextBestCandidate);
        }
        DoubleMatrix observedSuggestion = gpsmboForSuggestions.evaluateRowsWithOF(nextBestCandidate);

        DoubleMatrix newKnowledge = DoubleMatrix.concatHorizontally(nextBestCandidate, observedSuggestion);
        System.out.println("New observed point:");
        newKnowledge.print();

        gpsmboForSuggestions.updatePrior(newKnowledge);
      }
    } catch (SMBO.SMBOSearchCompleted ex) {

    }

    int indexOfSuggestionThatLedToBestOFValue = gpsmboForSuggestions.selectBestIndexBasedOnResponse(gpsmboForSuggestions.getObservedGridEntries());
    System.out.println("Index of suggestion that led to the best OF value: " + indexOfSuggestionThatLedToBestOFValue);
  }

}