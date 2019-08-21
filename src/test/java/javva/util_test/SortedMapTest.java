package javva.util_test;

import org.junit.Assert;
import org.junit.Test;

import java.util.*;

import static org.jblas.DoubleMatrix.zeros;

public class SortedMapTest {

  @Test
  public void sortedMap() {
    int size = 5;
    Double[] gridEntries = new Double[size*10];
    int i;
    for (i = 0; i < size*10; i++) {
      gridEntries[i] = (double) i / 10;
    }

    SortedMap<String, Object[]> grid = Collections.synchronizedSortedMap(new TreeMap());
    grid.put("X", gridEntries);
    grid.put("Z", gridEntries);
    grid.put("Y", gridEntries);
    grid.put("AA", gridEntries);

    ArrayList<String> arr = new ArrayList<String>();
    for (Map.Entry<String, Object[]> gridItem : grid.entrySet()) {
        arr.add(gridItem.getKey());
      }

    Assert.assertArrayEquals(new String[]{"AA", "X", "Y", "Z"}, arr.toArray(new String[]{}));
    Assert.assertArrayEquals(grid.keySet().toArray(), arr.toArray(new String[]{}));
  }
}
