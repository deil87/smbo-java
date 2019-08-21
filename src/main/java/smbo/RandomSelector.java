package smbo;

import java.util.*;


class RandomSelector {

  SortedMap<String, Object[]> _grid;
  String[] _dimensionNames;

  private int _spaceSize;
  transient private Set<Integer> _visitedPermutationHashes = new LinkedHashSet<>();
  Random _randomGen;

  public RandomSelector(SortedMap<String, Object[]> grid, long seed) {
    _grid = grid;
    _randomGen = new Random(seed);
    _dimensionNames = _grid.keySet().toArray(new String[0]);
    _spaceSize = calculateSpaceSize(_grid);
  }

  public RandomSelector(SortedMap<String, Object[]> grid) {
    this(grid, -1);
  }

  GridEntry getNext() throws NoUnexploredGridEntitiesLeft {
    SortedMap<String, Object> _next = Collections.synchronizedSortedMap(new TreeMap());
    int[] indices = nextIndices();
    for (int i = 0; i < indices.length; i++) {
      _next.put(_dimensionNames[i], _grid.get(_dimensionNames[i])[indices[i]]);
    }
    return new GridEntry(_next, hashIntArray(indices));
  }

  //This approach is not very efficient as over time we will start to hit cache more often and selecting unseen combination will become harder.
  private int[] nextIndices() throws NoUnexploredGridEntitiesLeft {
    int[] chosenIndices =  new int[_dimensionNames.length];

    if(_visitedPermutationHashes.size() == _spaceSize) {
      throw new NoUnexploredGridEntitiesLeft(_spaceSize);
    }

    int hashOfIndices = 0;
    do {
      for (int i = 0; i < _dimensionNames.length; i++) {
        String name = _dimensionNames[i];
        int dimensionLength = _grid.get(name).length;
        int chosenIndex = _randomGen.nextInt(dimensionLength);
        chosenIndices[i] = chosenIndex;
      }
      hashOfIndices = hashIntArray(chosenIndices);
    } while (_visitedPermutationHashes.contains(hashOfIndices) && _visitedPermutationHashes.size() != _spaceSize /*&& skipIndices(chosenIndices)*/);
    _visitedPermutationHashes.add(hashOfIndices);


    return chosenIndices;
  }

  public int spaceSize() {
    return _spaceSize;
  }

  public static class NoUnexploredGridEntitiesLeft extends Exception{
    public int exploredCount;
    public NoUnexploredGridEntitiesLeft(int exploredCount) {
      this.exploredCount = exploredCount;
    }
  }

  private int calculateSpaceSize(Map<String, Object[]> grid) {
    String[] dimensionNames = grid.keySet().toArray(new String[0]);
    int spaceSize = 1;
    for (int i = 0; i < dimensionNames.length; i++) {
      String name = dimensionNames[i];
      int dimensionLength = grid.get(name).length;
      spaceSize *= dimensionLength;
    }
    return spaceSize;
  }

  public Set<Integer> getVisitedPermutationHashes() {
    return _visitedPermutationHashes;
  }

  private static int hashIntArray(int[] ar) {
    Integer[] hashMe = new Integer[ar.length];
    for (int i = 0; i < ar.length; i++)
      hashMe[i] = ar[i];
//        hashMe[i] = ar[i] * _grid.get(_dimensionNames[i]).length;
    return Arrays.deepHashCode(hashMe);
  }
}

