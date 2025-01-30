lineGraph <- function(vectorPhi, vectorY) {
  msk <- order(vectorY);vectorPhi <- vectorPhi[msk];vectorY<-vectorY[msk]; plot(vectorY, vectorPhi, col="black");
}