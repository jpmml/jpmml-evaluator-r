library("magrittr")

data(iris)

evaluatorBuilder = newLoadingModelEvaluatorBuilder() %>%
	loadFile("resources/DecisionTreeIris.pmml")

test_that("EvaluatorBuilder", {
	expect_true(isS4(evaluatorBuilder))
	expect_true(is(evaluatorBuilder@javaEvaluatorBuilder, "jobjRef"))
})

evaluator = evaluatorBuilder %>%
	build()

test_that("Evaluator", {
	expect_true(isS4(evaluator))
	expect_true(is(evaluator@javaEvaluator, "jobjRef"))
})

evaluator = evaluator %>%
	verify()

arguments = as.list(iris[1, 1:4])
results = evaluator %>%
	evaluate(arguments)

test_that("evaluate(Evaluator, list)", {
	expect_true(is.list(arguments))
	expect_equal(4, length(arguments))
	expect_true(is.list(results))
	expect_equal(4, length(results))
	expect_equal(c("Species", "probability(setosa)", "probability(versicolor)", "probability(virginica)"), names(results))
})

argumentsDf = iris[, 1:4]
resultsDf = evaluator %>%
	evaluateAll(argumentsDf)

test_that("evaluateAll(Evaluator, data.frame)", {
	expect_true(is.data.frame(argumentsDf))
	expect_equal(c(150, 4), dim(argumentsDf))
	expect_true(is.data.frame(resultsDf))
	expect_equal(c(150, 4), dim(resultsDf))
	expect_equal(c("Species", "probability(setosa)", "probability(versicolor)", "probability(virginica)"), colnames(resultsDf))
})
