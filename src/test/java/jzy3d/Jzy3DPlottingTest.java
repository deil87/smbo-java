package jzy3d;

import org.jzy3d.analysis.AbstractAnalysis;
import org.jzy3d.analysis.AnalysisLauncher;
import org.jzy3d.chart.factories.AWTChartComponentFactory;
import org.jzy3d.colors.Color;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.plot3d.primitives.Scatter;
import org.jzy3d.plot3d.rendering.canvas.Quality;

import java.util.Random;

public class Jzy3DPlottingTest extends AbstractAnalysis{
  public static void main(String[] args) throws Exception {
    AnalysisLauncher.open(new Jzy3DPlottingTest());
  }

  @Override
  public void init(){
    int size = 500000;
    float x;
    float y;
    float z;
    float a;

    Coord3d[] points = new Coord3d[size];
    Color[]   colors = new Color[size];
//    float[]   widths = new float[size];

    Random r = new Random();
    r.setSeed(0);

    for(int i=0; i<size; i++){
      x = r.nextFloat() - 0.5f;
      y = r.nextFloat() - 0.5f;
      z = r.nextFloat() - 0.5f;
      points[i] = new Coord3d(x, y, z);
      a = 0.25f;
      colors[i] = new Color(x, y, z, a);
    }

    Scatter scatter = new Scatter(points, colors, 3);
    chart = AWTChartComponentFactory.chart(Quality.Advanced, "newt");
    chart.getScene().add(scatter);
  }
}

//public class Jzy3DPlottingTest {
//
//
//  @Test
//  public void basic() throws IOException, InterruptedException {
//    int size = 5000;
//    float x;
//    float y;
//    float z;
//    float a;
//
//    Coord3d[] points = new Coord3d[size];
//    Color[]   colors = new Color[size];
//
//    Random r = new Random();
//    r.setSeed(0);
//
//    for(int i=0; i<size; i++){
//      x = r.nextFloat() - 0.5f;
//      y = r.nextFloat() - 0.5f;
//      z = r.nextFloat() - 0.5f;
//      points[i] = new Coord3d(x, y, z);
//      a = 0.25f;
//      colors[i] = new Color(x, y, z, a);
//    }
//
//    Scatter scatter = new Scatter(points, colors);
//    Chart chart = AWTChartComponentFactory.chart(Quality.Advanced, "newt");
//    chart.getScene().add(scatter);
////    ChartLauncher.openChart(chart);
//
////    Thread.sleep(50000);
//
//    File temp = new File(System.getProperty("java.io.tmpdir"), "screenshot.png");
//    chart.screenshot(temp);
//    BufferedImage img = ImageIO.read(temp);
//    temp.delete();
//  }
//}
