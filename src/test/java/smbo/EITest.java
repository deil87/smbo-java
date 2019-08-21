package smbo;

import org.jblas.DoubleMatrix;
import org.junit.Test;
import utils.DoubleMatrixUtils;

import static org.junit.Assert.*;

public class EITest {

  @Test
  public void computeMTerm() {

    // Case 1 , theBiggerTheBetter = true
    EI ei = new EI(0.5, true);

    DoubleMatrix means = new DoubleMatrix(5,1, 1,4,9,16,25);
    DoubleMatrix mTerm = ei.computeMTerm(means);

    assertEquals(means.add(0.5), mTerm);

    // Case 2 , theBiggerTheBetter = false
    EI ei2 = new EI(0.5, false);
    DoubleMatrix mTerm2 = ei2.computeMTerm(means);

    assertEquals(means.neg().sub(0.5), mTerm2);
  }

  @Test
  public void computePDF() {

    // Case 1 , theBiggerTheBetter = true
    EI ei = new EI(0.5, true);

    DoubleMatrix means = new DoubleMatrix(5,1, 0, 1,4,9,16);
    DoubleMatrix pdf = ei.computePDF(means);

    DoubleMatrixUtils.multilinePrint("PDF", pdf);

    // For  ~ N( 0, 1 )
    assertEquals(0.398942, pdf.get(0,0), 1e-5);
  }

  @Test
  public void computeCDF() {

    // Case 1 , theBiggerTheBetter = true
    EI ei = new EI(0.5, true);

    DoubleMatrix means = new DoubleMatrix(5,1, 0, 1,4,9,16);
    DoubleMatrix cdf = ei.computeCDF(means);

    DoubleMatrixUtils.multilinePrint("CDF", cdf);
    // For  ~ N( 0, 1 ) CDF for mean value should return half of the area of a bell-curve i.e. 0.5
    assertEquals(0.5, cdf.get(0,0), 1e-5);
  }

  // tradeoff = 0.5
  @Test
  public void compute() {

    // Case 1 , theBiggerTheBetter = true
    EI ei = new EI(0.5, true);

    DoubleMatrix means = new DoubleMatrix(5,1, 0, 0,0,0,0);
    DoubleMatrix variances = new DoubleMatrix(5,1, 0.85, 0.1,0.9,0.2,1);
    DoubleMatrix af = ei.compute(means, variances);

    // It is expected for the same means we should get higher `af` where we have higher variance
    double expectedToBeTheHighest = af.get(4, 0);

    for( DoubleMatrix row :af.rowsAsList() ) {
      assertTrue(expectedToBeTheHighest >= row.get(0,0));
    }

    DoubleMatrixUtils.multilinePrint("AF", af);
  }

  // Given same variance we are selecting highest means
  @Test
  public void compute2() {

    // Case 1 , theBiggerTheBetter = true
    EI ei = new EI(0.5, true);

    DoubleMatrix means = new DoubleMatrix(5,1, 0, 0.5,0,1,0);
    DoubleMatrix variances = new DoubleMatrix(5,1, 0.5, 0.5,0.5,0.5,0.5);
    DoubleMatrix af = ei.compute(means, variances);

    // It is expected for the same means we should get higher `af` where we have higher variance
    int indexOfHighestMean = 3;
    double expectedToBeTheHighest = af.get(indexOfHighestMean, 0);

    for( DoubleMatrix row :af.rowsAsList() ) {
      assertTrue(expectedToBeTheHighest >= row.get(0,0));
    }

    DoubleMatrixUtils.multilinePrint("AF", af);
  }

  //
  @Test
  public void compute_tradeoff() {

    // Case 1 , theBiggerTheBetter = true
    double tradeoff = 1.0;
    EI ei = new EI(tradeoff, true);

    DoubleMatrix means = new DoubleMatrix(5,1, 0, 1,0,0.5,0);
    DoubleMatrix variances = new DoubleMatrix(5,1, 0.5, 1,0.5,0.0,0.5);
    DoubleMatrix af = ei.compute(means, variances);

    // It is expected for the same means we should get higher `af` where we have higher variance
    int indexOfLowestVariance = 3;
    double expectedValueOfAF = af.get(indexOfLowestVariance, 0);
//    assertEquals(tradeoff + 0.5, expectedValueOfAF, 1e-5);
//
//    int indexOfHighestVariance = 1;
//
//    double expectedHighest = af.get(indexOfHighestVariance, 0);
//
//    for( DoubleMatrix row :af.rowsAsList() ) {
//      assertTrue(expectedHighest >= row.get(0,0));
//    }

    DoubleMatrixUtils.multilinePrint("AF", af);
  }

  @Test
  public void compute_tradeoff2() {

    // Case 1 looks like when tradeoff is big then it makes it a hard threshold and the bigger/further mean from incumbent the higher af score
    // The smallest tradeoff (but >0) the easier it is for variance to win against mean
    double tradeoff = -1.0;
    EI ei = new EI(tradeoff, true);

    DoubleMatrix means = new DoubleMatrix(5,1, 0, 1,0,0.9,0);
    DoubleMatrix variances = new DoubleMatrix(5,1, 0.1, 0.5,0.1,1,0.1);
    DoubleMatrix af = ei.compute(means, variances);

    // It is expected for the same means we should get higher `af` where we have higher variance
    int indexOfHighestVariance = 3;
    double expectedValueOfAF = af.get(indexOfHighestVariance, 0);

    DoubleMatrixUtils.multilinePrint("AF", af);

//    for( DoubleMatrix row :af.rowsAsList() ) {
//      assertTrue(expectedValueOfAF >= row.get(0,0));
//    }

  }
}