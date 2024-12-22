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

setClassUnion("OptionalLogical", c("logical", "missing"))

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
	def = function(evaluator, argumentsDf, stringsAsFactors = TRUE){
		standardGeneric("evaluateAll")
	}
)
setMethod("evaluateAll",
	signature = c("Evaluator", "data.frame", "OptionalLogical"),
	definition = function(evaluator, argumentsDf, stringsAsFactors){
		rdsArgumentsDf = serializeArguments(argumentsDf)
		if(missing(stringsAsFactors)){
			stringsAsFactors = TRUE
		}
		rdsResultsDf = J("org.jpmml.evaluator.rexp.RExpUtil")$evaluateAll(evaluator@javaEvaluator, rdsArgumentsDf, stringsAsFactors)
		resultsDf = unserializeResults(rdsResultsDf)
		errors = attr(resultsDf, "errors")
		resultsDf = data.frame(resultsDf, check.names = FALSE, stringsAsFactors = stringsAsFactors)
		if(!is.null(errors)){
			attr(resultsDf, "errors") = errors
		}
		return(resultsDf)
	}
)