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

  // This is an analogy to the concept of Simulated Annealing. It is not the same as increasing priorSize by `_explorationEnergy` value. Here we already aware about variance and uncertanty.
  private int _explorationEnergy = 10;
  public boolean isIncumbentColdStartSetupHappened() {
    return _incumbentColdStartSetupHappened;
  }
  
  // Exploration vs. exploitation tradeOff
  //abs(_tradeOff) should not be bigger than max value of the function.... otherwise we will not be able to take variance into account in EI
  //          for example _tradeOff = -5 will make it inefficient for function where max(f) = 5
  private double _tradeOff = 0.0;
  
  // Note: we assume that we are using EI for optimisation of metrics that are positive.
  private boolean _theBiggerTheBetter;

  public EI(double tradeoff, int explorationEnergy, boolean theBiggerTheBetter) {
    _tradeOff = tradeoff;
    _explorationEnergy = explorationEnergy;
    _theBiggerTheBetter = theBiggerTheBetter;
  }

  public EI(double tradeoff, boolean theBiggerTheBetter) {
    this(tradeoff, 10, theBiggerTheBetter);
  }

  //TODO we should set incumbent right after prior evaluation and not after a first evaluation which is after prior evaluation
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
    double currentTradeOff = _tradeOff;
    //TODO subtracting incumbent is already a way to minimize affect of big function values
//    if(_explorationEnergy > 0) {
//      currentTradeOff = _theBiggerTheBetter ? -_incumbent: _incumbent;
//      _explorationEnergy--;
//      System.out.println("Exploration energy left: " + _explorationEnergy);
//    }
    //TODO do we need to .mul(-1) if add and sub are swapped?
    DoubleMatrix mTermMtx = _theBiggerTheBetter ? means.sub(_incumbent).add(_tradeOff) : means.mul(-1).sub(_tradeOff).add(_incumbent);
    return mTermMtx;
  }

}
