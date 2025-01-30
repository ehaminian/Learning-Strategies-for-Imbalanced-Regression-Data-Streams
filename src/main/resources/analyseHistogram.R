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

analyseHistogram <- function(){
  data <- read.csv("C:\\Users\\Ehsan\\Desktop\\git\\eea1\\Results\\df.csv", header = TRUE, sep = ",");
  trueValues <- data$TrueValue;
  basePredictionValues <- data$BasePrediction;
  histPredictionValuesUnder <- data$ExperimentPredictionHistUNDER;
  histPredictionValuesOver <- data$ExperimentPredictionHistOVER;
  relevancyValues <- data$relevancyValue;

  phi_threshold <- 0.9;
  baseRmsePhi <- rmse_phi(trueValues, basePredictionValues, relevancyValues, phi_threshold);
  histRmsePhiUnder <- rmse_phi(trueValues, histPredictionValuesUnder, relevancyValues, phi_threshold);
  histRmsePhiOver <- rmse_phi(trueValues, histPredictionValuesOver, relevancyValues, phi_threshold);

  baseRmse <- rmse_normal(trueValues, basePredictionValues);
  histRmseUnder <- rmse_normal(trueValues, histPredictionValuesUnder);
  histRmseOver <- rmse_normal(trueValues, histPredictionValuesOver);

  baseSera <- sera(trueValues, basePredictionValues, relevancyValues);
  histSeraUnder <- sera(trueValues, histPredictionValuesUnder, relevancyValues);
  histSeraOver <- sera(trueValues, histPredictionValuesOver, relevancyValues);

  dataFrame <- data.frame(baseRmsePhi, histRmsePhiUnder, histRmsePhiOver, baseRmse, histRmseUnder, histRmseOver, baseSera, histSeraUnder, histSeraOver);
  colnames(dataFrame) <- c("baseRmsePhi", "histRmsePhiUnder", "histRmsePhiOver", "baseRmse", "histRmseUnder", "histRmseOver", "baseSera", "histSeraUnder", "histSeraOver");
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