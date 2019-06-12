package smbo;

import org.jblas.DoubleMatrix;
import org.jblas.MatrixFunctions;
import org.junit.Test;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.QuickChart;
import org.knowm.xchart.XYChart;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;

public class GPSurrogateModelSinBenchmark {


  @Test
  public void mathFunctions() throws IOException {
//    NormalDistribution normalDistribution = new NormalDistribution(5, 1);
//    normalDistribution.reseedRandomGenerator(1234);

//    double[] sample = normalDistribution.sample(sampleSize);

    int size = 200;
    double[] xVar = new double[size*10];
    int i;
    for (i = 0; i < size*10; i++) {
      xVar[i] = (double) i / 10;
    }

    DoubleMatrix sampleDM = new DoubleMatrix(xVar);

    DoubleMatrix yDM = MatrixFunctions.sin(sampleDM.div(2.5));

    assertEquals(Math.sin(sampleDM.get(0, 0)), yDM.get(0, 0), 1e-5);

    DoubleMatrix resY = yDM.mul(5);
    // Create Chart
    XYChart chart = QuickChart.getChart("Sample Chart", "X", "Y", "y(x)", xVar, resY.toArray());

    // Show it
//    new SwingWrapper(chart).displayChart();
    // Save it
//    BitmapEncoder.saveBitmap(chart, "./Sample_Chart", BitmapEncoder.BitmapFormat.PNG);
//
//     or save it in high-res
    BitmapEncoder.saveBitmapWithDPI(chart, "./Sample_Chart_300_DPI", BitmapEncoder.BitmapFormat.PNG, 300);

  }



  public EvaluatedGE convertToMtx(ArrayList<EvaluatedGridEntry> evaluatedGridEntries) {

    double[] gridEntries = new double[evaluatedGridEntries.size()];
    double[] gridEntriesResponseValues = new double[evaluatedGridEntries.size()];
    int i = 0;
    for(EvaluatedGridEntry entry: evaluatedGridEntries ) {
      gridEntries[i] = (double) entry._entry.getEntry().get("X");
      gridEntriesResponseValues[i] = entry.evaluatedRes;
      i++;
    }
    return new EvaluatedGE(new DoubleMatrix(gridEntries), new DoubleMatrix(gridEntriesResponseValues));
  }

  @Test
  public void functionApproximationWithGP() throws IOException , SMBO.SMBOSearchCompleted {

    long seed = 1234;
    int size = 200;
    Double[] hyperParametersGrid = new Double[size*10];
    int i;
    for (i = 0; i < size*10; i++) {
      hyperParametersGrid[i] = (double) i / 10;
    }

    HashMap<String, Object[]> grid = new HashMap<>();
    grid.put("X", hyperParametersGrid);

    GPSMBO gpsmbo = new GPSMBO(grid, true, 1234);

      DoubleMatrix nextBestHyperparameters = gpsmbo.getNextBestCandidateForEvaluation();

  }


}