# Disaster Loss Estimation Improvements

## Data improvements

1. Highly accurate disaster geometry.\
   We compute analytics used for model training based on disaster geometry, the more accurate geometry - the more accurate analytics and results we get. For example: if we have flood geometry repeating USA borders, we can't expect accurate results. 
2. Specific geometry types. \
   We should separate the geometry of disaster exposure from other types of geometry (alert, some of the severity zones, etc).
3. More analytical parameters.\
   For example, the number of hospitals or fire stations close to the disaster area could be relevant for estimating losses. Also, some disaster-type specific analytics, like types of vegetation in the area could help estimate the loss for a wildfire.
4. Data about the consequences of a disaster.\
   We could analyze satellite data before and after the disaster to get some information about actually affected area of a disaster. Also, some providers or news have data on the number of people affected, injured, and dead as a result of a disaster.
5. Unbiased initial data on losses.\
   We used EM-DAT, which has the loss data only for significant disasters, so the model is biased because it didn't train on minor hazards and can't provide accurate results on average. Also, losses from EM-DAT are not precise (3 Billion, 2Million), they give more like just a context for the number of zeros.

## Methods improvements

1. Use non-linear models.\
   Simple non-linear models are still pretty explainable and may produce better results than the linear approach. More complex models would be less explainable but may work very well.
2. Classification.\
   We could try classification instead of regression to define the loss category, rather than the exact number. For example: under 1M, 1-10M, 10M-100M, and more than 100M.
3. Separate models for each disaster type.\
   Train separate models for each disaster type, using individual approaches and data for training.
4. Location-specific models.\
   Loss caused by the same type of disaster can be different for different locations. We could try to make mini-models for different locations, and not try to come up with a global solution. For example, if we have good coverage of wildfire data for the USA or even just California, we could develop a model that predicts wildfire losses for California. This model will be specific to wildfires in California and it may work worse in other locations.
