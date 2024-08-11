setClass("EvaluatorBuilder",
	slots = c(
		"javaEvaluatorBuilder" = "jobjRef"
	)
)

setGeneric("build",
	def = function(evaluatorBuilder){
		standardGeneric("build")
	}
)
setMethod("build",
	signature = c("EvaluatorBuilder"),
	definition = function(evaluatorBuilder){
		javaEvaluator = .jcall(evaluatorBuilder@javaEvaluatorBuilder, returnSig = "Lorg/jpmml/evaluator/Evaluator;", "build")
		evaluator = new("Evaluator", javaEvaluator = javaEvaluator)
		return(evaluator)
	}
)

setClass("ModelEvaluatorBuilder",
	contains = "EvaluatorBuilder"
)

setClass("LoadingModelEvaluatorBuilder",
	contains = "ModelEvaluatorBuilder"
)

newLoadingModelEvaluatorBuilder = function(){
	loadingModelEvaluatorBuilder = new("LoadingModelEvaluatorBuilder", javaEvaluatorBuilder = .jnew("org/jpmml/evaluator/LoadingModelEvaluatorBuilder"))
	return(loadingModelEvaluatorBuilder)
}

setGeneric("loadFile",
	def = function(loadingModelEvaluatorBuilder, path){
		standardGeneric("loadFile")
	}
)
setMethod("loadFile", 
	signature = c("LoadingModelEvaluatorBuilder", "character"),
	definition = function(loadingModelEvaluatorBuilder, path){
		javaFile = .jnew("java/io/File", path)
		javaEvaluatorBuilder = .jcall(loadingModelEvaluatorBuilder@javaEvaluatorBuilder, returnSig = "Lorg/jpmml/evaluator/LoadingModelEvaluatorBuilder;", "load", javaFile)
		return(loadingModelEvaluatorBuilder)
	}
)