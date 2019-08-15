package smbo;

import org.jblas.DoubleMatrix;
import org.knowm.xchart.*;
import org.knowm.xchart.style.markers.SeriesMarkers;

import java.awt.*;
import java.io.IOException;

public class MeanVariancePlotHelper {


  /**
   *
   * @param unObservedGridEntries whole grid without points that were evaluated for prior
   * @param YValDM whole grid evaluated with real Objective Function
   * @param onlyPriorEvaluations matrix with prior evaluations
   * @param suggestions double matrix with suggestions
   * @throws IOException
   */
  static void plotAgainstRealOF(DoubleMatrix unObservedGridEntries, DoubleMatrix YValDM, DoubleMatrix onlyPriorEvaluations, DoubleMatrix suggestions, GPSMBO gpsmboForSuggestions ) throws IOException {
    XYChart chart = new XYChartBuilder().width(1000).height(500).title("SMBO suggestions").xAxisTitle("X").yAxisTitle("Y").build();
    chart.getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Scatter);
    chart.getStyler().setLegendVisible(false);

    chart.addSeries("Original OF", unObservedGridEntries.toArray(), YValDM.toArray());
    XYSeries series = chart.addSeries("Prior", onlyPriorEvaluations.getColumn(0).toArray(), onlyPriorEvaluations.getColumn(1).toArray());
    series.setMarker(SeriesMarkers.DIAMOND);
    for(int si = 0; si < suggestions.rows ; si++) {
      DoubleMatrix suggestion = suggestions.getRow(si);
      XYSeries series2 = chart.addSeries("Suggestion:" + si, suggestion.toArray(), evaluateRowsWithOF(gpsmboForSuggestions, suggestion).sub(0.2).toArray());
      series2.setMarker(SeriesMarkers.PLUS);

      series2.setMarkerColor(new java.awt.Color( Math.max(255 - si* 3, 0),0, 0));
    }


    BitmapEncoder.saveBitmapWithDPI(chart, "./SMBO_GP_suggestions_based_on_max_improvement_300_DPI", BitmapEncoder.BitmapFormat.PNG, 300);

  }

  /**
   *
   * @param unObservedGridEntries whole grid without points that were evaluated for prior
   * @param alreadyEvaluated matrix with prior evaluations
   * @param suggestions double matrix with suggestions
   * @throws IOException
   */
  static void plotWithVarianceIntervals(DoubleMatrix unObservedGridEntries, DoubleMatrix alreadyEvaluated, DoubleMatrix suggestions, GPSurrogateModel.MeanVariance meanVariance, GPSMBO gpsmboForSuggestions ) throws IOException {
    int numberOfUnObserved = unObservedGridEntries.rows;
    int numberOfObserved = alreadyEvaluated.rows;
    int numberOfMeans = meanVariance.getMean().rows;
    int numberOfVariances = meanVariance.getVariance().rows;

    XYChart chart = new XYChartBuilder().width(1000).height(500)
            .title("SMBO suggestions iteration by iteration with variance intervals. UO = " + numberOfUnObserved + ", O =" + numberOfObserved + ", M = " + numberOfMeans + ", V = " + numberOfVariances)
            .xAxisTitle("X").yAxisTitle("Y").build();

    chart.getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Scatter);
    chart.getStyler().setLegendVisible(false);

    DoubleMatrix means = meanVariance.getMean();
    DoubleMatrix variances = meanVariance.getVariance();
//    chart.addSeries("Varience interval upper bound", unObservedGridEntries.toArray(), YValDM.toArray());
    XYSeries series = chart.addSeries("Evaluated with OF", alreadyEvaluated.getColumn(0).toArray(), alreadyEvaluated.getColumn(1).toArray());
    series.setMarker(SeriesMarkers.DIAMOND);

    XYSeries seriesMeans = chart.addSeries("Predicted means", unObservedGridEntries.toArray(), means.toArray());
    seriesMeans.setMarker(SeriesMarkers.PLUS);

    XYSeries varianceTopBound = chart.addSeries("Variance top bound", unObservedGridEntries.toArray(), means.add(variances).toArray());
    varianceTopBound.setMarker(SeriesMarkers.TRIANGLE_DOWN);
    varianceTopBound.setMarkerColor(Color.GRAY);

    XYSeries varianceBottomBound = chart.addSeries("Variance bottom bound", unObservedGridEntries.toArray(), means.sub(variances).toArray());
    varianceBottomBound.setMarker(SeriesMarkers.TRIANGLE_UP);
    varianceBottomBound.setMarkerColor(Color.DARK_GRAY);

//    for(int si = 0; si < suggestions.rows ; si++) {
//      DoubleMatrix suggestion = suggestions.getRow(si);
//      XYSeries series2 = chart.addSeries("Suggestion:" + si, suggestion.toArray(), evaluateRowsWithOF(gpsmboForSuggestions, suggestion).sub(0.2).toArray());
//      series2.setMarker(SeriesMarkers.PLUS);
//
//      series2.setMarkerColor(new java.awt.Color( Math.max(255 - si* 3, 0),0, 0));
//    }

//    new XChartPanel()

    BitmapEncoder.saveBitmapWithDPI(chart, "./SMBO_GP_suggestions_with_variances_300_DPI_" + alreadyEvaluated.rows, BitmapEncoder.BitmapFormat.PNG, 300);

  }

  //TODO duplicate
  static private DoubleMatrix evaluateRowsWithOF(GPSMBO gpsmbo, DoubleMatrix unObservedGridEntries) {
    DoubleMatrix YValDM = null;
    for(DoubleMatrix row :unObservedGridEntries.rowsAsList()) {
      DoubleMatrix evaluationDM = gpsmbo.objectiveFunction(row);
      if(YValDM == null) {
        YValDM = evaluationDM;
      } else {
        YValDM = DoubleMatrix.concatVertically(YValDM, evaluationDM);
      }
    }
    return YValDM;
  }
}
