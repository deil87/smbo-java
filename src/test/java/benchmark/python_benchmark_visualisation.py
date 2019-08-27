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



data = pd.read_csv("H2O_vs_SMBO_ObservedObjectiveFunction.csv", names = ["learning_rate", "max_depth", "ntrees", "response"])

fig = plt.figure(figsize=(8, 6))
ax = fig.add_subplot(111, projection='3d')

for index, row in data.iterrows():
  xs = row['learning_rate']
  print(xs)
  ys = row['max_depth']
  zs = row['response']

  ntrees = row['ntrees']
  color = 'yellow' if ntrees == 30 else 'red' if ntrees == 40 else 'maroon' if ntrees == 50 else 'black'
  if ntrees == 30:
    color = 'yellow'
  elif ntrees == 40:
    color = 'red'
  elif ntrees == 50:
    color = 'maroon'
  else:
    color = 'black'

  ax.scatter(xs, ys, zs, alpha=0.6, c=color, edgecolors='none', size = 10)

ax.set_xlabel('Learning rate')
ax.set_ylabel('Max depth')
ax.set_zlabel('Real response')



# Surface of response
%matplotlib inline
from matplotlib import cm
data = pd.read_csv("H2O_vs_SMBO_ObservedObjectiveFunction.csv", names = ["learning_rate", "max_depth", "ntrees", "response"])

fig = plt.figure(figsize=(10, 8))
ax = fig.add_subplot(111, projection='3d')

xs = np.ravel(data[['learning_rate']])
print(xs.shape)
ys = np.ravel(data[['max_depth']])
print(ys.shape)
zs = np.ravel(data[['response']])
print(zs.shape)

radii = np.linspace(0.125, 1.0, 5)
print(radii.shape)
#         ntrees = row['ntrees']


#         ax.scatter(xs, ys, zs, alpha=0.6, c=color, edgecolors='none', s=30)

ax = Axes3D(fig)
surf = ax.plot_trisurf(xs, ys, zs, cmap=cm.jet, linewidth=0.1)
# fig.colorbar(surf, shrink=0.5, aspect=5)

ax.set_xlabel('Learning rate')
ax.set_ylabel('Max depth')
ax.set_zlabel('Real response')

plt.show()


#Choosing palette
#URL https://medium.com/@neuralnets/data-visualization-with-python-and-seaborn-part-3-69647c19bf2
sns.palplot(sns.cubehelox_palette(n_colors=8, start=1.7, rot=0.2, dark=0, light=0.95, reverse=True))






# Hue coloring based on index of attempt (not by sorted value of response)
from sklearn import preprocessing
data = pd.read_csv("H2O_vs_SMBO_ObservedObjectiveFunction.csv", names = ["learning_rate", "max_depth", "ntrees", "response"])
# print(type(data[['response']]))
data['response_label'] = data[['response']].apply(lambda row: str(row.response) + "_" + str(row.name), axis=1)
# print(data.head())
# print(data.dtypes)
# print(data.index.values.astype(str))
palette2 = sns.diverging_palette(10, 220, l=70, n=80)
# palette2 = sns.cubehelix_palette(n_colors=80, start=1.7, rot=0.2, dark=0.3, light=0.95, reverse=True)
palette2AsHex = palette2.as_hex()
# print(palette2AsHex)
# print(palette2[50])
respAsStringArr = np.ravel(data[['response_label']])

# print(respAsStringArr)
palette_dict = {}

# for index, respValue in data[['response']].iterrows():
for index, respValue in enumerate(respAsStringArr):
  #     print(str(index) + ": " + str(respValue) + ":" + palette2AsHex[index])
  palette_dict[respValue] = palette2AsHex[index] #We need to add index to response key as thhere could be duplicates

# print(len(palette_dict))
# print(palette_dict)

g = sns.FacetGrid(data, col="ntrees", hue='response_label', aspect = 1.2, hue_order=respAsStringArr, # hue ordered by suggestion index
                  size = 3.5, palette=palette_dict)

sizesFrame = data[['response']]

# https://stats.stackexchange.com/questions/281162/scale-a-number-between-a-range
normalized_sizesFrame=((sizesFrame-sizesFrame.min()) / (sizesFrame.max() - sizesFrame.min())) * (600 - 100) + 100
print(normalized_sizesFrame.head(10))

sizesAsSeries = normalized_sizesFrame['response']
sizesAsList = list(sizesAsSeries)
# normalized_sizes_1D = np.ravel(normalized_sizesFrame)


g.map(plt.scatter, "learning_rate", "max_depth", alpha=0.8,
      edgecolor='white', linewidth=0.5, s=sizesAsList * 4) # size should reflect response abs value
fig = g.fig
fig.subplots_adjust(top=0.8, wspace=0.3)
fig.suptitle('Wine Type - Sulfur Dioxide - Residual Sugar - Alcohol - Quality', fontsize=14)
l = g.add_legend(title='Response')











