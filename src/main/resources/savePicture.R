savePicture <- function(){
  statistics<-read.table("Results\\df.csv",header=TRUE,sep = ",");
  msk<-order(statistics$TrueValue);
  statistics<-statistics[msk,];

  tiff("Results\\pic.tif",units="in", width=7, height=10, res=300 );
  par(mfrow=c(4,1));
  plot(statistics$TrueValue,statistics$chevPropability,xlab = "Target value",ylab = "Chebyshev probability",col="black", lwd=1);
  plot(statistics$TrueValue,statistics$chevK,xlab = "Target value",ylab = "K",col="black", lwd=1);
  plot(statistics$TrueValue,statistics$phiiRon,xlab = "Target value",ylab = expression(phi),col="black", lwd=1);
  grid( col = "lightgray", lty = "dotted",lwd = par("lwd"), equilogs = TRUE);
  boxplot(statistics$TrueValue,
          main = "",
          xlab = "BoxPlot of Target Value",
          ylab = "",
          col = "orange",
          border = "brown",
          horizontal = TRUE,
          notch = TRUE
  );
  dev.off;
  graphics.off();
}