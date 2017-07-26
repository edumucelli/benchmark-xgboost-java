
# library("devtools")
# install_github(repo = "jpmml/r2pmml")

library(caret)
library(r2pmml)
library(xgboost)

NROW_TRAIN = 2000
N_columns = c(100, 500, 1000)

train_with_formula <- function(formula, data, method, tune_length = 1, tune_grid = NULL) {
    crtl = trainControl(method = "cv",
                        # verboseIter = TRUE,
                        returnData = TRUE)
                        method_fit = train( formula,
                                            data = data,
                                            method = method,
                                            trControl = crtl,
                                            tuneLength = tune_length,
                                            tuneGrid = tune_grid,
                                            control = list(maxit = 2))
    return (method_fit)
}

train_with_matrix <- function(train_x, train_y, method, tune_length = 1, tune_grid = NULL) {
    crtl = trainControl(method = "cv",
                        # verboseIter = TRUE,
                        returnData = TRUE)
                        method_fit = train( x = train_x,
                                            y = train_y,
                                            method = method,
                                            trControl = crtl,
                                            tuneLength = tune_length,
                                            tuneGrid = tune_grid,
                                            control = list(maxit = 2))
    return (method_fit)
}

PMML = function(train_x, train_y, model_path){
  # PMML
  method = "gbm"
  model_fit = train_with_matrix(train_x, train_y, method)
  r2pmml(model_fit, paste0(model_path, '/', method, ".", dim(train_x)[2], ".pmml"))
}

XGB_xgb = function(train_x, train_y, model_path){
    # XGB
    method = "xgbTree"
    tuneGrid = expand.grid(nrounds = 500,
                           max_depth = 6,
                           eta = 0.01,
                           gamma = 0.5,
                           colsample_bytree = 0.8,
                           min_child_weight = 1,
                           subsample = 1)

    model_fit = train_with_matrix(train_x, train_y, method, tune_grid = tuneGrid)
    # https://github.com/dmlc/xgboost/issues/1955
    bst <- xgboost:::xgb.Booster.check(model_fit$finalModel, saveraw = FALSE)
    xgb.save(bst, fname = paste0(model_path, '/', method, ".", dim(train_x)[2], ".xgb"))
}

XGB_logreg = function(train_x, train_y, model_path){
    # XGB with only 1 iteration = Logistic Regression
    method = "xgbLinear"
    tuneGrid = expand.grid(nrounds = 1,
                           eta = 0.7,
                           lambda = 0.1,
                           alpha = 0.01)

    model_fit = train_with_matrix(train_x, train_y, method, tune_grid = tuneGrid)
    # https://github.com/dmlc/xgboost/issues/1955
    bst <- xgboost:::xgb.Booster.check(model_fit$finalModel, saveraw = FALSE)
    xgb.save(bst, fname = paste0(model_path, '/', method, ".", dim(train_x)[2], ".xgb"))
}

RJava = function(train_x, train_y, model_path){   
    # RJava
    method = "glm"
    model_fit = train_with_matrix(train_x, train_y, method)
    saveRDS(model_fit, paste0(model_path, '/', method, ".", dim(train_x)[2], ".r"))
}

generate_models = function(func_list, N_columns, model_path){
    for (NCOL in N_columns) {
        CLASS = replicate(NROW_TRAIN, sample(c('true', 'false'), size = 1, replace=FALSE, prob=c(0.7, 0.3)))
        data = data.frame(CLASS = CLASS, replicate(NCOL, sample(rnorm(NROW_TRAIN), replace=TRUE)))

        trainIndex = createDataPartition(data$CLASS, p = .75, times = 1, list = FALSE)

        train = data[trainIndex, ]
        train_x = train[, -1]
        train_y = train[, 1]

        for( f_ in func_list){
            f_(train_x, train_y, model_path)
        }
        
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
}

generate_models(list(XGB_logreg), N_columns, '/Users/r.serres/Documents/search/benchmark-ml-java/generate_models/models')
