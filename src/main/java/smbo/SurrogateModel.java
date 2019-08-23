package smbo;

import org.jblas.DoubleMatrix;

public abstract class SurrogateModel {
  public abstract GPSurrogateModel.MeanVariance predictMeanAndVariance(DoubleMatrix observedGridEntries, DoubleMatrix unObservedGridEntries);
}
