histogramFromFrequencies <- function(breaks, counts) {
  myhist <-list(breaks=breaks, counts=counts, density=counts/diff(breaks), xname="Overall Cond");
  class(myhist) <- "histogram";
  plot(myhist)
}