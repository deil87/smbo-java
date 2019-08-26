import pandas as pd
import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d import Axes3D
import matplotlib as mpl
import numpy as np
import seaborn as sns
%matplotlib inline

data = pd.read_csv("H2O_vs_SMBO_MeanVariance.csv", names = ["attemptIdx", "learning_rate", "max_depth", "ntrees", "mean", "variance", "response"])

groups = data.groupby("attemptIdx")
nplots = len(groups)
for k, groupIndex in groups:
  #     print(k)
  #     print(groupIndex)

  if(k % 3 == 0):
    xs = groupIndex['learning_rate']
    ys = groupIndex['max_depth']
    zs = groupIndex['mean']

    data_points = [(x, y, z) for x, y, z in zip(xs, ys, zs)]
    #     gridParams = '1' + str(nplots) + '1'

    ss = list(groupIndex['variance'])
    colors = ['yellow' if wt == 30 else 'red' if wt == 40 else 'maroon' if wt == 50 else 'black' for wt in list(groupIndex['ntrees'])]

    fig = plt.figure(figsize=(8, 6))
    ax = fig.add_subplot(111, projection='3d')

    for data, color, size in zip(data_points, colors, ss):
      x, y, z = data
      ax.scatter(x, y, z, alpha=0.4, c=color, edgecolors='none', s=size*200)

    ax.set_xlabel('Learning rate')
    ax.set_ylabel('Max depth')
    ax.set_zlabel('Predicted mean')