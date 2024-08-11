JPMML-Evaluator-R
=================

PMML evaluator library for R.

# Features #

This package provides R wrapper classes and functions for the [JPMML-Evaluator](https://github.com/jpmml/jpmml-evaluator) library.

# Prerequisites #

* Java Platform, Standard Edition 8 or newer.
* R 3.3, 4.0 or newer.

# Installation #

This package has not been released to CRAN yet.

Installing the latest snapshot version from GitHub using the [`devtools`](https://cran.r-project.org/package=devtools) package:

```R
library("devtools")

install_github("jpmml/jpmml-evaluator-r")
```

# Usage #

Building a verified model evaluator from a PMML file:

```R
library("jpmml")
library("magrittr") # Defines the `%>%` operator

evaluator = newLoadingModelEvaluatorBuilder() %>%
	loadFile("DecisionTreeIris.pmml") %>%
	build()

evaluator = evaluator %>%
	verify()
```

Evaluating a single data record:

```R
arguments = list(
	"Sepal.Length" = 5.1,
	"Sepal.Width" = 3.5,
	"Petal.Length" = 1.4,
	"Petal.Width" = 0.2
)

results = evaluator %>%
	evaluate(arguments)

print(results)
```

Evaluating a collection of data records:

```R
data(iris)

argumentsDf = iris

resultsDf = evaluator %>%
	evaluateAll(argumentsDf)

print(resultsDf)
```

# De-installation #

Removing the package:

```R
remove.packages("jpmml")
```

# License #

JPMML-Evaluator-R is licensed under the terms and conditions of the [GNU Affero General Public License, Version 3.0](https://www.gnu.org/licenses/agpl-3.0.html).

# Additional information #

JPMML-Evaluator-R is developed and maintained by Openscoring Ltd, Estonia.

Interested in using [Java PMML API](https://github.com/jpmml) software in your company? Please contact [info@openscoring.io](mailto:info@openscoring.io)