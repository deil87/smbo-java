package smbo;

import org.jblas.*;
import org.jblas.ranges.IntervalRange;

import static org.jblas.DoubleMatrix.eye;
import static org.jblas.MatrixFunctions.exp;
import static utils.DoubleMatrixUtils.multilinePrint;

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
    DoubleMatrix mainTerm = d.mul(0.5).div(ell).div(sigma * sigma);
    return exp(mainTerm.neg());
  }

  //  K0 + sigma^2*I    K1
  //  K2                K3
  //
  // 
  MeanVariance posteriorMeanAndVariance(double sigma, double ell, DoubleMatrix covariancePrior, DoubleMatrix observedData, DoubleMatrix unobservedEntrie, DoubleMatrix priorMeans) {
    int n = observedData.columns;
    assert priorMeans.rows == n;

    double sigmaBest = sigma;
    double ellBest = ell;
    boolean isGSOverGPHyperparametersEnabled = false;
    if(isGSOverGPHyperparametersEnabled) {
      // but in that way our covariancePrior was computed with different sigma and ell from previous iteration
      double[] bestFoundGPHps = gridSearchOverGPsHyperparameters(observedData, priorMeans);
      sigmaBest = bestFoundGPHps[0];
      ellBest = bestFoundGPHps[1];
    }

    System.out.println("Best sigma:" + sigmaBest + "     Best ell:" + ellBest + ". Number of observed = " + n);

    DoubleMatrix covarianceBetweenNewAndPrior = getCovarianceMtxWithGaussianKernel(sigmaBest, ellBest, observedData, unobservedEntrie);   // K1
    DoubleMatrix K2 = covarianceBetweenNewAndPrior.transpose();
    DoubleMatrix covarianceForNewObservations = getCovarianceMtxWithGaussianKernel(sigmaBest, ellBest, unobservedEntrie, unobservedEntrie); // K3

    DoubleMatrix K = combineKMatrices(covariancePrior, covarianceBetweenNewAndPrior, K2, covarianceForNewObservations);

    // Step 1: Compute means

    // mt(x) = K(X*, Xt) * [K(Xt , Xt) + σ^2 * I ]^-1 * yt 
    //
    //           ^^                ^^                 ^^
    //           K2                 b                  c (= priorMeans)

    // Note: without multiplying by sigma^2 here I was getting negative variances. Because probably in one part of the overall formula I WAS using sigma but here I did not (just eye(n))
    DoubleMatrix identity = eye(n)/*.mul(sigmaBest * sigmaBest)*/;
    DoubleMatrix b = Solve.solve(covariancePrior.add(eye(n).mul(sigmaBest * sigmaBest)), identity); // solve with Identity B matrix in Ax=B will return us inverse.

    DoubleMatrix ab = K2.mmul(b);
    DoubleMatrix posteriorMeanMatrix = ab.mmul(priorMeans);

    // Step 2: Compute standard deviations/ sigmas
    //
    // Sigma = K(X*, X*) − K(X*, Xt) [ K(Xt , Xt) + σ^2 *I]^−1 * K(Xt , X*)
    //
    //           ^^                ^^      ^^
    // varianceForNewObservation   K2      b                  K1(=covarianceBetweenNewAndPrior) 

    DoubleMatrix varianceBetweenPrior = ab.mmul(covarianceBetweenNewAndPrior);
    DoubleMatrix varianceCovarianceMatrixForNewObservation = covarianceForNewObservations.sub(varianceBetweenPrior); //TODO Maybe we need to swap K1 and K2???

    // Note: we are taking abs() as variance in prior could be bigger than variance in our new observations
    //TODO not sure that we need to return only diag elements
    DoubleMatrix variances = MatrixFunctions.abs(varianceCovarianceMatrixForNewObservation.diag());
    return new MeanVariance(posteriorMeanMatrix, variances, K);
  }

  DoubleMatrix combineKMatrices(DoubleMatrix covariancePrior, DoubleMatrix covarianceBetweenNewAndPrior, DoubleMatrix k2, DoubleMatrix varianceForNewObservation) {
    DoubleMatrix K_UPPER = DoubleMatrix.concatHorizontally(covariancePrior, covarianceBetweenNewAndPrior);
    DoubleMatrix K_LOWER = DoubleMatrix.concatHorizontally(k2, varianceForNewObservation);
    return DoubleMatrix.concatVertically(K_UPPER, K_LOWER);
  }

  // TODO not tested
  public DoubleMatrix ifNeededInitCovariancePrior(DoubleMatrix observedGridEntries) {
    if(covariancePrior == null)
      covariancePrior = getCovarianceMtxWithGaussianKernel(sigma, ell, observedGridEntries, observedGridEntries);
//    else throw new IllegalStateException("Unexpected covariancePrior");
    return covariancePrior;
  }

  // TODO not tested
  public DoubleMatrix updateCovariancePrior(DoubleMatrix observedGridEntries, DoubleMatrix newObservationFromObjectiveFun) {
    assert covariancePrior != null;
    DoubleMatrix K0 = covariancePrior;

    DoubleMatrix onlyFeaturesFromObserved = observedGridEntries.getColumns(new IntervalRange(0, observedGridEntries.columns - 1)).transpose();
    DoubleMatrix onlyFeaturesFromMewObservation = newObservationFromObjectiveFun.getColumns(new IntervalRange(0, newObservationFromObjectiveFun.columns - 1)).transpose();
    DoubleMatrix K1 = getCovarianceMtxWithGaussianKernel(sigma, ell, onlyFeaturesFromObserved, onlyFeaturesFromMewObservation);
    DoubleMatrix K2 = K1.transpose();
    DoubleMatrix K3 = getCovarianceMtxWithGaussianKernel(sigma, ell, onlyFeaturesFromMewObservation, onlyFeaturesFromMewObservation);
    covariancePrior = combineKMatrices(K0, K1, K2, K3);
    return covariancePrior;
  }

  /**
   *  Goal is to evaluate `unObservedGridEntries` */
  public MeanVariance evaluate(DoubleMatrix observedGridEntriesWithMeans, DoubleMatrix unObservedGridEntries) {

    DoubleMatrix onlyFeatures = observedGridEntriesWithMeans.getColumns(new IntervalRange(0, observedGridEntriesWithMeans.columns - 1)).transpose();
    DoubleMatrix onlyMeans = observedGridEntriesWithMeans.getColumn(observedGridEntriesWithMeans.columns - 1);

    ifNeededInitCovariancePrior(onlyFeatures); // maybe this update should come from outside of surrogate model similar to single entry updates
//    DoubleMatrixUtils.multilinePrint("Covariance prior:", covariancePrior);

    //Once we got prior covariance matrix we can predict for all the other unobserved grid entries
    MeanVariance meanVariance = posteriorMeanAndVariance(sigma, ell, covariancePrior, onlyFeatures, unObservedGridEntries, onlyMeans);

    return meanVariance;
  }

  /**
   * observedDataDM: row-vector
   *    ( Xi1  Xi2 Xi3.... ) for one variable case ie i=1
   *
   * observedMeansDM: is a column-vector
   */
  double[] gridSearchOverGPsHyperparameters(DoubleMatrix observedDataDM, DoubleMatrix observedMeansDM) {
    double bestSigma = 0.1;
    double bestEll = 0.1;
    double mll = Double.NEGATIVE_INFINITY;

    System.out.println("Starting HPS search for GP...");
    for (double sigma = 0.1; sigma < 0.9; sigma+=0.1) {
      for (double ell = 0.1; ell < 6.4; ell+=0.1) {
        sigma = (double) Math.round(sigma * 10) / 10;
        ell = (double) Math.round(ell*10) / 10;
        System.out.println("Evaluating for Sigma = " + sigma + ", Ell = " + ell);
        int n = observedDataDM.rows;

        DoubleMatrix K = getCovarianceMtxWithGaussianKernel(sigma, ell, observedDataDM, observedDataDM);

//        System.out.println("Observed data:");
//        multilinePrint(observedDataDM);
//        System.out.println("Observed means:");
//        multilinePrint(observedMeansDM);
//        System.out.println("Matrix K:");
//        multilinePrint(K);

        // r = -1/2*t(y)%*%solve(K)%*%y - 1/2*log(det(K)) - n/2*log(pi*2)
        Decompose.QRDecomposition<DoubleMatrix> qr = Decompose.qr(K.add(eye(K.columns).mul(sigma * sigma))); // do we need to add noise here to K?
        double determinant = 1;
        for (double diagElem : qr.r.diag().data) {
          determinant *= diagElem;
        }
        System.out.println("Determinant non abs: " + determinant);

        double det_K = Math.abs(determinant);
        DoubleMatrix firstTerm = observedMeansDM.transpose().mul(-0.5).mmul(Solve.solve(K, eye(K.columns).mul(sigma * sigma))).mmul(observedMeansDM);
        double r = firstTerm.get(0) - 0.5 * Math.log(det_K) - ((double) n/2) * Math.log(Math.PI*2);
        if(r > mll) {
          mll = r;
          bestSigma = sigma;
          bestEll = ell;
          System.out.println("Found better HPS that maximise MaximumLogLikelihood (" + mll + ": Sigma = " + bestSigma + ", Ell = " + bestEll);
        }
      }
    }
    
    return new double[] {bestSigma, bestEll};
  }



}
