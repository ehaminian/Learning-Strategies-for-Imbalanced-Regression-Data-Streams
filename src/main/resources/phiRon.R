phiIRon <- function(Y) {
  d<-boxplot(Y);
  m1<-min(Y);
  m2<-d$stats[1];
  m3<-d$stats[5];
  m4<-max(Y);
  M <- matrix(0,ncol=3,nrow=0);
  if(m1<m2){
    M <- rbind(M,c(m1,1,0));
    M <- rbind(M,c(m2,1,0));
  }else{
    M <- rbind(M,c(m2,1,0));
  };
  M <- rbind(M,c(d$stats[3],0,0));
  if(m3<m4){
    M <- rbind(M,c(m3,1,0));
    M <- rbind(M,c(m4,1,0));
  }else{
    M <- rbind(M,c(m3,1,0));
  };
  ph <-IRon::phi.control(Y, method="range", control.pts=M);
  IRon::phi(Y,phi.parms=ph);
}