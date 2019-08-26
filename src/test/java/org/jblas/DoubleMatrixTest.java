package org.jblas;

import org.junit.Assert;
import org.junit.Test;
import utils.DoubleMatrixUtils;

import java.io.IOException;

public class DoubleMatrixTest {

  @Test
  public void getLastColumn() {
    DoubleMatrix matrix = new DoubleMatrix(1, 5, 1,2,3,4,5);
    Assert.assertEquals(5, matrix.getColumn(matrix.columns - 1).get(0,0), 1e-5);
  }

  @Test
  public void argMaxCols_argMinCols() {
    DoubleMatrix matrix = new DoubleMatrix(2, 5, 1,2,3,4,5, 6,7,8,9,10);
    DoubleMatrixUtils.multilinePrint(matrix);
    int[] columnArgmaxs = matrix.columnArgmaxs();
    Assert.assertEquals(1, columnArgmaxs[4], 1e-5);

    int[] columnArgmins = matrix.columnArgmins();
    Assert.assertEquals(0, columnArgmins[4], 1e-5);
  }

  @Test
  public void orderOfOperations() {
    DoubleMatrix matrix = new DoubleMatrix(2, 5, 2,2,2,2,2, 2,2,2,2,2);
    DoubleMatrixUtils.multilinePrint(matrix);
    DoubleMatrix result = matrix.div(2).add(0.5);
    DoubleMatrixUtils.multilinePrint(result);

    DoubleMatrix expected = new DoubleMatrix(2, 5, 1.5,1.5,1.5,1.5,1.5, 1.5,1.5,1.5,1.5,1.5);
    Assert.assertEquals(expected, result);
  }

  //Saving is not CSV.
  @Test
  public void save() throws IOException {
    DoubleMatrix matrix = new DoubleMatrix(2, 5, 2,2,2,2,2, 2,2,2,2,2);
    DoubleMatrixUtils.multilinePrint(matrix);
//    matrix.save("DoubleMatrixSaveToFile"); // saves as binary

    String fileName = "DoubleMatrixSaveToFile.csv";
    DoubleMatrixUtils.save(fileName, matrix);

    DoubleMatrix loaded = DoubleMatrix.loadCSVFile(fileName);

    Assert.assertEquals(matrix,loaded);


  }
}
