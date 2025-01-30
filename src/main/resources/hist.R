histogram <- function(vector, numberOfBreaks, title) {
  hist(vector, breaks = numberOfBreaks, main = title)
};
simpleHistogram <- function(vector) {
  hist(vector)
}