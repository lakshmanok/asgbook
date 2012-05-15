data <- read.table("../output/citycategories_files/citydata.txt", header=TRUE);
summary(data)

means <- colMeans(data);
sd  <- apply(data, 2, sd);

data <- scale(data); # normalize
summary(data);

model <- kmeans(data, 3); # 3 clusters
print(model$centers)
print(means)
print(sd)

# unscale the values
centers <- model$centers
for (i in 1:length(means)){
   centers[,i] = centers[,i] * sd[i] + means[i]
}
print(centers)

# plot cluster result
png("cluster1.png");
library(cluster);
clusplot(data, model$cluster, color=TRUE, shade=TRUE, lines=0);


# find the best value of K
icv <- (nrow(data)-1)*sum(apply(data,2,var)) # for K=1
for (K in 2:15){
  icv[K] <- sum(kmeans(data, centers=K)$withinss)
}
png("cluster2.png")
plot(1:15, icv, type="b", xlab="Number of Clusters", ylab="Intra-cluster variance")
