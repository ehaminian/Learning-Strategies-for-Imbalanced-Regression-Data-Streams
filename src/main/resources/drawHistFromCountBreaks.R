drawHistFromCountBreaks <- function(counts,breaks) {
  break_widths <- diff(breaks);
  png("Results\\barplot.png", width = 1600, height = 800);
  bp <- barplot(counts, width = break_widths, space = 0, xlab = "Breaks", ylab = "Counts", main = "Histogram");
  bp <- c(bp, max(breaks) + (breaks[2] - breaks[1]));
  axis(1, at = bp, labels = breaks) ;
  dev.off();
}