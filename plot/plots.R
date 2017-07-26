library(ggplot2)
library(reshape)
library(scales)
library(dplyr)
library(tidyr)

theme_paper <- function() {
  theme_update( panel.background  = element_blank(),
                panel.grid.major  = element_blank(),
                axis.title.x      = element_text(vjust = 0.5),
                axis.title.y      = element_text(angle = 90, vjust = 1),
                axis.text.x       = element_text(angle = 45, hjust=1),  
                plot.margin       = unit(c(1, 1, 1, 1), "lines"),
                plot.title        = element_text(face="bold", hjust = 0.5))
}

theme_set(theme_bw())
theme_paper()

method = "boosting" # boosting
input_file = paste0("results_", method, ".csv")
output_file = paste0("benchmark_", method, ".pdf")

data = read.csv(input_file, col.names = c('ROWS', 'COLUMNS', 'TIME_MILLIS', 'MODEL'), header = FALSE)
data %>% unite(UC, ROWS, COLUMNS, sep = "x") %>%
         mutate(UC = factor(UC, levels=c("100x100", "500x100", "1000x100", "100x500", "500x500", "1000x500", "100x1000", "500x1000", "1000x1000"), 
                                labels=c("UC 1 (100x100)", "UC 2 (500x100)", "UC 3 (1000x100)", "UC 4 (100x500)", "UC 5 (500x500)", "UC 6 (1000x500)", "UC 7 (100x1000)", "UC 8 (500x1000)", "UC 9 (1000x1000)"))) %>%
         group_by(UC, MODEL) %>%
         summarize(MEAN_TIME_MILLIS = mean(TIME_MILLIS),
                   SE_TIME_MILLIS = 1.96 * sd(TIME_MILLIS) / sqrt(n())) %>%
         arrange(MODEL) %>%
    ggplot(., aes(x = as.factor(MODEL), y = MEAN_TIME_MILLIS, color = MODEL, fill = MODEL)) +
        labs(title = "XGBoost Java") +
        xlab("Number of columns") +
        ylab("Time (ms)") +
        ggtitle('Benchmark ML using GLM and XGBoost linear') + 
        geom_bar(stat="identity") +
        geom_errorbar(aes(ymin = MEAN_TIME_MILLIS-SE_TIME_MILLIS, ymax = MEAN_TIME_MILLIS+SE_TIME_MILLIS), width = 0.2) +
        # scale_y_continuous(breaks = seq(0, 150, 15)) +
        facet_wrap(~UC) +
        theme(legend.position = "bottom", legend.title = element_blank(), axis.title.x = element_blank(), axis.text.x = element_blank())
ggsave(output_file)