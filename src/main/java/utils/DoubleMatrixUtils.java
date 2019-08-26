package utils;

import org.jblas.DoubleMatrix;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public abstract class DoubleMatrixUtils {

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

  public static void save(String fileName, DoubleMatrix matrix) throws IOException {
    double[][] as2DArray = matrix.toArray2();

    BufferedWriter br = new BufferedWriter(new FileWriter(fileName));

    // Append strings from array
    for (double[] row : as2DArray) {
      StringBuilder sb = new StringBuilder();
      int length = row.length;
      int idx = 0;
      for( double rowItem : row) {
        sb.append(rowItem);
        if(idx != length-1)
          sb.append(",");
        idx++;
      }
      sb.append("\n");
      br.write(sb.toString());
    }
    br.close();

  }

}
