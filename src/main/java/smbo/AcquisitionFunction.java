package smbo;


import org.jblas.DoubleMatrix;

public abstract class AcquisitionFunction implements Cloneable{
  public abstract boolean isIncumbentColdStartSetupHappened();
  public abstract void setIncumbent(double incumbent);
  public abstract double getIncumbent();
  public abstract void updateIncumbent(double possiblyNewIncumbent);
  public abstract DoubleMatrix compute(DoubleMatrix medians, DoubleMatrix variances);

  // TODO probably we don't need cloning functionality for current usage
  protected Object clone() throws CloneNotSupportedException {
    return super.clone();
  }

  // TODO probably we don't need cloning functionality for current usage
  protected AcquisitionFunction cloneTyped() throws CloneNotSupportedException {
    return (AcquisitionFunction) clone();
  }
}

