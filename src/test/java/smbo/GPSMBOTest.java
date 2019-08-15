package smbo;

import org.jblas.DoubleMatrix;
import org.jblas.ranges.IntervalRange;
import org.junit.Test;
import org.knowm.xchart.*;
import org.knowm.xchart.style.markers.SeriesMarkers;
import smbo.of.ObjectiveFunction;
import smbo.of.SinOFDefault;
import utils.TestUtils;

import java.io.IOException;
import java.util.HashMap;

import static org.junit.Assert.*;

public class GPSMBOTest {


  @Test
  public void updatePrior() {

    // Use case: ( X  Y  )
    int size = 5;
    Double[] gridEntries = new Double[size*10];
    int i;
    for (i = 0; i < size*10; i++) {
      gridEntries[i] = (double) i / 10;
    }

    HashMap<String, Object[]> grid = new HashMap<>();
    grid.put("X", gridEntries);

    ObjectiveFunction sinOF = new SinOFDefault();
    GPSMBO gpsmbo = new GPSMBO(sinOF, grid, true, 1234);

    DoubleMatrix newObservation = new DoubleMatrix(new double[] {5, 42}).transpose();

    gpsmbo.updatePrior(newObservation);

    assertEquals(5, gpsmbo.getObservedGridEntries().get(0,0), 1e-5);
    assertEquals(42, gpsmbo.getObservedGridEntries().get(0,1), 1e-5);
    TestUtils.multilinePrint(gpsmbo.getObservedGridEntries());

  }

  @Test
  public void initializePriorOfSMBOWIthBatchEvaluation() throws SMBO.SMBOSearchCompleted {

    // Use case: ( X  Y  )
    int size = 5;
    Double[] gridEntries = new Double[size*10];
    int i;
    for (i = 0; i < size*10; i++) {
      gridEntries[i] = (double) i / 10;
    }

    HashMap<String, Object[]> grid = new HashMap<>();
    grid.put("X", gridEntries);

    ObjectiveFunction sinOF = new SinOFDefault();

    GPSMBO gpsmbo = new GPSMBO(sinOF, grid, true, 1234);

    gpsmbo.initializePriorOfSMBOWithBatchEvaluation();


    TestUtils.multilinePrint(gpsmbo.getObservedGridEntries());
    assertEquals(10, gpsmbo.getObservedGridEntries().rows, 1e-5);
  }

  @Test
  public void materializeGrid() throws SMBO.SMBOSearchCompleted {

    // Use case: ( X  Y  )
    int size = 15;
    Double[] gridEntries = new Double[size*10];
    int i;
    for (i = 0; i < size*10; i++) {
      gridEntries[i] = (double) i / 10;
    }

    HashMap<String, Object[]> grid = new HashMap<>();
    grid.put("X", gridEntries);

    ObjectiveFunction sinOF = new SinOFDefault();

    GPSMBO gpsmbo = new GPSMBO(sinOF, grid, true, 1234);

    gpsmbo.initializePriorOfSMBOWithBatchEvaluation(); // This will take 10 entries for prior
    gpsmbo.materializeGrid(); // Rest 150 - 10 will be materialized

    TestUtils.multilinePrint(gpsmbo.getUnObservedGridEntries());
    assertEquals(140, gpsmbo.getUnObservedGridEntries().rows, 1e-5);
  }

  // Based on MaxImprovement acquisition and theBiggerTheBetter = true
  @Test
  public void selectBest() {
    int size = 15;
    Double[] gridEntries = new Double[size*10];
    int i;
    for (i = 0; i < size*10; i++) {
      gridEntries[i] = (double) i / 10;
    }

    HashMap<String, Object[]> grid = new HashMap<>();
    grid.put("X", gridEntries);

    ObjectiveFunction sinOF = new SinOFDefault();

    GPSMBO gpsmbo = new GPSMBO(sinOF, grid, true, 1234);
    DoubleMatrix afAvaluations = new DoubleMatrix(5,1, 1,3,7,2,5);
    DoubleMatrix means = new DoubleMatrix(5,1, 1,2,3,4,5);
    assertEquals(3, gpsmbo.selectBest( afAvaluations), 1e-5);
  }

