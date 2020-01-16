library(ggplot2)
library(reshape)
library(scales)
library(dplyr)
library(tidyr)
library(stringr)
library(purrr)
library(checkmate)

theme_paper <- function() {
    theme_update(
        panel.background  = element_blank(),
        panel.grid.major  = element_blank(),
        axis.title.x      = element_text(vjust = 0.5),
        axis.title.y      = element_text(angle = 90, vjust = 1),
        axis.text.x       = element_text(angle = 45, hjust = 1),
        plot.margin       = unit(c(1, 1, 1, 1), "lines"),
        plot.title        = element_text(face = "bold", hjust = 0.5)
    )
}

theme_set(theme_bw())
theme_paper()

options("width"=200)

EVALUATED_PERCENTILES = c("0.50", "0.90", "0.95", "0.99", "0.999", "0.9999")
EVALUATED_BOOSTER_TYPES = c("TREE", "LINEAR")

predictor_name_from_benchmark <- function (benchmark) {
    # benchmark.Main.benchPMML:benchPMML·p0.00 => "PMML"
    predictor_dot_percentile = str_split_fixed(benchmark, ':', 2)[2]
    return (str_remove(str_split_fixed(predictor_dot_percentile, '·p', 2)[1], "bench"))
}

percentile_from_benchmark <- function (benchmark) {
    # benchmark.Main.benchPMML:benchPMML·p0.99 => 0.99
    predictor_dot_percentile = str_split_fixed(benchmark, ':', 2)[2]
    return (str_split_fixed(predictor_dot_percentile, '·p', 2)[2])
}

plot_per_booster_and_percentile <- function(data, booster_type, at_percentile) {
    assertChoice(booster_type, EVALUATED_BOOSTER_TYPES)
    assertChoice(at_percentile, EVALUATED_PERCENTILES)

    title = sprintf("Benchmark ML using GLM and XGBoost Linear at %sth", at_percentile)

    if (booster_type == "TREE") {
        title = sprintf("Benchmark ML using XGBoost at %sth", at_percentile)
    }

    ntieth_percentile_per_predictor = data %>%
        filter(PERCENTILE == at_percentile) %>%
        filter(BOOSTER_TYPE == booster_type)

    # == Compare use-case-wise for different predictors

    ggplot(ntieth_percentile_per_predictor, aes(x = as.factor(PREDICTOR), y = LATENCY_IN_MS, color = PREDICTOR, fill = PREDICTOR)) +
        ggtitle(title) +
        ylab("Time (ms)") +
        geom_bar(stat = "identity") +
        facet_wrap(~UC) +
        theme(legend.position = "bottom", legend.title = element_blank(), axis.title.x = element_blank(), axis.text.x = element_blank())

    ggsave(sprintf("prediction_time_by_uc_%s_%s.pdf", tolower(booster_type), at_percentile))

    # == Compare model-wise for different percentiles

    ggplot(ntieth_percentile_per_predictor, aes(x = as.factor(UC), y = LATENCY_IN_MS, color = PREDICTOR, fill = PREDICTOR)) +
        ggtitle(title) +
        ylab("Time (ms)") +
        geom_bar(stat="identity") +
        facet_wrap(~PREDICTOR) +
        theme(legend.position = "bottom", legend.title = element_blank(), axis.title.x = element_blank())

    ggsave(sprintf("prediction_time_by_predictor_%s_%s.pdf", tolower(booster_type), at_percentile))

    # == Zoom into use cases 1, 2, and 3 to have an idea how the libraries perform with a hundred samples.

    uc_123 = data %>%
        filter(UC %in% c("UC 1 (100x100)", "UC 2 (500x100)", "UC 3 (1000x100)"))

    ggplot(uc_123, aes(x = as.factor(PREDICTOR), y = LATENCY_IN_MS, color = PREDICTOR, fill = PREDICTOR)) +
        ggtitle(title) +
        ylab("Time (ms)") +
        geom_bar(stat = "identity") +
        facet_wrap(~UC) +
        theme(legend.position = "bottom", legend.title = element_blank(), axis.title.x = element_blank(), axis.text.x = element_blank())

    ggsave(sprintf("zoom_uc_123_%s_%s.pdf", tolower(booster_type), at_percentile))
}

data = read.csv(
    "benchmark-xgboost-java.csv",
    col.names = c(
        "BENCHMARK", "MODE", "THREADS", "SAMPLES", "LATENCY_IN_MS", "LATENCY_IN_MS_ERROR", "UNIT", "BOOSTER_TYPE", "ROWS", "COLUMNS"
    ),
    skip = 1,
    header = FALSE
) %>%
    select(BENCHMARK, LATENCY_IN_MS, LATENCY_IN_MS_ERROR, BOOSTER_TYPE, ROWS, COLUMNS) %>%
    # Extract both percentile and concise name from default JMH class naming
    mutate(PREDICTOR = purrr::pmap_chr(list(BENCHMARK), predictor_name_from_benchmark)) %>%
    mutate(PERCENTILE = purrr::pmap_chr(list(BENCHMARK), percentile_from_benchmark)) %>%
    # BENCHMARK column is not required anymore
    select(-BENCHMARK) %>%
    # Transform ROWS and COLUMNS into a unique factor representing the benchmark use cases
    unite(UC, ROWS, COLUMNS, sep = "x") %>%
    mutate(UC = factor(UC, levels = c("100x100", "500x100", "1000x100", "100x500", "500x500", "1000x500", "100x1000", "500x1000", "1000x1000"),
                labels = c("UC 1 (100x100)", "UC 2 (500x100)", "UC 3 (1000x100)", "UC 4 (100x500)", "UC 5 (500x500)", "UC 6 (1000x500)", "UC 7 (100x1000)", "UC 8 (500x1000)", "UC 9 (1000x1000)")))


# Generate charts for all percentiles and all booster types
for (at_percentile in EVALUATED_PERCENTILES) {
    for (booster_type in EVALUATED_BOOSTER_TYPES) {
        plot_per_booster_and_percentile(data, booster_type, at_percentile)
    }
}

# == For a single chart, e.g., 99th percentile using tree booster

at_percentile = "0.99"
booster_type = "TREE"

ntieth_percentile_per_predictor = data %>%
    filter(PERCENTILE == at_percentile) %>%
    filter(BOOSTER_TYPE == booster_type)

ggplot(ntieth_percentile_per_predictor, aes(x = as.factor(UC), y = LATENCY_IN_MS, color = PREDICTOR, fill = PREDICTOR)) +
    ggtitle(title) +
    ylab("Time (ms)") +
    geom_bar(stat="identity") +
    facet_wrap(~PREDICTOR) +
    theme(legend.position = "bottom", legend.title = element_blank(), axis.title.x = element_blank())

ggsave(sprintf("prediction_time_by_predictor_%s_%s.pdf", tolower(booster_type), at_percentile))

ggplot(ntieth_percentile_per_predictor, aes(x = as.factor(PREDICTOR), y = LATENCY_IN_MS, color = PREDICTOR, fill = PREDICTOR)) +
    ggtitle(title) +
    ylab("Time (ms)") +
    geom_bar(stat = "identity") +
    facet_wrap(~UC) +
    theme(legend.position = "bottom", legend.title = element_blank(), axis.title.x = element_blank(), axis.text.x = element_blank())

ggsave(sprintf("prediction_time_by_uc_%s_%s.pdf", tolower(booster_type), at_percentile))