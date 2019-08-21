package smbo;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class AcquisitionFunctionTest {

  @Test
  public void cloneTyped() throws CloneNotSupportedException{

    EI ei = new EI(0.0, 5, true);

    Assert.assertEquals(0.0, ei.getIncumbent(), 1e-5);

    AcquisitionFunction copyOfEI = ei.cloneTyped();
    ei.setIncumbent(100);

    Assert.assertEquals(100.0, ei.getIncumbent(), 1e-5);
    Assert.assertEquals(0.0, copyOfEI.getIncumbent(), 1e-5);
  }
}