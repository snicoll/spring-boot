import groovy.util.XmlSlurper

private String format(String input) {
	input.replace("<code>", "`")
		.replace("</code>", "`")
		.replace("&lt;", "<")
		.replace("&gt;", ">")
		.replace("<br>", " ")
		.replace("\n", " ")
		.replace("&quot;", '"')
		.replaceAll('\\{@code (.*?)\\}', '`$1`')
		.replaceAll('\\{@link (.*?)\\}', '`$1`')
		.replaceAll('\\{@literal (.*?)\\}', '`$1`')
		.replaceAll('<a href=."(.*?)".>(.*?)</a>', '\$1[\$2]')
}

private writeParametersTable(PrintWriter writer, def parameters, def configuration) {
	writer.println '[cols="3,2,2,3,8"]'
	writer.println '|==='
	writer.println '| Name | Type | Default | User property | Description'
	writer.println()
	parameters.each { parameter ->
		def name = parameter.name.text()
		writer.println("| $name")
		def type = parameter.type.text()
		if (type.lastIndexOf('.') >= 0) {
			type = type.substring(type.lastIndexOf('.') + 1)
		}
		writer.println("| `$type`")
		def defaultValue = "${configuration[name].@'default-value'}"
		if (defaultValue) {
			writer.println("| `$defaultValue`")
		}
		else {
			writer.println("|")
		}
		def userProperty = "${configuration[name].text().replace('${', '`').replace('}', '`')}"
		if (userProperty) {
			writer.println("| ${userProperty}")
		}
		else {
			writer.println("|")
		}
		writer.println("| ${format(parameter.description.text())}")
		def since = parameter.since.text()
		if (since) {
			writer.println("")
			writer.println("Since ${since}.")
		}
		writer.println()
	}
	writer.println '|==='
}

def plugin = new XmlSlurper().parse("${project.build.outputDirectory}/META-INF/maven/plugin.xml" as File)
String goalPrefix = plugin.goalPrefix.text()
File goalsAdoc = new File(project.build.directory, "generated-resources/goals.adoc")
goalsAdoc.parentFile.mkdirs()
goalsAdoc.withPrintWriter { writer ->
	writer.println '[cols="1,3"]'
	writer.println '|==='
	writer.println '| Goal | Description'
	writer.println()
	plugin.mojos.mojo.each { mojo ->
		writer.println "| ${goalPrefix}:${mojo.goal.text()}"
		writer.println "| ${format(mojo.description.text())}"
		writer.println()
	}
	writer.println '|==='
	plugin.mojos.mojo.each { mojo ->
		def sectionId = "goals-${mojo.goal.text()}"
		writer.println()
		writer.println("[[$sectionId]]")
		writer.println("== `${goalPrefix}:${mojo.goal.text()}`")
		writer.println("`${plugin.groupId.text()}:${plugin.artifactId.text()}:${plugin.version.text()}:${mojo.goal.text()}`")
		writer.println()
		writer.println(format(mojo.description.text()))
		writer.println()
		def parameters = mojo.parameters.parameter.findAll { it.editable.text() == 'true' }
		def requiredParameters =  parameters.findAll { it.required.text() == 'true' }
		if (requiredParameters.size()) {
			writer.println("[[$sectionId-required-parameters]]")
			writer.println("=== Required parameters")
			writeParametersTable(writer, requiredParameters, mojo.configuration)
			writer.println()
		}
		def optionalParameters = parameters.findAll { it.required.text() == 'false' }
		if (optionalParameters.size()) {
			writer.println("[[$sectionId-optional-parameters]]")
			writer.println("=== Optional parameters")
			writeParametersTable(writer, optionalParameters, mojo.configuration)
			writer.println()
		}
	}
}
