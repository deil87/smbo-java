package smbo;

import org.jblas.DoubleMatrix;
import utils.DoubleMatrixUtils;

public class DiverseSelector {

  /**
   *
   * @param vec1 row-vector
   * @param vec2 row-vector
   * @return
   */
    double cosSimilarity(DoubleMatrix vec1, DoubleMatrix vec2) {

      DoubleMatrix mul = vec1.mul(vec2);
//      DoubleMatrixUtils.multilinePrint(mul);
      DoubleMatrix dotProduct = mul.rowSums();
      double dot = vec1.dot(vec2);
      assert dot == dotProduct.get(0, 0);
      double normOfVec1 = vec1.norm2();
      double normOfVec2 = vec2.norm2();
      double cos = dot / (normOfVec1 * normOfVec2);
      return cos;
    }

  /**
   * @param vec
   * @param vecs
   * @return column vector of cosine similarities
   */
  DoubleMatrix oneToManyCosSimilarity(DoubleMatrix vec, DoubleMatrix vecs) {
    DoubleMatrix similarities = null;
    for (DoubleMatrix row : vecs.rowsAsList()) {
      DoubleMatrix newEntry = new DoubleMatrix(1, 1, cosSimilarity(vec, row));
      similarities = similarities== null ? newEntry : DoubleMatrix.concatVertically(similarities, newEntry);
    }
    return similarities;
  }


  DoubleMatrix oneToManySelectMostDistant(DoubleMatrix vec, DoubleMatrix vecs) {
    DoubleMatrix similarities = oneToManyCosSimilarity(vec, vecs);
    int indexOfMostDistant = similarities.columnArgmins()[0]; // In cosine similarity the smaller value the farthest it is
    return vecs.getRow(indexOfMostDistant);
  }

  MostDistantEntry manyToManySelectMostDistant(DoubleMatrix baseRows, DoubleMatrix rows) {
    DoubleMatrix cummulativeSimilarities = null;
    for (DoubleMatrix baseRow : baseRows.rowsAsList()) {
      DoubleMatrix similarities = oneToManyCosSimilarity(baseRow, rows);
      DoubleMatrixUtils.multilinePrint("Similarity", similarities);

      cummulativeSimilarities = cummulativeSimilarities == null ? similarities : cummulativeSimilarities.add(similarities);
    }
    DoubleMatrixUtils.multilinePrint("Cumulative similarities", cummulativeSimilarities);
    int indexOfMostDistant = cummulativeSimilarities.columnArgmins()[0];
    return new MostDistantEntry(indexOfMostDistant, rows.getRow(indexOfMostDistant));

  }

  public static class MostDistantEntry {
    int index;
    DoubleMatrix entry;

    public MostDistantEntry(int index, DoubleMatrix entry) {
      this.index = index;
      this.entry = entry;
    }
  }

  double pairwiseCosSimilarity(DoubleMatrix mtx1, DoubleMatrix mtx2) {
      throw new UnsupportedOperationException();
  }


}
