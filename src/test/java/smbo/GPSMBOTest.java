package smbo;

import org.jblas.DoubleMatrix;
import org.junit.Test;
import utils.TestUtils;

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
    GPSMBO gpsmbo = new GPSMBO(grid, true, 1234);

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
    GPSMBO gpsmbo = new GPSMBO(grid, true, 1234);

    gpsmbo.initializePriorOfSMBOWIthBatchEvaluation();


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
    GPSMBO gpsmbo = new GPSMBO(grid, true, 1234);

    gpsmbo.initializePriorOfSMBOWIthBatchEvaluation(); // This will take 10 entries for prior
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
    GPSMBO gpsmbo = new GPSMBO(grid, true, 1234);
    DoubleMatrix afAvaluations = new DoubleMatrix(5,1, 1,3,7,2,5);
    DoubleMatrix means = new DoubleMatrix(5,1, 1,2,3,4,5);
    assertEquals(3, gpsmbo.selectBest(means, afAvaluations), 1e-5);
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
    GPSMBO gpsmbo = new GPSMBO(grid, true, 1234);
    gpsmbo.initializePriorOfSMBOWIthBatchEvaluation();
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
  public void getNextBestHyperparameters() throws SMBO.SMBOSearchCompleted {
    int size = 5;
    Double[] gridEntries = new Double[size*10];
    int i;
    for (i = 0; i < size*10; i++) {
      gridEntries[i] = (double) i / 10;
    }

    HashMap<String, Object[]> grid = new HashMap<>();
    grid.put("X", gridEntries);
    GPSMBO gpsmbo = new GPSMBO(grid, true, 1234);

    DoubleMatrix nextBestCandidate = gpsmbo.getNextBestCandidateForEvaluation();
    double prediction = nextBestCandidate.get(0, 0);
    assertTrue(prediction <=5 && prediction >= -5);

  }

  @Test
  public void getNextBestHyperparameters_ALL() throws SMBO.SMBOSearchCompleted {
    int size = 5;
    Double[] gridEntries = new Double[size*10];
    int i;
    for (i = 0; i < size*10; i++) {
      gridEntries[i] = (double) i / 10;
    }

    HashMap<String, Object[]> grid = new HashMap<>();
    grid.put("X", gridEntries);
    GPSMBO gpsmbo = new GPSMBO(grid, true, 1234);

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
}