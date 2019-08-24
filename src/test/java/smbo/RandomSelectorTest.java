package smbo;

import org.junit.Assert;
import org.junit.Test;

import java.util.*;

public class RandomSelectorTest {

  @Test
  public void cloneTyped() throws RandomSelector.NoUnexploredGridEntitiesLeft{
    long seed = 12345;

    SortedMap<String, Object[]> grid = Collections.synchronizedSortedMap(new TreeMap());
    grid.put("X", new Object[]{1,2,3,4,5,6,7,8,9});
    RandomSelector randomSelector = new RandomSelector(grid, seed);
    randomSelector.getNext();

    assert randomSelector.getVisitedPermutationHashes().size() == 1;

    RandomSelector copyOfRandomSelector = randomSelector.cloneTyped();
    randomSelector.getNext();

    assert randomSelector.getVisitedPermutationHashes().size() == 2;
    assert copyOfRandomSelector.getVisitedPermutationHashes().size() == 1;
  }

  public void getNext() throws RandomSelector.NoUnexploredGridEntitiesLeft {

    long seed = 12345;
    SortedMap<String, Object[]> grid = Collections.synchronizedSortedMap(new TreeMap());
    grid.put("X", new Object[]{1,2,3,4,5,6,7,8,9});

    RandomSelector randomSelector = new RandomSelector(grid, seed);

    Set<Object> set = new HashSet<>();
    try {
      for (int i = 0; i < grid.get("X").length; i++) {
        GridEntry next = randomSelector.getNext();
        Map<String, Object> item = next.getEntry();
        for (Map.Entry<String, Object> entry : item.entrySet()) {
          System.out.println("Selected dim " + entry.getKey() + " with value " + entry.getValue());
          set.add(entry.getValue());
        }
      }
    } catch (RandomSelector.NoUnexploredGridEntitiesLeft ex) {

    }

    Assert.assertEquals(9, set.size());
    Assert.assertEquals(9, randomSelector.getVisitedPermutationHashes().size());

  }


}