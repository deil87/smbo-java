package smbo.kernel;

import org.jblas.DoubleMatrix;
import org.jblas.Geometry;

import static org.jblas.MatrixFunctions.exp;

public class SquaredExponentialKernel extends Kernel {
  @Override
  public DoubleMatrix apply(double signalVariance, double ell, double noiseVariance, DoubleMatrix x1, DoubleMatrix x2) {
    DoubleMatrix distance = Geometry.pairwiseSquaredDistances(x1, x2);
    DoubleMatrix mainTerm = distance.mul(0.5).div(ell * ell);
    return exp(mainTerm.neg()).mul(signalVariance * signalVariance).add(noiseVariance * noiseVariance);
  }
}
