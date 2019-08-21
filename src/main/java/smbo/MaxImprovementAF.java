package smbo;

import org.jblas.DoubleMatrix;

public class MaxImprovementAF extends AcquisitionFunction {

  private double _incumbent = 0.0;
  private boolean _incumbentColdStartSetupHappened = false;
  public boolean isIncumbentColdStartSetupHappened() {
    return _incumbentColdStartSetupHappened;
  }

  // Note: we assume that we are using EI for optimisation of metrics that are positive.
  private boolean _theBiggerTheBetter;

  public MaxImprovementAF(boolean theBiggerTheBetter) {
    _theBiggerTheBetter = theBiggerTheBetter;
  }

  public void setIncumbent(double incumbent) {
    _incumbent = incumbent;
    _incumbentColdStartSetupHappened = true;
  }

  public double getIncumbent() {
    return _incumbent;
  }

  public void updateIncumbent(double possiblyNewIncumbent) {
    if(possiblyNewIncumbent > _incumbent) setIncumbent(possiblyNewIncumbent);
  }

  @Override
  public DoubleMatrix compute(DoubleMatrix mean, DoubleMatrix variances) {
    if(_theBiggerTheBetter) {
      return mean.sub(_incumbent);
    }
    return null;
  }

}
