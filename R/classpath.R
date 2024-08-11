.classpath = function(){
	pkg.jpmml = find.package("jpmml")

	java_dir = file.path(pkg.jpmml, "java")

	jar_files = readLines(file.path(java_dir, "classpath.txt"), encoding = "UTF-8", warn = FALSE)

	jar_files = sapply(jar_files, function(x){
		return(file.path(java_dir, x))
	})

	return(paste(jar_files, collapse = .Platform$path.sep))
}