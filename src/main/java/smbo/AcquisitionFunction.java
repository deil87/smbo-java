package smbo;


import org.jblas.DoubleMatrix;

public abstract class AcquisitionFunction {
  public abstract boolean isIncumbentColdStartSetupHappened();
  public abstract void setIncumbent(double incumbent);
  public abstract void updateIncumbent(double possiblyNewIncumbent);
  public abstract DoubleMatrix compute(DoubleMatrix medians, DoubleMatrix variances);
}

