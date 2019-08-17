package org.jblas;

import org.junit.Test;
import utils.TestUtils;

import static org.jblas.DoubleMatrix.eye;
import static org.jblas.DoubleMatrix.zeros;

// Purpose is to check that finding inverse matrix is equivalent to solving with identity B matrix
public class InverseMatrixTest {

  @Test
  public void getInverse() {
    // Because it is being constructed by columns we need to transpose to get conventional form
    DoubleMatrix matrix = new DoubleMatrix(2, 2, 1,2,3,4).transpose();
    DoubleMatrix identity = eye(2);
    DoubleMatrix inverseOfMatrix = Solve.solve(matrix, identity);
    TestUtils.multilinePrint(inverseOfMatrix);

    // A = matrix, B = zeroes
    DoubleMatrix inverseOfMatrix2 = Solve.solve(matrix, zeros(2));
    TestUtils.multilinePrint(inverseOfMatrix2);
  }

  @Test
  public void getInverse3by3() {
    DoubleMatrix matrix3 = new DoubleMatrix(3, 3, 7,5,1,4,4,9,12,3,2).transpose();
    DoubleMatrix identity = eye(3);
    TestUtils.multilinePrint(matrix3);
    TestUtils.multilinePrint(identity);
    DoubleMatrix inverseOfMatrix3 = Solve.solve(matrix3, identity);
    TestUtils.multilinePrint(inverseOfMatrix3);
  }
}
