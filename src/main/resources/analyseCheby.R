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
sera <- function(trueValues, predictionValues, relevancyValues) {
  seraVect <- IRon::sera(
    trues = trueValues,
    preds = predictionValues,
    phi.trues = relevancyValues,
    ph = NULL,
    pl = FALSE,
    m.name = "Model",
    step = 0.001,
    return.err = FALSE,
    norm = TRUE
  );
  return(format(round(seraVect,3),nsmall = 2))
};
analyseCheby <- function() {
  data <- read.csv("C:\\Users\\Ehsan\\Desktop\\git\\eea1\\Results\\df.csv", header = TRUE, sep = ",");
  mskOrder<-order(data$TrueValue);
  data<-data[mskOrder,];

  baseRmsePhi<-rmse_phi(trueValues = data$TrueValue, predictionValues = data$BasePrediction, relevancyValues = data$relevancyValue, threshold = 0.9);
  chebyRmsePhiUnder<-rmse_phi(trueValues = data$TrueValue, predictionValues = data$ExperimentPredictionUNDER, relevancyValues = data$relevancyValue, threshold = 0.9);
  chebyRmsePhiOver<-rmse_phi(trueValues = data$TrueValue, predictionValues = data$ExperimentPredictionOVER, relevancyValues = data$relevancyValue, threshold = 0.9);

  baseRmse<-rmse_normal(trueValues = data$TrueValue, predictionValues = data$BasePrediction);
  chebyRmseUnder<-rmse_normal(trueValues = data$TrueValue, predictionValues = data$ExperimentPredictionUNDER);
  chebyRmseOver<-rmse_normal(trueValues = data$TrueValue, predictionValues = data$ExperimentPredictionOVER);

  baseSera<-sera(trueValues = data$TrueValue, predictionValues = data$BasePrediction, relevancyValues = data$relevancyValue);
  chebySeraUnder<-sera(trueValues = data$TrueValue, predictionValues = data$ExperimentPredictionUNDER, relevancyValues = data$relevancyValue);
  chebySeraOver<-sera(trueValues = data$TrueValue, predictionValues = data$ExperimentPredictionOVER, relevancyValues = data$relevancyValue);


  dataFrame <- data.frame(baseRmsePhi, chebyRmsePhiUnder, chebyRmsePhiOver, baseRmse, chebyRmseUnder, chebyRmseOver, baseSera, chebySeraUnder, chebySeraOver);
  colnames(dataFrame) <- c("baseRmsePhi", "chebyRmsePhiUnder", "chebyRmsePhiOver", "baseRmse", "chebyRmseUnder", "chebyRmseOver", "baseSera", "chebySeraUnder", "chebySeraOver");
  fileName <- "C:\\Users\\Ehsan\\Desktop\\git\\eea1\\Results\\results_1.csv";
  if (file.exists(fileName)) {
    existedDataframe <- tryCatch(
      expr = {
        read.csv(fileName);
      },
      error = function(e) {
        q(status = 2);
      });
    dataFrame <- tryCatch(
      expr = {
        rbind(existedDataframe, dataFrame);
      },
      error = function(e) {
        q(status = 2);
      });
    write.csv(dataFrame,  file = fileName , row.names = FALSE);
  } else {
    write.csv(dataFrame,  file = fileName , row.names = FALSE);
  };
  5
}

