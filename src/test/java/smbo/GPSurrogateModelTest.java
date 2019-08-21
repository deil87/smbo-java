package smbo;

import org.jblas.DoubleMatrix;
import org.junit.Test;
import utils.DoubleMatrixUtils;

import static org.junit.Assert.*;

public class GPSurrogateModelTest extends DoubleMatrixUtils {

  @Test
  public void getCovarianceMtxWithGaussianKernel() {
    DoubleMatrix mtx1 = new DoubleMatrix(1,5,new double[]{1, 2, 3, 4, 5});
    DoubleMatrix mtx2 = new DoubleMatrix(1,5,new double[]{1, 2, 3, 4, 5});
    DoubleMatrix covarianceMtxWithGaussianKernel = new GPSurrogateModel(1,1).getCovarianceMtxWithGaussianKernel(1, 1, mtx1, mtx2);
    multilinePrint(covarianceMtxWithGaussianKernel);
  }

  @Test
  public void posteriorMeanAndVariance() {
    double sigma = 0.1; // gamma =  1 / (2* sigma^2)  ie o.1 sigma -> gamma = 50
    double ell = 1;
    GPSurrogateModel gpSurrogateModel = new GPSurrogateModel(sigma, ell);
    DoubleMatrix observed = new DoubleMatrix(1, 3, 1, 2, 3);
    DoubleMatrix observedMeans = new DoubleMatrix(3, 1, 42, 43, 44);

    DoubleMatrix prior = gpSurrogateModel.getCovarianceMtxWithGaussianKernel(sigma, ell, observed, observed);

    // When we increase value X for new observation we move closer to N(0, 1) value.
    DoubleMatrix newObservation = new DoubleMatrix(1, 1, 3.00001);
    GPSurrogateModel.MeanVariance meanVariance = gpSurrogateModel.posteriorMeanAndVariance(sigma, ell, prior, observed, newObservation, observedMeans);
    double mean = meanVariance.getMean().get(0, 0);
    double variance = meanVariance.getVariance().get(0, 0);
    multilinePrint("Mean:", meanVariance.getMean());
    multilinePrint("Variance:", meanVariance.getVariance());
    assertTrue(mean < 44 && mean > 43);
    assertTrue(variance < 0.01 && variance > 0);

    // Let's increase X
    DoubleMatrix newObservation2 = new DoubleMatrix(1, 1, 16);
    GPSurrogateModel.MeanVariance meanVariance2 = gpSurrogateModel.posteriorMeanAndVariance(sigma, ell, prior, observed, newObservation2, observedMeans);
    double mean2 = meanVariance2.getMean().get(0, 0);
    double variance2 = meanVariance2.getVariance().get(0, 0);
    multilinePrint("Mean:", meanVariance2.getMean());
    multilinePrint("Variance:",meanVariance2.getVariance());
    assertEquals(0, mean2 , 1e-5);
    assertEquals(1, variance2 , 1e-5);
  }

  @Test
  public void evaluate() {
    double sigma = 0.6;
    double ell = 2.0;
    GPSurrogateModel gpSurrogateModel = new GPSurrogateModel(sigma, ell);
    DoubleMatrix observed = new DoubleMatrix(1, 3, 1, 2, 3);
    DoubleMatrix unobserved = new DoubleMatrix(1, 1, 3.1);

    DoubleMatrix observedMeans = new DoubleMatrix(3, 1, 42, 43, 44);

    DoubleMatrix observedWithMeans = DoubleMatrix.concatHorizontally(observed.transpose(), observedMeans);
    GPSurrogateModel.MeanVariance meanVarianceFromEvaluate = gpSurrogateModel.evaluate(observedWithMeans, unobserved);

    // Let's compare it with directly computed by `posteriorMeanAndVariance`
    DoubleMatrix prior = gpSurrogateModel.getCovarianceMtxWithGaussianKernel(sigma, ell, observed, observed);
    GPSurrogateModel.MeanVariance expected = gpSurrogateModel.posteriorMeanAndVariance(sigma, ell, prior, observed, unobserved, observedMeans);

    assertEquals(expected.getMean(), meanVarianceFromEvaluate.getMean());
    assertEquals(expected.getVariance(), meanVarianceFromEvaluate.getVariance());
  }

  @Test
  public void combineMatrices() {
    DoubleMatrix k0 = new DoubleMatrix(3, 3, 1, 2, 3, 4, 5, 6, 7, 8, 9);
    DoubleMatrix k1 = new DoubleMatrix(3, 1, 10,11,12);
    DoubleMatrix k2 = new DoubleMatrix(1, 3, 13,14,15);
    DoubleMatrix k3 = new DoubleMatrix(1, 1, 16);
    DoubleMatrix matrix = new GPSurrogateModel().combineKMatrices(k0, k1, k2, k3);
    multilinePrint(matrix);

    DoubleMatrix expected = new DoubleMatrix(4, 4, 1, 2, 3, 13, 4, 5, 6, 14, 7, 8, 9, 15, 10,11,12,16);
    assertEquals(expected, matrix);
  }

  @Test
  public void gridSearchOverGPsHyperparameters() {

    GPSurrogateModel gpSurrogateModel = new GPSurrogateModel();
    DoubleMatrix observed = new DoubleMatrix(1, 5, 1,2,3,4,5);

    DoubleMatrix observedMeans = new DoubleMatrix(5, 1, 1,1.2,1.1,0.9,0.95);
    double[] hps = new GPSurrogateModel().gridSearchOverGPsHyperparameters(observed, observedMeans);

    System.out.println("Sigma:" + hps[0]);
    System.out.println("Ell:" + hps[1]);

  }
}