data = pd.read_csv("H2O_vs_SMBO_ObservedObjectiveFunction.csv", names = ["learning_rate", "max_depth", "ntrees", "response"])
sample_size = 14
data = data.head(sample_size)
sizesFrame = data[['response']]
normalized_sizesFrame=((sizesFrame-sizesFrame.min()) / (sizesFrame.max() - sizesFrame.min())) * (600 - 50) + 50
sizes = list(normalized_sizesFrame['response'])

print(data.head())
# data['quality_label'] = data['response'].apply(lambda value: '0.9778318912237329_0' if value <= 0.98 else ('0.9945414091470952_1' if value <= 0.99 else '0.9999184177997528_2'))
data['quality_label'] = data['response'].apply(lambda value: 'high' if value >= 0.99 else ('medium' if value >= 0.97 else 'low'))


data['response_label'] = data.apply(lambda row: str(row.response) + "_" + str(row.name), axis=1)

########## Pallete
palette2 = sns.diverging_palette(10, 220, l=70, n=len(data))
palette2AsHex = palette2.as_hex()
respAsStringArr = list(np.ravel(data['response_label']))

palette_dict = {}

palette_dict['low'] = '#ee9195'
palette_dict['high'] = '#f1a5a8'
palette_dict['medium'] = '#f4babc'

# for index, respValue in enumerate(respAsStringArr):
#     palette_dict[respValue] = palette2AsHex[index] #We need to add index to response key as thhere could be duplicates


# hue_ordel_list = ['0.9945414091470952_1','0.9778318912237329_0', '0.9999184177997528_2', '0.9934524103831892_3', '0.8957379480840545_4', '0.9999419035846724_5', '0.8957416563658838_6']
hue_ordel_list = ['low','medium', 'high']

print(type(hue_ordel_list))
print("hue_ordel_list:")
print(hue_ordel_list)

print("Comparison:")

print(palette_dict)
print(type(respAsStringArr))
print(respAsStringArr)


sizesFrame = data[['response']]
normalized_sizesFrame=((sizesFrame-sizesFrame.min()) / (sizesFrame.max() - sizesFrame.min())) * (600 - 100) + 100
sizesAsList = np.ravel(normalized_sizesFrame['response'])

sizesAsList = [500,10, 200]

# print(sizesAsList.shape)
print(sizesAsList)
data['marker_size'] = normalized_sizesFrame['response']



print(data.dtypes)
print(data.head(sample_size))

g = sns.FacetGrid(data, col="ntrees", col_order=[30, 40, 50, 100], hue="quality_label", hue_order=hue_ordel_list, size=4)

g.map(plt.scatter, "learning_rate", "max_depth", alpha=0.8,
      edgecolor='white', linewidth=0.5, s=sizesAsList)
fig = g.fig
fig.subplots_adjust(top=0.8, wspace=0.3)
fig.suptitle('Wine Type - Sulfur Dioxide - Residual Sugar - Alcohol - Quality', fontsize=14)
l = g.add_legend(title='Wine Quality Class')


#####   Plotly
import plotly.express as px
gapminder = px.data.gapminder()

print(gapminder.head())
print(data.head())

# fig = px.scatter(gapminder.query("year==2007"), x="gdpPercap", y="lifeExp", size="pop", color="continent",
#                  hover_name="country", log_x=True, size_max=60,  facet_col="continent")
# fig.show()
from plotly.graph_objs import *
layout = Layout(
  paper_bgcolor='rgba(0,0,0,0)',
  plot_bgcolor='rgba(0,0,0,0)'
)

fig = px.scatter(data, x="learning_rate", y="max_depth", size="marker_size", color="suggestion_index",
                 hover_name="response", log_x=True, size_max=10,  facet_col="ntrees")
fig.show()


#Histogram ###########################################
import numpy as np
import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt
from scipy import stats

data = pd.read_csv("H2O_vs_SMBO_assembly_of_best_entries_SMBO.csv", names = ["suggestion_index_for_best_response","learning_rate", "max_depth", "ntrees", "response"])
dataRGS = pd.read_csv("H2O_vs_SMBO_assembly_of_best_entries_RGS.csv", names = ["suggestion_index_for_best_response"])


fig = plt.figure(figsize=(10, 8))
# ax = fig.add_subplot(111, projection='3d')
# print(data.head())
# data['suggestion_index_for_best_response'].hist(bins=100, grid=False, xlabelsize=12, ylabelsize=12)
plt.xlabel("suggestion_index_for_best_response", fontsize=15)
plt.ylabel("Frequency",fontsize=15)

# print(dataRGS.head())

sns.distplot(data['suggestion_index_for_best_response'], kde=False, rug=True, color='blue', bins=range(0, 60, 1), label= "SMBO")
sns.distplot(dataRGS['suggestion_index_for_best_response'], kde=False, rug=True, color='red', bins=range(0, 60, 1), label= "RGS")