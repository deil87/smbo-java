package smbo.kernel;

import org.jblas.DoubleMatrix;

public abstract class Kernel {

  public abstract DoubleMatrix apply(double signalVariance, double ell, double noiseVariance, DoubleMatrix x1, DoubleMatrix x2);
}
