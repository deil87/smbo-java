package utils;

import org.jblas.DoubleMatrix;

public abstract class TestUtils {

  public static void multilinePrint(DoubleMatrix matrix) {
    for (int i = 0; i < matrix.rows; i++) {
      matrix.getRow(i).print();
    }
  }

  public static void multilinePrint(String label, DoubleMatrix matrix) {
    System.out.println(label);
    for (int i = 0; i < matrix.rows; i++) {
      matrix.getRow(i).print();
    }
  }

}
