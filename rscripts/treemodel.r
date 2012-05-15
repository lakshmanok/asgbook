
data <- read.table("../output/gdipattern_files/gdipatterns.txt");
colnames(data)[1] <- "pop";
colnames(data)[2] <- "light";
colnames(data)[3] <- "gdi";
#attach(data)

summary(data)

library(rpart);

data$gdi = round(data$gdi)
model <- rpart(gdi ~ pop + light, method="class", data=data)
printcp(model)

png("treemodel1_cv.png");
plotcp(model)

post(model, file="treemodel1.ps", title="Full tree");
text(model, use.n=TRUE, all=TRUE, cex=0.8);

# prune the tree
model <- prune(model, cp=0.04);
post(model, file="treemodel2.ps", title="Pruned tree");
text(model, use.n=TRUE, all=TRUE, cex=0.8);

