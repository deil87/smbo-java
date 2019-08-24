package smbo.kernel;

import org.jblas.DoubleMatrix;
import org.jblas.Geometry;

import static org.jblas.MatrixFunctions.pow;

public class RationalQuadraticKernel extends Kernel {
  double alpha;

  public RationalQuadraticKernel(double alpha) {
    this.alpha = alpha;
  }

  @Override
  public DoubleMatrix apply(double signalVariance, double ell, double noiseVariance, DoubleMatrix x1, DoubleMatrix x2) {
    DoubleMatrix distance = Geometry.pairwiseSquaredDistances(x1, x2);
    DoubleMatrix mainTerm = distance.div(2* ell * ell * alpha).add(1);
    return pow(mainTerm, -alpha).mul(signalVariance * signalVariance).add(noiseVariance * noiseVariance);
  }

}