  @Test
  public void dropSuggestionFromUnObservedGridEntries() throws SMBO.SMBOSearchCompleted {
    int size = 2;
    Double[] gridEntries = new Double[size*10];
    int i;
    for (i = 0; i < size*10; i++) {
      gridEntries[i] = (double) i / 10;
    }

    HashMap<String, Object[]> grid = new HashMap<>();
    grid.put("X", gridEntries);

    ObjectiveFunction sinOF = new SinOFDefault();

    GPSMBO gpsmbo = new GPSMBO(sinOF, grid, true, 1234);
    gpsmbo.initializePriorOfSMBOWithBatchEvaluation();
    gpsmbo.materializeGrid();

    DoubleMatrix unObservedGridEntries = gpsmbo.getUnObservedGridEntries();

    int bestIndex = 3;
    double rowWithBestSuggestion = unObservedGridEntries.getRow(bestIndex).get(0,0);
    gpsmbo.dropSuggestionFromUnObservedGridEntries(bestIndex);

    TestUtils.multilinePrint(gpsmbo.getUnObservedGridEntries());

    double[] unobservedValues = gpsmbo.getUnObservedGridEntries().toArray();
    for(double unobserved : unobservedValues) {
      assertTrue(rowWithBestSuggestion != unobserved);
    }

    assertEquals(9, gpsmbo.getUnObservedGridEntries().length);
  }

  @Test
  public void selectBestBasedOnResponse() throws SMBO.SMBOSearchCompleted {
    int size = 2;
    Double[] gridEntries = new Double[size*10];
    int i;
    for (i = 0; i < size*10; i++) {
      gridEntries[i] = (double) i / 10;
    }

    HashMap<String, Object[]> grid = new HashMap<>();
    grid.put("X", gridEntries);

    ObjectiveFunction sinOF = new SinOFDefault();

    GPSMBO gpsmbo = new GPSMBO(sinOF, grid, true, 1234);

    DoubleMatrix observed = new DoubleMatrix(5, 2, 1,2,3,4,5, 8,7,9,19,5);
    DoubleMatrix best = gpsmbo.selectBestBasedOnResponse(observed);

    TestUtils.multilinePrint("Best row", best);

    assertEquals(new DoubleMatrix(1,2,4, 19), best);
  }


  @Test
  public void getNextBestHyperparameters() throws SMBO.SMBOSearchCompleted, IOException{
    int size = 5;
    Double[] gridEntries = new Double[size*10];
    int i;
    for (i = 0; i < size*10; i++) {
      gridEntries[i] = (double) i / 10;
    }

    HashMap<String, Object[]> grid = new HashMap<>();
    grid.put("X", gridEntries);

    ObjectiveFunction sinOF = new SinOFDefault();

    GPSMBO gpsmbo = new GPSMBO(sinOF, grid, true, 1234);

    DoubleMatrix nextBestCandidate = gpsmbo.getNextBestCandidateForEvaluation();
    double prediction = nextBestCandidate.get(0, 0);
    assertTrue(prediction <=5 && prediction >= -5);

  }

  @Test
  public void getNextBestHyperparameters_ALL() throws SMBO.SMBOSearchCompleted, IOException {
    int size = 5;
    Double[] gridEntries = new Double[size*10];
    int i;
    for (i = 0; i < size*10; i++) {
      gridEntries[i] = (double) i / 10;
    }

    HashMap<String, Object[]> grid = new HashMap<>();
    grid.put("X", gridEntries);

    ObjectiveFunction sinOF = new SinOFDefault();

    GPSMBO gpsmbo = new GPSMBO(sinOF, grid, true, 1234);

    double[] suggestions = new double[size*10 - 10]; // prior is 10
    try{
      int suggestionIdx = 0;
      while (true) {
        DoubleMatrix nextBestCandidate = gpsmbo.getNextBestCandidateForEvaluation();
        double candidate = nextBestCandidate.get(0, 0);
        suggestions[suggestionIdx] = candidate;
        suggestionIdx++;
      }
    } catch (SMBO.SMBOSearchCompleted ex) {

    }

    TestUtils.multilinePrint("Prior:", gpsmbo.getObservedGridEntries());

    for(int si = 0; si < suggestions.length ; si++) {
      System.out.println(si + ": " + suggestions[si]);
    }
    assertEquals(40, suggestions);

  }

  @Test
  public void getNextBestHyperparameters_ALL_with_visualisation() throws SMBO.SMBOSearchCompleted, IOException {
    int size = 25;
    Double[] gridEntries = new Double[size*10];
    int i;
    for (i = 0; i < size*10; i++) {
      gridEntries[i] = (double) i / 10;
    }

    HashMap<String, Object[]> grid = new HashMap<>();
    grid.put("X", gridEntries);

    ObjectiveFunction sinOF = new SinOFDefault();
    GPSMBO gpsmbo = new GPSMBO(sinOF, grid, true, 1234);
    gpsmbo.materializeGrid();
    DoubleMatrix unObservedGridEntries = gpsmbo.getUnObservedGridEntries();

    // Evaluate whole grid to draw original OF
    DoubleMatrix YValDM = evaluateRowsWithOF(gpsmbo, unObservedGridEntries);

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

    XYChart chart = new XYChartBuilder().width(1000).height(500).title("SMBO suggestions").xAxisTitle("X").yAxisTitle("Y").build();
    chart.getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Scatter);
    chart.getStyler().setLegendVisible(false);

