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

serializeArguments = function(arguments){
	conn = rawConnection(raw(0), "r+")
	serialize(arguments, conn, ascii = FALSE)
	rdsArguments = rawConnectionValue(conn)
	close(conn)

	return(rdsArguments)
}

unserializeResults = function(rdsResults){
	conn = rawConnection(rdsResults)
	results = unserialize(conn)
	close(conn)

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
		rdsArguments = serializeArguments(arguments)
		rdsResults = J("org.jpmml.evaluator.rexp.RExpUtil")$evaluate(evaluator@javaEvaluator, rdsArguments)
		results = unserializeResults(rdsResults)
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