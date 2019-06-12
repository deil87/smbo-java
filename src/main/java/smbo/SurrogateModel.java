package smbo;

import org.jblas.DoubleMatrix;

public abstract class SurrogateModel {
  public abstract GPSurrogateModel.MeanVariance evaluate(DoubleMatrix observedGridEntries, DoubleMatrix unObservedGridEntries);
}
