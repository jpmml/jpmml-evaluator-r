setClass("Evaluator",
	slots = c(
		"javaEvaluator" = "jobjRef"
	)
)

setGeneric("verify",
	def = function(evaluator){
		standardGeneric("verify")
	}
)
setMethod("verify",
	signature = c("Evaluator"),
	definition = function(evaluator){
		javaEvaluator = .jcall(evaluator@javaEvaluator, returnSig = "Lorg/jpmml/evaluator/Evaluator;", "verify")
		return(evaluator)
	}
)

formatArguments = function(arguments){
	javaArguments = .jnew("java/util/HashMap")

	for(name in names(arguments)){
		value = arguments[[name]]

		if(is.integer(value)){
			javaValue = .jnew("java/lang/Integer", value)
		} else

		if(is.numeric(value)){
			javaValue = .jnew("java/lang/Double", value)
		} else

		if(is.logical(value)){
			javaValue = .jnew("java/lang/Boolean", value)
		} else

		if(is.character(value)){
			javaValue = .jnew("java/lang/String", value)
		} else

		if(is.factor(value)){
			javaValue = .jnew("java/lang/String", as.character(value))
		} else

		{
			stop("Not a scalar value")
		}

		.jrcall(javaArguments, "put", name, javaValue)
	}

	return(javaArguments)
}

parseResults = function(javaResults){
	results = list()

	javaKeySet = .jrcall(javaResults, "keySet")

	javaIt = .jrcall(javaKeySet, "iterator")
	while(.jrcall(javaIt, "hasNext")){
		javaName = .jrcall(javaIt, "next")
		javaValue = .jrcall(javaResults, "get", javaName)

		name = .jsimplify(javaName)
		value = .jsimplify(javaValue)

		if(is(value, "jobjRef")){
			value = .jrcall("org/jpmml/evaluator/EvaluatorUtil", "decode", value)
		}

		results[[name]] = value
	}

	return(results)
}

setGeneric("evaluate",
	def = function(evaluator, arguments){
		standardGeneric("evaluate")
	}
)
setMethod("evaluate",
	signature = c("Evaluator", "list"),
	definition = function(evaluator, arguments){
		javaArguments = formatArguments(arguments)
		javaResults = .jrcall(evaluator@javaEvaluator, "evaluate", javaArguments)
		results = parseResults(javaResults)
		return(results)
	}
)

setGeneric("evaluateAll",
	def = function(evaluator, argumentsDf){
		standardGeneric("evaluateAll")
	}
)
setMethod("evaluateAll",
	signature = c("Evaluator", "data.frame"),
	definition = function(evaluator, argumentsDf){
		results = apply(argumentsDf, 1, function(x){
			return(evaluate(evaluator, as.list(x))) 
		})
		resultsDf = as.data.frame(do.call(rbind, results))
		return(resultsDf)
	}
)