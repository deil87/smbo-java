package smbo;

import org.jblas.*;
import org.jblas.ranges.IntervalRange;
import smbo.kernel.Kernel;
import smbo.kernel.RationalQuadraticKernel;
import smbo.kernel.SquaredExponentialKernel;

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

  final Kernel kernel;
  private DoubleMatrix covariancePrior = null;
  
  private double _currentBestSigma = -1;
  private double _currentBestEll = -1;

  // Whether we performed hyper parameters search for GP based on prior
  boolean initialisationHappenedForGPHps = false;

  double sigma = 0.6;
  double ell = 2.0;
  double noiseVariance = 0.1;

  public GPSurrogateModel(Kernel kernel, double sigma, double ell, double noiseVariance) {
//    this.kernel = new SquaredExponentialKernel();
    this.kernel = kernel; //new RationalQuadraticKernel(12); // TODO alpha should be searched for
    this.sigma = sigma;
    this.ell = ell;
    //TODO noiseVariance?
  }
  public GPSurrogateModel() {
    this.kernel = new RationalQuadraticKernel(12);;
  }

  public double getSigma() {
    return sigma;
  }

  public double getEll() {
    return ell;
  }

  public double getNoiseVariance() {
    return noiseVariance;
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
  DoubleMatrix getCovarianceMtxWithKernel(Kernel kernel, double signalVariance, double ell, double noiseVariance, DoubleMatrix x1, DoubleMatrix x2) {
    return kernel.apply(signalVariance, ell, noiseVariance, x1, x2);
  }

  //TODO we can keep flag/state in GPSMBO and
  public void performHpsGridSearchAndUpdateHps(DoubleMatrix observedDataOnlyFeatures, DoubleMatrix means) {
    boolean isGSOverGPHyperparametersEnabled = true;
    if(!initialisationHappenedForGPHps && isGSOverGPHyperparametersEnabled) {
      // but in that way our covariancePrior was computed with different sigma and ell from previous iteration
      double[] bestFoundGPHps = gridSearchOverGPsHyperparameters(observedDataOnlyFeatures.transpose(), means);
      sigma = bestFoundGPHps[0];
      ell = bestFoundGPHps[1];
      noiseVariance = bestFoundGPHps[2];
      System.out.println("Best sigma:" + sigma + "     Best ell:" + ell + "     Best noise variance:" + noiseVariance + ". Number of observed = " + observedDataOnlyFeatures.rows);
      initialisationHappenedForGPHps = true;
    }
  }

  //  K0 + sigma^2*I    K1
  //  K2                K3
  //
  // 
  MeanVariance posteriorMeanAndVariance(double sigma, double ell, double noiseVariance, DoubleMatrix covariancePrior, DoubleMatrix observedData, DoubleMatrix unobservedEntrie, DoubleMatrix priorMeans) {
    int n = observedData.columns;
    assert priorMeans.rows == n;

    System.out.println("Sigma:" + sigma + "     Ell:" + ell + "     NoiseVariance:" + noiseVariance + ". Number of observed = " + n);

    DoubleMatrix covarianceBetweenNewAndPrior = getCovarianceMtxWithKernel(kernel, sigma, ell, noiseVariance, observedData, unobservedEntrie);   // K1
    DoubleMatrix K2 = covarianceBetweenNewAndPrior.transpose();
    DoubleMatrix covarianceForNewObservations = getCovarianceMtxWithKernel(kernel, sigma, ell, noiseVariance, unobservedEntrie, unobservedEntrie); // K3

    DoubleMatrix K = combineKMatrices(covariancePrior, covarianceBetweenNewAndPrior, K2, covarianceForNewObservations);

    // Step 1: Compute means

    // mt(x) = K(X*, Xt) * [K(Xt , Xt) + σ^2 * I ]^-1 * yt 
    //
    //           ^^                ^^                 ^^
    //           K2                 b                  c (= priorMeans)

    DoubleMatrix identity = eye(n);
    DoubleMatrix b = Solve.solve(covariancePrior.add(eye(n).mul(noiseVariance * noiseVariance)), identity); // solve with Identity B matrix in Ax=B will return us inverse.

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
  /**
   *
   * @param observedGridEntriesOnlyFeatures column-vector
   */
  public DoubleMatrix ifNeededInitCovariancePrior(DoubleMatrix observedGridEntriesOnlyFeatures) {
    if(covariancePrior == null)
      covariancePrior = getCovarianceMtxWithKernel(kernel, sigma, ell, noiseVariance, observedGridEntriesOnlyFeatures, observedGridEntriesOnlyFeatures);
    return covariancePrior;
  }

  // TODO not tested
  public DoubleMatrix updateCovariancePrior(DoubleMatrix observedGridEntries, DoubleMatrix newObservationFromObjectiveFun) {
    assert covariancePrior != null; // as we can't say that we updating something before it was initialised
    DoubleMatrix K0 = covariancePrior;

    DoubleMatrix onlyFeaturesFromObserved = observedGridEntries.getColumns(new IntervalRange(0, observedGridEntries.columns - 1)).transpose();
    DoubleMatrix onlyFeaturesFromMewObservation = newObservationFromObjectiveFun.getColumns(new IntervalRange(0, newObservationFromObjectiveFun.columns - 1)).transpose();
    DoubleMatrix K1 = getCovarianceMtxWithKernel(kernel, sigma, ell, noiseVariance, onlyFeaturesFromObserved, onlyFeaturesFromMewObservation);
    DoubleMatrix K2 = K1.transpose();
    DoubleMatrix K3 = getCovarianceMtxWithKernel(kernel, sigma, ell, noiseVariance, onlyFeaturesFromMewObservation, onlyFeaturesFromMewObservation);
    covariancePrior = combineKMatrices(K0, K1, K2, K3);
    return covariancePrior;
  }

  /**
   *  Goal is to predictMeansAndVariances for `unObservedGridEntries` */
  public MeanVariance predictMeansAndVariances(DoubleMatrix observedGridEntriesWithMeans, DoubleMatrix unObservedGridEntries) {

    DoubleMatrix onlyFeatures = observedGridEntriesWithMeans.getColumns(new IntervalRange(0, observedGridEntriesWithMeans.columns - 1)).transpose();
    DoubleMatrix onlyMeans = observedGridEntriesWithMeans.getColumn(observedGridEntriesWithMeans.columns - 1);

    ifNeededInitCovariancePrior(onlyFeatures); // TODO maybe this update should come from outside of surrogate model similar to single entry updates

    //Once we got prior covariance matrix we can predict for all the other unobserved grid entries
    MeanVariance meanVariance = posteriorMeanAndVariance(sigma, ell, noiseVariance, covariancePrior, onlyFeatures, unObservedGridEntries, onlyMeans);

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
    double bestNoiseVariance = 0.01;
    double[] noiseVariances = new double[] {0.01, 0.05, 0.1};
    double mll = Double.NEGATIVE_INFINITY;

    System.out.println("Starting HPS search for GP...");
    for (double sigma = 0.1; sigma < 0.9; sigma+=0.1) {
      for (double ell = 0.1; ell < 6.4; ell+=0.1) {
        for (double noiseVariance : noiseVariances) {
          sigma = (double) Math.round(sigma * 10) / 10;
          ell = (double) Math.round(ell * 10) / 10;
          System.out.println("Evaluating for Sigma = " + sigma + ", Ell = " + ell);
          int n = observedDataDM.rows;

          DoubleMatrix K = getCovarianceMtxWithKernel(kernel, sigma, ell, noiseVariance, observedDataDM, observedDataDM);

//        System.out.println("Observed data:");
//        multilinePrint(observedDataDM);
//        System.out.println("Observed means:");
//        multilinePrint(observedMeansDM);
//        System.out.println("Matrix K:");
//        multilinePrint(K);

          // r = -1/2*t(y)%*%solve(K)%*%y - 1/2*log(det(K)) - n/2*log(pi*2)
          Decompose.QRDecomposition<DoubleMatrix> qr = Decompose.qr(K.add(eye(K.columns).mul(noiseVariance * noiseVariance))); // do we need to add noise here to K?
          double determinant = 1;
          for (double diagElem : qr.r.diag().data) {
            determinant *= diagElem;
          }
          System.out.println("Determinant non abs: " + determinant);

          double det_K = Math.abs(determinant);
          DoubleMatrix firstTerm = observedMeansDM.transpose().mul(-0.5).mmul(Solve.solve(K.add(eye(K.columns).mul(noiseVariance * noiseVariance)), eye(K.columns))).mmul(observedMeansDM);
          double r = firstTerm.get(0) - 0.5 * Math.log(det_K) - ((double) n / 2) * Math.log(Math.PI * 2);
          System.out.println("r = " + r);
          if (r > mll) {
            mll = r;
            bestSigma = sigma;
            bestEll = ell;
            bestNoiseVariance = noiseVariance;
            System.out.println("Found better HPS that maximise MaximumLogLikelihood (" + mll + ": Sigma = " + bestSigma + ", Ell = " + bestEll + ", NoiseVariance = " + bestNoiseVariance );
          }
        }
      }
    }
    
    return new double[] {bestSigma, bestEll, bestNoiseVariance};
  }



}
