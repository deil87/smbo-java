package smbo;

import org.jblas.Decompose;
import org.jblas.DoubleMatrix;
import org.jblas.Geometry;
import org.jblas.Solve;
import org.jblas.ranges.IntervalRange;
import utils.TestUtils;

import static org.jblas.DoubleMatrix.eye;
import static org.jblas.MatrixFunctions.exp;

/**
 * This surrogate model is based on Gaussian multivariate process regression
 */
public class GPSurrogateModel extends SurrogateModel{

  public DoubleMatrix getCovariancePrior() {
    return covariancePrior;
  }

  public void setCovariancePrior(DoubleMatrix covariancePrior) {
    this.covariancePrior = covariancePrior;
  }

  private DoubleMatrix covariancePrior = null;
  
  private double _currentBestSigma = -1;
  private double _currentBestEll = -1;

  double sigma = 0.6;
  double ell = 2.0;

  public GPSurrogateModel(double sigma, double ell) {
    this.sigma = sigma;
    this.ell = ell;
  }
  public GPSurrogateModel() {
  }

  public static class MeanVariance {

    public DoubleMatrix getMean() {
      return mean;
    }

    public DoubleMatrix getVariance() {
      return variance;
    }

    private DoubleMatrix mean;
    private DoubleMatrix variance;
    private DoubleMatrix posteriorCovariance; //posterior -> prior kernel
    public MeanVariance(DoubleMatrix mean, DoubleMatrix variance, DoubleMatrix posteriorCovariance) {
      this.mean = mean;
      this.variance = variance;
      this.posteriorCovariance = posteriorCovariance;
    }
  }

  /**
   * Matrices x1 and x2 are supposed to be a row-vectors as we are computing distances between columns */
  DoubleMatrix getCovarianceMtxWithGaussianKernel(double sigma, double ell, DoubleMatrix x1, DoubleMatrix x2) {
    DoubleMatrix d = Geometry.pairwiseSquaredDistances(x1, x2);
    return exp(d.mul(0.5).div(ell).neg()).mul(sigma * sigma);
  }

  //  K0 + sigma^2*I    K1
  //  K2                K3
  //
  // 
  MeanVariance posteriorMeanAndVariance(double sigma, double ell, DoubleMatrix covariancePrior, DoubleMatrix observedData, DoubleMatrix newObservation, DoubleMatrix priorMeans) {
    int n = observedData.columns;
    assert priorMeans.rows == n;

    DoubleMatrix covarianceBetweenNewAndPrior = getCovarianceMtxWithGaussianKernel(sigma, ell, observedData, newObservation);   // K1
    DoubleMatrix K2 = covarianceBetweenNewAndPrior.transpose();
    DoubleMatrix varianceForNewObservations = getCovarianceMtxWithGaussianKernel(sigma, ell, newObservation, newObservation); // K3

    DoubleMatrix K = combineKMatrices(covariancePrior, covarianceBetweenNewAndPrior, K2, varianceForNewObservations);

    // Step 1: Compute means

    // mt(x) = K(X*, Xt) * [K(Xt , Xt) + σ^2 * I ]^-1 * yt 
    //
    //           ^^                ^^                 ^^
    //           K2                 b                  c (= priorMeans)

    // Note: without multiplying by sigma^2 here I was getting negative variances. Because probably in one part of the overall formula I WAS using sigma but here I did not (just eye(n))
    DoubleMatrix noiseIdentity = eye(n).mul(sigma * sigma);
    DoubleMatrix b = Solve.solve(covariancePrior, noiseIdentity); // solve with Identity B matrix in Ax=B will return us inverse.

    DoubleMatrix ab = K2.mmul(b);
    DoubleMatrix posteriorMeanMatrix = ab.mmul(priorMeans);

    // Step 2: Compute standard deviations/ sigmas
    //
    // Sigma = K(X*, X*) − K(X*, Xt) [ K(Xt , Xt) + σ^2 *I]^−1 * K(Xt , X*)
    //
    //           ^^                ^^      ^^
    // varianceForNewObservation   K2      b                  K1(=covarianceBetweenNewAndPrior) 

    DoubleMatrix varianceBetweenPrior = ab.mmul(covarianceBetweenNewAndPrior);
    DoubleMatrix varianceCovarianceMatrix = varianceForNewObservations.sub(varianceBetweenPrior); //TODO Maybe we need to swap K1 and K2???

    DoubleMatrix variances = varianceCovarianceMatrix.diag();
    return new MeanVariance(posteriorMeanMatrix, variances, K); //TODO not sure that we need to return only diag elements
  }

