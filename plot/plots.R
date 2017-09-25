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

options("width"=200)



plot_linear_charts <- function (data) {

    title = 'Benchmark ML using GLM and XGBoost Linear'

    grouped_by_uc_model = data %>% unite(UC, ROWS, COLUMNS, sep = "x") %>%
                                   mutate(UC = factor(UC, levels=c("100x100", "500x100", "1000x100", "100x500", "500x500", "1000x500", "100x1000", "500x1000", "1000x1000"), 
                                                          labels=c("UC 1 (100x100)", "UC 2 (500x100)", "UC 3 (1000x100)", "UC 4 (100x500)", "UC 5 (500x500)", "UC 6 (1000x500)", "UC 7 (100x1000)", "UC 8 (500x1000)", "UC 9 (1000x1000)"))) %>%
                                   group_by(UC, MODEL) %>%
                                   summarize(MEAN_TIME_MILLIS = mean(TIME_MILLIS),
                                             SE_TIME_MILLIS = 1.96 * sd(TIME_MILLIS) / sqrt(n())) %>%
                                   arrange(MODEL)

    ggplot(grouped_by_uc_model, aes(x = as.factor(MODEL), y = MEAN_TIME_MILLIS, color = MODEL, fill = MODEL)) +
        ggtitle(title) + 
        ylab("Time (ms)") +
        geom_bar(stat="identity") +
        geom_errorbar(aes(ymin = MEAN_TIME_MILLIS-SE_TIME_MILLIS, ymax = MEAN_TIME_MILLIS+SE_TIME_MILLIS), width = 0.2) +
        facet_wrap(~UC) +
        theme(legend.position = "bottom", legend.title = element_blank(), axis.title.x = element_blank(), axis.text.x = element_blank())
    ggsave("linear_time_by_model_facet_uc.pdf")

    ggplot(grouped_by_uc_model, aes(x = as.factor(UC), y = MEAN_TIME_MILLIS, color = MODEL, fill = MODEL)) +
        ggtitle(title) + 
        ylab("Time (ms)") +
        geom_bar(stat="identity") +
        geom_errorbar(aes(ymin = MEAN_TIME_MILLIS-SE_TIME_MILLIS, ymax = MEAN_TIME_MILLIS+SE_TIME_MILLIS), width = 0.2) +
        facet_wrap(~MODEL) +
        theme(legend.position = "bottom", legend.title = element_blank(), axis.title.x = element_blank())
    ggsave("linear_time_by_uc_facet_model.pdf")

    grouped_by_uc_model %>%
    filter(UC == "UC 9 (1000x1000)", MODEL %in% c(" XGB-Predictor (xgbLinear)", " XGBoost4J (xgbLinear)")) %>%
    ggplot(., aes(x = as.factor(MODEL), y = MEAN_TIME_MILLIS, color = MODEL, fill = MODEL)) +
        ylab("Time (ms)") +
        ggtitle(title) + 
        geom_bar(stat="identity") +
        geom_errorbar(aes(ymin = MEAN_TIME_MILLIS-SE_TIME_MILLIS, ymax = MEAN_TIME_MILLIS+SE_TIME_MILLIS), width = 0.2) +
        theme(legend.position = "bottom", legend.title = element_blank(), axis.title.x = element_blank(), axis.text.x = element_blank())
    ggsave("linear_zoom_uc9_linear.pdf")

    grouped_by_uc_model %>%
    filter(UC %in% c("UC 1 (100x100)", "UC 2 (500x100)", "UC 3 (1000x100)")) %>%
    ggplot(., aes(x = as.factor(MODEL), y = MEAN_TIME_MILLIS, color = MODEL, fill = MODEL)) +
        ylab("Time (ms)") +
        ggtitle(title) + 
        geom_bar(stat="identity") +
        geom_errorbar(aes(ymin = MEAN_TIME_MILLIS-SE_TIME_MILLIS, ymax = MEAN_TIME_MILLIS+SE_TIME_MILLIS), width = 0.2) +
        theme(legend.position = "bottom", legend.title = element_blank(), axis.title.x = element_blank(), axis.text.x = element_blank()) +
        facet_wrap(~UC)
    ggsave("linear_zoom_uc123_linear.pdf")

}

