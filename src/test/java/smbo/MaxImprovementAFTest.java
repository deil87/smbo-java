package smbo;

import org.jblas.DoubleMatrix;
import org.junit.Test;

import static org.junit.Assert.*;

public class MaxImprovementAFTest {

  @Test
  public void compute() {
    MaxImprovementAF maxImprovementAF = new MaxImprovementAF(true);

    DoubleMatrix means = new DoubleMatrix(new double[]{1,7,3,9} );

    //Not used actually
    DoubleMatrix variances = new DoubleMatrix(4,4, new double[]{1,0,0,0,    0,2,0,0,   0,0,3,0,   0,0,0,4} );

    maxImprovementAF.setIncumbent(0.5);

    DoubleMatrix computedMaxImprovements = maxImprovementAF.compute(means, variances);

    int indexOfTheBiggestMeanImprovement = computedMaxImprovements.argmax();
    double bestNextValue = means.get(indexOfTheBiggestMeanImprovement);

    assertEquals(9, bestNextValue, 1e-5);


  }

}