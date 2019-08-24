package smbo;

import org.jblas.DoubleMatrix;
import org.junit.Test;
import utils.DoubleMatrixUtils;

import static org.junit.Assert.*;

public class DiverseSelectorTest {

  @Test
  public void cosSimilarityForSameVectors() {

    DoubleMatrix vec1 = new DoubleMatrix(1, 3, 1, 2, 3);
    DoubleMatrixUtils.multilinePrint(vec1);
    DiverseSelector diverseSelector = new DiverseSelector();
    double cosSimilarity = diverseSelector.cosSimilarity(vec1, vec1);

    assertEquals(1, cosSimilarity, 1e-5);
  }

  @Test
  public void cosSimilarity() {

    DoubleMatrix vec1 = new DoubleMatrix(1, 3, 1, 2, 3);
    DoubleMatrix vec2 = new DoubleMatrix(1, 3, 1, 2, 0);
    DoubleMatrixUtils.multilinePrint(vec1);
    DoubleMatrixUtils.multilinePrint(vec2);
    DiverseSelector diverseSelector = new DiverseSelector();
    double cosSimilarity = diverseSelector.cosSimilarity(vec1, vec2);

    assertNotEquals(1, cosSimilarity, 1e-5);
  }

  @Test
  public void oneVsManyCosineSimilarity() {

    DiverseSelector diverseSelector = new DiverseSelector();

    DoubleMatrix vec1 = new DoubleMatrix(1, 3, 1, 2, 3);
    DoubleMatrix rows = new DoubleMatrix(2, 3, 1, 1, 2, 2,3,0);
    DoubleMatrixUtils.multilinePrint("Vec1", vec1);
    DoubleMatrixUtils.multilinePrint("Rows", rows);

    DoubleMatrix a = new DoubleMatrix(1, 3, 1, 2, 3);
    DoubleMatrix b = new DoubleMatrix(1, 3, 1, 2, 0);

    double cosSimilarityA_B = diverseSelector.cosSimilarity(a, b);

    DoubleMatrix vecCosSimilarities = diverseSelector.oneToManyCosSimilarity(vec1, rows);

    assertEquals(new DoubleMatrix(2,1, 1, cosSimilarityA_B), vecCosSimilarities);
  }

  @Test
  public void oneToManySelectMostDistant() {

    DiverseSelector diverseSelector = new DiverseSelector();

    DoubleMatrix vec1 = new DoubleMatrix(1, 3, 1, 2, 3);
    DoubleMatrix rows = new DoubleMatrix(3, 3, 1, 1, 1, 2, 2, 2,3,0,1);
    DoubleMatrixUtils.multilinePrint("Vec1", vec1);
    DoubleMatrixUtils.multilinePrint("Rows", rows);

    DoubleMatrix mostDistant = diverseSelector.oneToManySelectMostDistant(vec1, rows);

    assertEquals(new DoubleMatrix(3,1, 1, 2, 0).transpose(), mostDistant);
  }

  @Test
  public void manyToManySelectMostDistant() {

    DiverseSelector diverseSelector = new DiverseSelector();

    DoubleMatrix vec1 = new DoubleMatrix(1, 3, 1, 2, 3);
    DoubleMatrix vec2 = new DoubleMatrix(1, 3, 3, 4, 5);
    DoubleMatrix baseRows = DoubleMatrix.concatVertically(vec1, vec2);
    DoubleMatrix rows = new DoubleMatrix(3, 3, 1, 2, 4, 3, 4, 7,2,3,4).transpose();
    DoubleMatrixUtils.multilinePrint(rows);
    DoubleMatrixUtils.multilinePrint("Vec1", vec1);
    DoubleMatrixUtils.multilinePrint("Rows", rows);

    DoubleMatrix mostDistant = diverseSelector.manyToManySelectMostDistant(baseRows, rows).entry;

    assertEquals(new DoubleMatrix(3,1, 1, 2, 4).transpose(), mostDistant);
  }

  @Test
  public void manyToManySelectMostDistant_2D_case() {

    DiverseSelector diverseSelector = new DiverseSelector();

    DoubleMatrix vec1 = new DoubleMatrix(1, 2, 1, 2);
    DoubleMatrix vec2 = new DoubleMatrix(1, 2, 3, 1);
    DoubleMatrix baseRows = DoubleMatrix.concatVertically(vec1, vec2);
    DoubleMatrix rows = new DoubleMatrix(2, 3, 3, 2, 2, 2, 4, 1).transpose();
    DoubleMatrixUtils.multilinePrint(rows);
    DoubleMatrixUtils.multilinePrint("Vec1", vec1);
    DoubleMatrixUtils.multilinePrint("Rows", rows);

    DoubleMatrix mostDistant = diverseSelector.manyToManySelectMostDistant(baseRows, rows).entry;

    assertEquals(new DoubleMatrix(2,1, 4, 1).transpose(), mostDistant);
  }
}