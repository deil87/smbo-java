package smbo;

import org.jblas.DoubleMatrix;
import org.jblas.ranges.IntervalRange;
import org.junit.Test;
import smbo.of.ObjectiveFunction;
import smbo.of.SinOFDefault;
import smbo.of.SqrtOF;

import java.io.IOException;
import java.util.HashMap;

import static smbo.GPSMBO.evaluateRowsWithOF;

public class GPSMBO_E2E_Test {


  @Test
  public void learn_sinOF_with_Max_AF() throws SMBO.SMBOSearchCompleted, IOException {
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
  }

  @Test
  public void learn_sinOF_with_EI_acquisition() throws SMBO.SMBOSearchCompleted, IOException {

    double tradeoff = -5;
    EI ei = new EI(tradeoff, true);

    int size = 25;
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
  }

  @Test
  public void learn_sqrtOF_with_EI_acquisition() throws SMBO.SMBOSearchCompleted, IOException {

    double tradeoff = -5;
    EI ei = new EI(tradeoff, true);

    int size = 3;
    Double[] gridEntries = new Double[size*10];
    int i;
    for (i = 0; i < size*10; i++) {
      gridEntries[i] = (double) i / 10;
    }

    HashMap<String, Object[]> grid = new HashMap<>();
    grid.put("X1", gridEntries);

    int seed = 12345;

    ObjectiveFunction sqrtOF = new SqrtOF();

    GPSMBO gpsmboForTrueOF = new GPSMBO(sqrtOF, grid, ei, true, seed);
    gpsmboForTrueOF.materializeGrid();
    DoubleMatrix unObservedGridEntries = gpsmboForTrueOF.getUnObservedGridEntries();

    // Evaluate whole grid to draw original OF
    DoubleMatrix YValDM = evaluateRowsWithOF(gpsmboForTrueOF, unObservedGridEntries);

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
        DoubleMatrix observedSuggestion = evaluateRowsWithOF(gpsmboForSuggestions, nextBestCandidate);

        DoubleMatrix newKnowledge = DoubleMatrix.concatHorizontally(nextBestCandidate, observedSuggestion);
        System.out.println("New observed point:");
        newKnowledge.print();

        gpsmboForSuggestions.updatePrior(newKnowledge);
      }
    } catch (SMBO.SMBOSearchCompleted ex) {

    }
  }

}