plot_boosting_charts <- function (data) {

    title = 'Benchmark ML using XGBoost'

    grouped_by_uc_model = data %>% unite(UC, ROWS, COLUMNS, sep = "x") %>%
                                   mutate(UC = factor(UC, levels=c("100x100", "500x100", "1000x100", "100x500", "500x500", "1000x500", "100x1000", "500x1000", "1000x1000"), 
                                                          labels=c("UC 1 (100x100)", "UC 2 (500x100)", "UC 3 (1000x100)", "UC 4 (100x500)", "UC 5 (500x500)", "UC 6 (1000x500)", "UC 7 (100x1000)", "UC 8 (500x1000)", "UC 9 (1000x1000)"))) %>%
                                   group_by(UC, MODEL) %>%
                                   summarize(MEAN_TIME_MILLIS = mean(TIME_MILLIS),
                                             SE_TIME_MILLIS = 1.96 * sd(TIME_MILLIS) / sqrt(n())) %>%
                                   arrange(MODEL)

    # grouped_by_uc_model %>% filter(MODEL %in% c(" XGB-Predictor (xgbTree)"))

    ggplot(grouped_by_uc_model, aes(x = as.factor(MODEL), y = MEAN_TIME_MILLIS, color = MODEL, fill = MODEL)) +
        ggtitle(title) + 
        ylab("Time (ms)") +
        geom_bar(stat="identity") +
        geom_errorbar(aes(ymin = MEAN_TIME_MILLIS-SE_TIME_MILLIS, ymax = MEAN_TIME_MILLIS+SE_TIME_MILLIS), width = 0.2) +
        facet_wrap(~UC) +
        theme(legend.position = "bottom", legend.title = element_blank(), axis.title.x = element_blank(), axis.text.x = element_blank())
    ggsave("boosting_time_by_model_facet_uc.pdf")

    ggplot(grouped_by_uc_model, aes(x = as.factor(UC), y = MEAN_TIME_MILLIS, color = MODEL, fill = MODEL)) +
        ggtitle(title) + 
        ylab("Time (ms)") +
        geom_bar(stat="identity") +
        geom_errorbar(aes(ymin = MEAN_TIME_MILLIS-SE_TIME_MILLIS, ymax = MEAN_TIME_MILLIS+SE_TIME_MILLIS), width = 0.2) +
        facet_wrap(~MODEL) +
        theme(legend.position = "bottom", legend.title = element_blank(), axis.title.x = element_blank())
    ggsave("boosting_time_by_uc_facet_model.pdf")

    grouped_by_uc_model %>%
    filter(UC == "UC 9 (1000x1000)", MODEL %in% c(" RJava (GBM)", " XGBoost4J (xgbTree)")) %>%
    ggplot(., aes(x = as.factor(MODEL), y = MEAN_TIME_MILLIS, color = MODEL, fill = MODEL)) +
        ylab("Time (ms)") +
        ggtitle(title) + 
        geom_bar(stat="identity") +
        geom_errorbar(aes(ymin = MEAN_TIME_MILLIS-SE_TIME_MILLIS, ymax = MEAN_TIME_MILLIS+SE_TIME_MILLIS), width = 0.2) +
        theme(legend.position = "bottom", legend.title = element_blank(), axis.title.x = element_blank(), axis.text.x = element_blank())
    ggsave("boosting_zoom_uc9_boosting.pdf")

    # grouped_by_uc_model %>%
    # filter(UC %in% c("UC 1 (100x100)", "UC 2 (500x100)", "UC 3 (1000x100)")) %>%
    # ggplot(., aes(x = as.factor(MODEL), y = MEAN_TIME_MILLIS, color = MODEL, fill = MODEL)) +
    #     ylab("Time (ms)") +
    #     ggtitle(title) + 
    #     geom_bar(stat="identity") +
    #     geom_errorbar(aes(ymin = MEAN_TIME_MILLIS-SE_TIME_MILLIS, ymax = MEAN_TIME_MILLIS+SE_TIME_MILLIS), width = 0.2) +
    #     theme(legend.position = "bottom", legend.title = element_blank(), axis.title.x = element_blank(), axis.text.x = element_blank()) +
    #     facet_wrap(~UC)
    # ggsave("boosting_zoom_uc123_linear.pdf")

}

linear_data = read.csv("results_linear.csv", col.names = c('ROWS', 'COLUMNS', 'TIME_MILLIS', 'MODEL'), header = FALSE)
plot_linear_charts(linear_data)

boosting_data = read.csv("results_boosting.csv", col.names = c('ROWS', 'COLUMNS', 'TIME_MILLIS', 'MODEL'), header = FALSE)
plot_boosting_charts(boosting_data)