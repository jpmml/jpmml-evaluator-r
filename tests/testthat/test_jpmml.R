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

arguments$`Sepal.Length` = "error"
arguments$`Petal.Length` = "error"

test_that("evaluate(Evaluator, list) raises value check error", {
	expect_error(evaluate(evaluator, arguments), "org.jpmml.evaluator.ValueCheckException: Field \"Petal.Length\" cannot accept invalid value \"error\"")
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
	expect_true(is.factor(resultsDf$Species))
	expect_equal(c("setosa", "versicolor", "virginica"), levels(resultsDf$Species))
	expect_true(is.double(resultsDf$`probability(setosa)`))
	expect_null(attr(resultsDf, "errors"))
})

resultsDf = evaluator %>%
	evaluateAll(argumentsDf, stringsAsFactors = FALSE)

test_that("evaluateAll(Evaluator, data.frame, logical)", {
	expect_true(is.data.frame(resultsDf))
	expect_equal(c(150, 4), dim(resultsDf))
	expect_equal(c("Species", "probability(setosa)", "probability(versicolor)", "probability(virginica)"), colnames(resultsDf))
	expect_true(is.character(resultsDf$Species))
	expect_equal(c("setosa", "versicolor", "virginica"), unique(resultsDf$Species))
	expect_true(is.double(resultsDf$`probability(setosa)`))
	expect_null(attr(resultsDf, "errors"))
})

argumentsDf[13, ] = c("error", "error", "error", "error")
resultsDf = evaluator %>%
	evaluateAll(argumentsDf, stringsAsFactors = TRUE)

test_that("evaluateAll(Evaluator, data.frame, logical) raises and catches a value check error", {
	expect_true(is.data.frame(resultsDf))
	expect_equal(c(150, 4), dim(resultsDf))

	errors = attr(resultsDf, "errors")
	expect_length(errors, 150)
	expect_true(is.factor(errors))
	expect_equal(c("org.jpmml.evaluator.ValueCheckException: Field \"Petal.Length\" cannot accept invalid value \"error\"", NA_character_), levels(errors))
	
	#expect_equal(2, errors[1])
	expect_equal(1, errors[13])
	#expect_equal(2, errors[151])	
})