  DoubleMatrix combineKMatrices(DoubleMatrix covariancePrior, DoubleMatrix covarianceBetweenNewAndPrior, DoubleMatrix k2, DoubleMatrix varianceForNewObservation) {
    DoubleMatrix K_UPPER = DoubleMatrix.concatHorizontally(covariancePrior, covarianceBetweenNewAndPrior);
    DoubleMatrix K_LOWER = DoubleMatrix.concatHorizontally(k2, varianceForNewObservation);
    return DoubleMatrix.concatVertically(K_UPPER, K_LOWER);
  }

  public DoubleMatrix initCovariancePrior(DoubleMatrix observedGridEntries) {
    if(covariancePrior == null)
      covariancePrior = getCovarianceMtxWithGaussianKernel(sigma, ell, observedGridEntries, observedGridEntries);
    else throw new IllegalStateException("Unexpected covariancePrior");
    return covariancePrior;
  }

  public DoubleMatrix updateCovariancePrior(DoubleMatrix observedGridEntries, DoubleMatrix newObservationFromObjectiveFun) {
    assert covariancePrior != null;
    DoubleMatrix K0 = covariancePrior;
    DoubleMatrix K1 = getCovarianceMtxWithGaussianKernel(sigma, ell, observedGridEntries, newObservationFromObjectiveFun);
    DoubleMatrix K2 = K1.transpose();
    DoubleMatrix K3 = getCovarianceMtxWithGaussianKernel(sigma, ell, newObservationFromObjectiveFun, newObservationFromObjectiveFun);
    covariancePrior = combineKMatrices(K0, K1, K2, K3);
    return covariancePrior;
  }

  /**
   *  Goal is to evaluate `unObservedGridEntries` */
  public MeanVariance evaluate(DoubleMatrix observedGridEntriesWithMeans, DoubleMatrix unObservedGridEntries) {

    DoubleMatrix onlyFeatures = observedGridEntriesWithMeans.getColumns(new IntervalRange(0, observedGridEntriesWithMeans.columns - 1)).transpose();
    DoubleMatrix onlyMeans = observedGridEntriesWithMeans.getColumn(observedGridEntriesWithMeans.columns - 1);

    if (covariancePrior == null) {
      covariancePrior = getCovarianceMtxWithGaussianKernel(sigma, ell, onlyFeatures, onlyFeatures);
    }

//    TestUtils.multilinePrint("Covariance prior:", covariancePrior);

    //Once we got prior covariance matrix we can predict for all the other unobserved grid entries
    MeanVariance meanVariance = posteriorMeanAndVariance(sigma, ell, covariancePrior, onlyFeatures, unObservedGridEntries, onlyMeans);

    return meanVariance;
  }
  
  double[] gridSearchOverGPsHyperparameters(DoubleMatrix observedDataDM, DoubleMatrix observedMeansDM) {
    double argMaxSigma = 0.1;
    double argMaxEll = 0.1;
    double mll = Double.NEGATIVE_INFINITY;
    
    for (double sigma = 0.4; sigma < 0.9; sigma+=0.1) {
      for (double ell = 1.5; ell < 3.2; ell+=0.1) {
        
        int n = observedDataDM.length; 

        DoubleMatrix K = getCovarianceMtxWithGaussianKernel(sigma, ell, observedDataDM, observedDataDM);
        
        // r = -1/2*t(y)%*%solve(K)%*%y - 1/2*log(det(K)) - n/2*log(pi*2)
        Decompose.QRDecomposition<DoubleMatrix> qr = Decompose.qr(K);
        double determinant = 1;
        for (double diagElem : qr.r.diag().data) {
          determinant *= diagElem;
        }
        double det_K = Math.abs(determinant);
        DoubleMatrix firstTerm = observedMeansDM.transpose().mul(-0.5).mmul(Solve.solve(K, eye(K.columns))).mmul(observedMeansDM);
        double r = firstTerm.get(0) - 0.5 * Math.log(det_K) - (n/2) * Math.log(Math.PI*2);
        if(r > mll) {
          mll = r;
          argMaxSigma = sigma;
          argMaxEll = ell;
        }
      }
    }
    
    return new double[] {argMaxSigma, argMaxEll};
  }



}
