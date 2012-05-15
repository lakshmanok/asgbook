data <- read.table("../output/gdipattern_files/gdipatterns.txt");
summary(data)

cor(x=data$V1,y=data$V3,method="pearson")
cor(x=log(data$V1),y=data$V3,method="pearson")
cor(x=data$V2,y=data$V3,method="pearson")

cor(x=data$V1,y=data$V3,method="kendall")
cor(x=data$V2,y=data$V3,method="kendall")

model <- glm( data$V3 ~ data$V2 + data$V1 );
summary(model)
png(filename="linearmodelfit.png");
layout(matrix(c(1,2,3,4),2,2)); plot(model)

library("boot")
cverr <- suppressWarnings(cv.glm(data, model, K=5)); # 3 fold cross-validation
cverr$delta

est = round( 0.003494 + 0.034444 * data$V2 - 0.005992 * data$V1 )
summary( (est-data$V3)^2 )

# use only one variable
model <- glm( data$V3 ~ data$V2 );
summary(model);
cverr <- suppressWarnings(cv.glm(data, model, K=5)); # 3 fold cross-validation
cverr$delta


#frac = data$V2/data$V1
#cor(x=frac,y=data$V3,method="pearson")
#model <- lm( data$V3 ~ frac + data$V1 );
#summary(model)
#est = round( 0.361739 + 0.649456 * (data$V2/data$V1) - 0.005510 * data$V1 )
#summary( (est-data$V3) )
