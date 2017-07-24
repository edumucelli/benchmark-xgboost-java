library(caret)
# library("devtools")
# install_github(repo = "jpmml/r2pmml")
library(r2pmml)
library(xgboost)

NROW_TRAIN = 2000
columns = c(100, 500, 1000)
rows = c(100, 500, 1000)

train_with_formula <- function(formula, data, method, tune_length = 1, tune_grid = NULL) {
    crtl = trainControl(method = "cv",
                        # verboseIter = TRUE,
                        returnData = TRUE)
                        method_fit = train(formula,
                        data = data,
                        method = method,
                        trControl = crtl,
                        tuneLength = tune_length,
                        tuneGrid = tune_grid)
                        # For GLM
                        # control = list(maxit = 50))
    return (method_fit)
}

train_with_matrix <- function(train_x, train_y, method, tune_length = 1, tune_grid = NULL) {
    crtl = trainControl(method = "cv",
                        # verboseIter = TRUE,
                        returnData = TRUE)
                        method_fit = train(x = train_x,
                        y = train_y,
                        method = method,
                        trControl = crtl,
                        tuneLength = tune_length,
                        tuneGrid = tune_grid)
                        # For GLM
                        # control = list(maxit = 50))
    return (method_fit)
}

for (NCOL in columns) {
    CLASS = replicate(NROW_TRAIN, sample(c('true', 'false'), size = 1, replace=FALSE, prob=c(0.7, 0.3)))
    data = data.frame(CLASS = CLASS, replicate(NCOL, sample(rnorm(NROW_TRAIN), replace=TRUE)))

    trainIndex = createDataPartition(data$CLASS, p = .75, times = 1, list = FALSE)

    train = data[ trainIndex,]
    test  = data[-trainIndex,]

    train_x = train[, -1]
    train_y = train[, 1]

    # # PMML
    # method = "gbm"
    # model_fit = train_with_matrix(train_x, train_y, method)
    # r2pmml(model_fit, paste0(method, ".benchmark.", NCOL, ".pmml"))

    # XGB
    # method = "xgbTree"
    # tuneGrid = expand.grid(nrounds = 500,
    #                        max_depth = 6,
    #                        eta = 0.01,
    #                        gamma = 0.5,
    #                        colsample_bytree = 0.8,
    #                        min_child_weight = 1,
    #                        subsample = 1)

    # # print(paste("Training for", NCOL, "columns"))
    # model_fit = train_with_matrix(train_x, train_y, method, tune_grid = tuneGrid)
    # # https://github.com/dmlc/xgboost/issues/1955
    # bst <- xgboost:::xgb.Booster.check(model_fit$finalModel, saveraw = FALSE)
    # xgb.save(bst, fname = paste0(method, ".benchmark.", NCOL, ".xgb"))
    # # print(paste("Trained for", NCOL, "columns"))

    # RJava
    method = "glm"
    model_fit = train_with_matrix(train_x, train_y, method)
    saveRDS(model_fit, paste0(method, ".benchmark.", NCOL, ".r"))

    # RJava (local for dimensioning purposes)
    # for (NROW_TEST in rows) {
    #     # print(paste("Testing for", NROW_TEST, "rows for", method))
    #     test = head(test, NROW_TEST)
    #     elapsed_times = replicate(100, system.time(predict(model_fit, test))[3])
    #     mean_elapsed_time = mean(elapsed_times)
    #     se_elapsed_time = 1.96 * (sd(elapsed_times) / sqrt(length(elapsed_times)))
    #     print(paste0(NROW_TEST, ",", NCOL, ",", mean_elapsed_time * 1000, ",", se_elapsed_time * 1000, ",", toupper(method)))
    # }
}