
data <- read.table("../output/gdipattern_files/gdipatterns.txt");
colnames(data)[1] <- "pop";
colnames(data)[2] <- "light";
colnames(data)[3] <- "gdi";

summary(data)

library(neuralnet);
data$rich = round(data$gdi) > 2.5;
nn1 <- neuralnet(rich ~ pop+light, data=data, hidden=2, threshold=0.1, err.fct="ce", act.fct="logistic", linear.output=FALSE);
plot(nn1);

nn2 <- neuralnet(gdi ~ pop+light, data=data, hidden=3, err.fct="sse", threshold=0.5, act.fct="logistic", linear.output=TRUE);
plot(nn2);

library(nnet);
#nn2 <- nnet(gdi ~ pop + light, data=data, size=2, entropy=FALSE, linout=TRUE );
#summary(nn2)
data$classes = class.ind( round(data$gdi) );
nn3 <- nnet(classes ~ pop + light, data=data, size=2, entropy=TRUE );
summary(nn3)
