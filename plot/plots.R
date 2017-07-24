library(reshape2)
library(ggplot2)

data = read.csv('~/Desktop/benchmark_ml.tsv', sep='\t', stringsAsFactor=FALSE)
data = melt(data, id.vars = c("Package"), measure.vars= c(paste0("UC",1:9) ))

data$mean = as.numeric(lapply( strsplit(data$value, split=" "), function(x) x[1]))
data$se = as.numeric(lapply( strsplit(data$value, split=" "), function(x) as.numeric(x[3])))

data$variable = factor(data$variable, levels=paste0("UC",c(1,4,7,2,5,8,3,6,9)))

ggplot(data, aes(x=Package, y=mean, color=Package, fill=Package)) + 
  geom_bar(stat="identity" ) +
  geom_errorbar(aes(ymin=mean-se, ymax=mean+se), colour="black", width=.1) +
  facet_wrap(~variable) + 
  ggtitle('Benchmark ML using Boosting Machine')+ 
  ylab('Mean prediction time (ms)')
