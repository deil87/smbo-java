package smbo;

import org.jblas.DoubleMatrix;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.junit.Assert.*;

public class EvaluatedGridEntryTest {

  @Test
  public void getEvaluatedEntryAsMtx() {

    SortedMap<String, Object> grid = Collections.synchronizedSortedMap(new TreeMap());
    grid.put("X", 5.);
    grid.put("Z", 25.);
    grid.put("Y", 8.);
    int doesNotMatterHash = 42;
    GridEntry gridEntry = new GridEntry(grid, doesNotMatterHash);
    EvaluatedGridEntry evaluatedGridEntry = new EvaluatedGridEntry(gridEntry, 666.);

    Assert.assertEquals(new DoubleMatrix(1, grid.size() + 1, 5,8,25,666), evaluatedGridEntry.getEvaluatedEntryAsMtx());
  }
}