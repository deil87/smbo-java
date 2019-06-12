package smbo;

import org.jblas.DoubleMatrix;

public class EvaluatedGE {
  // TODO or maybe keep `dependantVariables` and `response` as one matrix with different columns?
  public DoubleMatrix dependantVariables;
  public DoubleMatrix response;

  public EvaluatedGE(DoubleMatrix dependantVariables, DoubleMatrix response) {
    this.dependantVariables = dependantVariables;
    this.response = response;
  }
}
