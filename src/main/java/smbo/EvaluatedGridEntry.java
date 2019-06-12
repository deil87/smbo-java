package smbo;

class EvaluatedGridEntry {
  GridEntry _entry;

  double evaluatedRes = Double.MIN_VALUE;

  public EvaluatedGridEntry(GridEntry item, double value) {
    _entry = item;
    evaluatedRes = value;

  }
}
