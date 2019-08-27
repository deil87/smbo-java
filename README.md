# Sequential Model-based Optimisation  [![Build Status](https://travis-ci.org/deil87/smbo-java.svg?branch=master)](https://travis-ci.org/deil87/smbo-java)

This project represents an implementation of the SMBO concept on top of JBlas linear algebra library.

Main idea of the SMBO is to use surrogate model to get fast predictions for all possible yet unobserved points in the hyperspace of parameters.
Ideally we would like to choose surrogate model which will be abble to provide uncertainty levels for its predictions as this can be exploited to tackle exploration/exploitation tradeoff problem.
One of the options that matches these requirements is Gaussian Processes(GP).

## Gaussian processes as a Surrogate model
Great thing about GP is that it supports online learning for free. This will save a lot of time resources as it is supposed that surrogate model will be able to take into account new true observations of objective function after every iteration.

### Kernels
Kernels define the way of how knowledge distributes around the hyperspace. They could be seen as means to encode our assumptions about response surface of a given objective function.
Two kernels are ready to use:
- Squared Exponential Kernel
- Rational Quadratic Kernel

#### Hyperparameters search for optimal kernel
Framework supports grid search(GS) for some free parameters of the kernels. Ability to customise GS is on the roadmap.

### Univariate objective functions

Even though framework supports multivariate objective functions, it is worth to highlight univariate scenario because we will be able to plot performance of the algorithm.
It can be helpful for getting started and for building deeper understanding of the concept.

## Benchmarks
### Random Grid Search vs Sequential Model-Based Optimisation

#### Scenario 1: 65 runs on Titanic data set, grid size = 60

**Objective function:** H2O GBM

**Grid:**

```
    HashMap<String, Object[]> hyperParms = new HashMap<>();

    hyperParms.put("_ntrees", new Integer[]{30, 40, 50});

    hyperParms.put("_max_depth", new Integer[]{1, 5, 2, 7});

    hyperParms.put("_learn_rate", new Float[]{0.01f, 0.1f, 0.3f, 0.4f, 0.5f});
```


We want to compare how fast __**on average**__ algorithms will be able to find best combination of hyper parameters. For the benchmark any framework that supports Random Grid Search will work just fine.
H2O.ai open source library is used for convenience as it is also written in Java. To make benchmark objective we will be doing multiple restarts with different seeds.

![Per iteration predictions for Means and Variances](/images/SMBO_vs_RGS_hist_65_runs.png?raw=true "SMBO vs RGS histogram 65 runs")


In theory, RGS's average index of best attempt should be N / 2, where N is a size of hyper parameters grid.
Below are results of the benchmark:

- SMBO ( prior size = 3 , randomly) :

 average index = 9.076923076923077 out of 60

- RGS:

 average index = 31.784615384615385 out of 60

 SMBO by 31.78 / 9 = 3.52 times was faster than RGS

Note that to make comparison more fair we can exclude first 3 attempts which are random in both cases:

- SMBO:

 average index = 6.557377049180328 out of 57

- RGS:

 average index = 30.258064516129032 out of 57

 SMBO by 30.25 / 6.55 = 4.6 times was faster than RGS

Summary:

- SMBO was able to find best value after exploring just **9 / 60 = 0.15** of the grid instead of **0.5** in RGS case

- SMBO was able to find best value not later than **21 attempt** in all 65 runs. It means that we can consider early stopping without sacrificing performance.

Usage:

```
    // Trade-off between exploration and exploitation
    double tradeoff = 0.0;
    EI acquisitionFun = new EI(tradeoff, true);

    int size = 10;
    Double[] gridEntriesForX = new Double[size*10];
    for (int i = 0; i < size*10; i++) {
      gridEntriesForX[i] = (double) i / 10;
    }

    SortedMap<String, Object[]> grid = Collections.synchronizedSortedMap(new TreeMap());
    grid.put("X", gridEntriesForX);

    int seed = 12345;

    ObjectiveFunction sinOF = new SinOFDefault();

    GPSMBO gpsmbo = new GPSMBO(sinOF, grid, acquisitionFun,true, seed);

    try{
      while (true) {
        DoubleMatrix nextBestCandidate = gpsmbo.getNextBestCandidateForEvaluation();

        DoubleMatrix observedOFValue = gpsmbo.evaluateRowsWithOF( nextBestCandidate);
        DoubleMatrix observedSuggestion = DoubleMatrix.concatHorizontally(nextBestCandidate, observedOFValue);
        gpsmbo.updatePrior(observedSuggestion);
      }
    } catch (SMBO.SMBOSearchCompleted ex) {
      List<Chart> meanVarianceCharts = gpsmbo.getMeanVarianceCharts();
      BitmapEncoder.saveBitmap(meanVarianceCharts, meanVarianceCharts.size() / 2 , 2, "MeanVariance_sin_" + size + "_" + seed, BitmapEncoder.BitmapFormat.PNG);
    }

```

### Multivariate objective functions

    // coming soon

## Random forest as a Surrogate model
    // coming soon

# Examples:
## Sin objective function:

Acquisition function: Expected Improvement

```
public class SinOF extends ObjectiveFunction{

  public EvaluatedGridEntry evaluate(GridEntry entry) {
    Double xVal = (Double)entry.getEntry().get("X");
    double result = Math.sin(xVal * 2.5) * 5 ;
    return new EvaluatedGridEntry(entry, result);
  }
}

```
## Complex periodic objective function:

```
public class ComplexPeriodicOF extends ObjectiveFunction{

  public EvaluatedGridEntry evaluate(GridEntry entry) {
    Double xVal = (Double)entry.getEntry().get("X");
    double secondFun = xVal > 2 ? Math.sqrt(xVal) * 1.25 : Math.sin(xVal + 2) * 2;

    double result = 3 * Math.exp( -xVal)* Math.cos(2 * Math.PI * xVal) + secondFun ;
    return new EvaluatedGridEntry(entry, result);
  }
}
`````

Grid of charts shows predicted means and variances on every iteration of the learning process.

![Per iteration predictions for Means and Variances](/images/MeanVariance_sin_10_12345.png?raw=true "Learning sin function")