    chart.addSeries("Original OF", unObservedGridEntries.toArray(), YValDM.toArray());
    XYSeries series = chart.addSeries("Prior", gpsmboSuggestions.getObservedGridEntries().getColumn(0).toArray(), gpsmboSuggestions.getObservedGridEntries().getColumn(1).toArray());
    series.setMarker(SeriesMarkers.DIAMOND);
    for(int si = 0; si < suggestions.rows ; si++) {
      DoubleMatrix suggestion = suggestions.getRow(si);
      XYSeries series2 = chart.addSeries("Suggestion:" + si, suggestion.toArray(), evaluateRowsWithOF(gpsmboSuggestions, suggestion).sub(0.2).toArray());
      series2.setMarker(SeriesMarkers.PLUS);

      series2.setMarkerColor(new java.awt.Color( Math.max(255 - si* 3, 0),0, 0));
    }


    BitmapEncoder.saveBitmapWithDPI(chart, "./SMBO_GP_suggestions_based_on_max_improvement_300_DPI", BitmapEncoder.BitmapFormat.PNG, 300);

  }

  @Test
  public void getNextBestHyperparameters_ALL_with_visualisation_EI_acquisition() throws SMBO.SMBOSearchCompleted, IOException {

    double tradeoff = 0.0;
    EI ei = new EI(tradeoff, true);

    int size = 10;
    Double[] gridEntries = new Double[size*10];
    int i;
    for (i = 0; i < size*10; i++) {
      gridEntries[i] = (double) i / 10;
    }

    HashMap<String, Object[]> grid = new HashMap<>();
    grid.put("X", gridEntries);

    int seed = 12345;

    ObjectiveFunction sinOF = new SinOFDefault();

    GPSMBO gpsmboForTrueOF = new GPSMBO(sinOF, grid, ei, true, seed);
    gpsmboForTrueOF.materializeGrid();
    DoubleMatrix unObservedGridEntries = gpsmboForTrueOF.getUnObservedGridEntries();

    // Evaluate whole grid to draw original OF
    DoubleMatrix YValDM = evaluateRowsWithOF(gpsmboForTrueOF, unObservedGridEntries);

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
        DoubleMatrix observedSuggestion = evaluateRowsWithOF(gpsmboForSuggestions, nextBestCandidate);
        gpsmboForSuggestions.updatePrior(DoubleMatrix.concatHorizontally(nextBestCandidate, observedSuggestion));
      }
    } catch (SMBO.SMBOSearchCompleted ex) {

    }

    XYChart chart = new XYChartBuilder().width(1000).height(500).title("SMBO suggestions").xAxisTitle("X").yAxisTitle("Y").build();
    chart.getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Scatter);
    chart.getStyler().setLegendVisible(false);

    chart.addSeries("Original OF", unObservedGridEntries.toArray(), YValDM.toArray());
    XYSeries series = chart.addSeries("Prior", onlyPriorEvaluation.getColumn(0).toArray(), onlyPriorEvaluation.getColumn(1).toArray());
    series.setMarker(SeriesMarkers.DIAMOND);
    for(int si = 0; si < suggestions.rows ; si++) {
      DoubleMatrix suggestion = suggestions.getRow(si);
      XYSeries series2 = chart.addSeries("Suggestion:" + si, suggestion.toArray(), evaluateRowsWithOF(gpsmboForSuggestions, suggestion).sub(0.2).toArray());
      series2.setMarker(SeriesMarkers.PLUS);

      series2.setMarkerColor(new java.awt.Color( Math.max(255 - si* 3, 0),0, 0));
    }


    BitmapEncoder.saveBitmapWithDPI(chart, "./SMBO_GP_suggestions_based_on_max_improvement_300_DPI", BitmapEncoder.BitmapFormat.PNG, 300);

  }

  private DoubleMatrix evaluateRowsWithOF(GPSMBO gpsmbo, DoubleMatrix unObservedGridEntries) {
    DoubleMatrix YValDM = null;
    for(DoubleMatrix row :unObservedGridEntries.rowsAsList()) {
      DoubleMatrix evaluationDM = gpsmbo.objectiveFunction(row);
      if(YValDM == null) {
        YValDM = evaluationDM;
      } else {
        YValDM = DoubleMatrix.concatVertically(YValDM, evaluationDM);
      }
    }
    return YValDM;
  }
}