rmse <- function(trues, preds) {
  return(Metrics::rmse(actual = trues, predicted=preds))
};

rmse_normal <- function(trueValues, predictionValues) {
  return(format(round(rmse(trues = trueValues, preds=predictionValues),2),nsmall = 2))
};

rmse_phi <- function(trueValues, predictionValues, relevancyValues, threshold) {
  msk <- relevancyValues >= threshold;
  trueValuesMsk <- trueValues[msk];
  predictionValuesMsk <- predictionValues[msk];
  relevancyValuesMsk <- relevancyValues[msk];
  d <- trueValuesMsk - predictionValuesMsk;
  m <- d * relevancyValuesMsk;
  r <- sqrt(mean(m ^ 2));
  return(format(round(r,2),nsmall = 2))
};

analyse <- function(type,median) {
  statistics<-read.table("Results/df.csv",header=TRUE,sep = ",");

  if(type=="r"){
    msk<-statistics$TrueValue>=median;
    statistics<-statistics[msk,];
  }else if(type=="l"){
    msk<-statistics$TrueValue<=median;
    statistics<-statistics[msk,];
  };

  msk<-order(statistics$TrueValue);
  statistics<-statistics[msk,];

  errorNormalBase_ALL<- rmse_normal(statistics$TrueValue,statistics$BasePrediction);
  errorNormalUnde_ALL<-rmse_normal(statistics$TrueValue,statistics$ExperimentPredictionUNDER);
  errorNormalOver_ALL<-rmse_normal(statistics$TrueValue,statistics$ExperimentPredictionOVER);
  errorPhiBase_ALL<-rmse_phi(statistics$TrueValue, statistics$BasePrediction,statistics$phiiRon, 0.0);
  errorPhiUnder_ALL<-rmse_phi(statistics$TrueValue, statistics$ExperimentPredictionUNDER,statistics$phiiRon, 0.0);
  errorPhiOver_ALL<-rmse_phi(statistics$TrueValue, statistics$ExperimentPredictionOVER,statistics$phiiRon, 0.0);


  phiThreshold<-1;
  msk<-statistics$phiiRon>=phiThreshold;
  statistics_1<-statistics[msk,];

  errorNormalBase_1<-rmse_normal(trueValues = statistics_1$TrueValue, predictionValues = statistics_1$BasePrediction);
  errorNormalUnder_1<-rmse_normal(trueValues = statistics_1$TrueValue, predictionValues = statistics_1$ExperimentPredictionUNDER);
  errorNormalOver_1<-rmse_normal(trueValues = statistics_1$TrueValue, predictionValues = statistics_1$ExperimentPredictionOVER);
  errorPhiBase_1<-rmse_phi(trueValues = statistics_1$TrueValue, predictionValues = statistics_1$BasePrediction,relevancyValues = statistics_1$phiiRon, threshold = phiThreshold);
  errorPhiUnder_1<-rmse_phi(trueValues = statistics_1$TrueValue, predictionValues = statistics_1$ExperimentPredictionUNDER,relevancyValues = statistics_1$phiiRon, threshold = phiThreshold);
  errorPhiOver_1<-rmse_phi(trueValues = statistics_1$TrueValue, predictionValues = statistics_1$ExperimentPredictionOVER,relevancyValues = statistics_1$phiiRon, threshold = phiThreshold);


  phiThreshold<-0.9;
  msk<-statistics$phiiRon>=phiThreshold;
  statistics_09<-statistics[msk,];

  errorNormalBase_09<-rmse_normal(trueValues = statistics_09$TrueValue, predictionValues = statistics_09$BasePrediction);
  errorNormalUnder_09<-rmse_normal(trueValues = statistics_09$TrueValue, predictionValues = statistics_09$ExperimentPredictionUNDER);
  errorNormalOver_09<-rmse_normal(trueValues = statistics_09$TrueValue, predictionValues = statistics_09$ExperimentPredictionOVER);
  errorPhiBase_09<-rmse_phi(trueValues = statistics_09$TrueValue, predictionValues = statistics_09$BasePrediction,relevancyValues = statistics_09$phiiRon, threshold = phiThreshold);
  errorPhiUnder_09<-rmse_phi(trueValues = statistics_09$TrueValue, predictionValues = statistics_09$ExperimentPredictionUNDER,relevancyValues = statistics_09$phiiRon, threshold = phiThreshold);
  errorPhiOver_09<-rmse_phi(trueValues = statistics_09$TrueValue, predictionValues = statistics_09$ExperimentPredictionOVER,relevancyValues = statistics_09$phiiRon, threshold = phiThreshold);



  phiThreshold<-0.8;
  msk<-statistics$phiiRon>=phiThreshold;
  statistics<-statistics[msk,];

  errorNormalBase<-rmse_normal(trueValues = statistics$TrueValue, predictionValues = statistics$BasePrediction);
  errorNormalUnder<-rmse_normal(trueValues = statistics$TrueValue, predictionValues = statistics$ExperimentPredictionUNDER);
  errorNormalOver<-rmse_normal(trueValues = statistics$TrueValue, predictionValues = statistics$ExperimentPredictionOVER);
  errorPhiBase<-rmse_phi(trueValues = statistics$TrueValue, predictionValues=statistics$BasePrediction,relevancyValues = statistics$phiiRon, threshold = phiThreshold);
  errorPhiUnder<-rmse_phi(trueValues = statistics$TrueValue, predictionValues=statistics$ExperimentPredictionUNDER,relevancyValues = statistics$phiiRon, threshold = phiThreshold);
  errorPhiOver<-rmse_phi(trueValues = statistics$TrueValue, predictionValues=statistics$ExperimentPredictionOVER,relevancyValues = statistics$phiiRon, threshold = phiThreshold);

  df <- data.frame(v1=errorNormalBase, v2=errorNormalUnder, v3=errorNormalOver, v4=errorPhiBase, v5=errorPhiUnder, v6=errorPhiOver,
                   v13=errorNormalBase_09,v14=errorNormalUnder_09,v15=errorNormalOver_09,v16=errorPhiBase_09,v17=errorPhiUnder_09,v18=errorPhiOver_09,
                   v19=errorNormalBase_1,v20=errorNormalUnder_1,v21=errorNormalOver_1,v22=errorPhiBase_1,v23=errorPhiUnder_1,v24=errorPhiOver_1,
                   v7=errorNormalBase_ALL,v8=errorNormalUnde_ALL,v9=errorNormalOver_ALL,v10=errorPhiBase_ALL,v11=errorPhiUnder_ALL,v12=errorPhiOver_ALL);
  write.table(df, "./Results/results.csv", sep = ",",  append = T,row.names=F,col.names=F );
  k<-c(errorNormalBase, errorNormalUnder, errorNormalOver, errorPhiBase, errorPhiUnder, errorPhiOver, errorNormalBase_ALL, errorNormalUnde_ALL,errorNormalOver_ALL,errorPhiBase_ALL,errorPhiUnder_ALL,errorPhiOver_ALL);
  k
}

