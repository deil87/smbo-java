package smbo;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.jblas.DoubleMatrix;
import org.jblas.MatrixFunctions;

/**
 * def calculate_f():
 *             z = (self.eta - m - self.par) / s
 *             return (self.eta - m - self.par) * norm.cdf(z) + s * norm.pdf(z)
 */
public class EI extends AcquisitionFunction {

  private double _incumbent = 0.0;
  private boolean _incumbentColdStartSetupHappened = false;
  public boolean isIncumbentColdStartSetupHappened() {
    return _incumbentColdStartSetupHappened;
  }
  
  // Exploration vs. exploitation tradeOff
  private double _tradeOff = 0.0;
  
  // Note: we assume that we are using EI for optimisation of metrics that are positive.
  private boolean _theBiggerTheBetter;

  public EI(double tradeoff, boolean theBiggerTheBetter) {
    _tradeOff = tradeoff;
    _theBiggerTheBetter = theBiggerTheBetter;
  }

  public void setIncumbent(double incumbent) {
    _incumbent = incumbent;
    _incumbentColdStartSetupHappened = true; 
  }
  
  public void updateIncumbent(double possiblyNewIncumbent) {
    if(possiblyNewIncumbent > _incumbent) setIncumbent(possiblyNewIncumbent);
  }

  /**
   * 
   * @param means per line predictions of mean marginalised over all predictors
   * @param variances per line sds of predictions marginalised over all predictors
   * @return weighted(exploitation/extrapolation) evaluation of the unseen entries from the hyperparameters space
   */
  public DoubleMatrix compute(DoubleMatrix means, DoubleMatrix variances) {

    DoubleMatrix sdTerm = MatrixFunctions.sqrt(variances);

    DoubleMatrix mTermValue = computeMTerm(means);

    DoubleMatrix zTermValueDM = mTermValue.div(sdTerm);

    DoubleMatrix pdfZTerm = computePDF(zTermValueDM);

    DoubleMatrix cdfZTerm = computeCDF(zTermValueDM);

    // Here we can balance   ( exploitation + exploration ) terms
    DoubleMatrix af = mTermValue.mul(cdfZTerm).add(sdTerm.mul(pdfZTerm)/*.mul(25)*/);

    return af;
  }

  DoubleMatrix computeCDF(DoubleMatrix zTermValueDM) {
    DoubleMatrix cdfZTerm = null;
    NormalDistribution normalDistribution = new NormalDistribution();
    for (DoubleMatrix zTerm : zTermValueDM.rowsAsList()) {
      DoubleMatrix cdfZTermPerRowDM = new DoubleMatrix(1, 1, normalDistribution.cumulativeProbability(zTerm.get(0, 0)));
      if (cdfZTerm == null) {
        cdfZTerm = cdfZTermPerRowDM;
      } else {
        cdfZTerm = DoubleMatrix.concatVertically(cdfZTerm, cdfZTermPerRowDM);
      }
    }
    return cdfZTerm;
  }

  DoubleMatrix computePDF(DoubleMatrix zTermValueDM) {
    DoubleMatrix pdfZTerm = null;
    NormalDistribution normalDistribution = new NormalDistribution();
    for (DoubleMatrix zTerm : zTermValueDM.rowsAsList()) {
      DoubleMatrix pdfZTermPerRowDM = new DoubleMatrix(1, 1, normalDistribution.density(zTerm.get(0, 0)));
      if (pdfZTerm == null) {
        pdfZTerm = pdfZTermPerRowDM;
      } else {
        pdfZTerm = DoubleMatrix.concatVertically(pdfZTerm, pdfZTermPerRowDM);
      }
    }
    return pdfZTerm;
  }

  DoubleMatrix computeMTerm(DoubleMatrix means) {
    return _theBiggerTheBetter ? means.sub(_incumbent).add(_tradeOff) : means.mul(-1).sub(_tradeOff).add(_incumbent);
  }

}
