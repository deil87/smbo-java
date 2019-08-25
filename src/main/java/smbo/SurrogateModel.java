package smbo;

import org.jblas.DoubleMatrix;

public abstract class SurrogateModel {
  public abstract GPSurrogateModel.MeanVariance predictMeansAndVariances(DoubleMatrix observedGridEntries, DoubleMatrix unObservedGridEntries);
}
