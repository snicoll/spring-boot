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

private writeParameters(PrintWriter writer, def parameters, def configuration, def sectionId) {
	parameters.each { parameter ->
		def name = parameter.name.text()
		writer.println "[[$sectionId-$name]]"
		writer.println "==== ${name}"
		writer.println '[cols="10h,90"]'
		writer.println '|==='
		writer.println()
		writer.println '| Type'
		def type = parameter.type.text()
		if (type.lastIndexOf('.') >= 0) {
			type = type.substring(type.lastIndexOf('.') + 1)
		}
		writer.println("| `$type`")
		def defaultValue = "${configuration[name].@'default-value'}"
		if (defaultValue) {
			writer.println '| Default value'
			writer.println("| `$defaultValue`")
		}
		def userProperty = "${configuration[name].text().replace('${', '`').replace('}', '`')}"
		writer.println '| User property'
		userProperty ? writer.println("| ${userProperty}") : writer.println("|")
		writer.println '| Description'
		writer.println("| ${format(parameter.description.text())}")
		writer.println '| Since'
		def since = parameter.since.text()
		since ? writer.println("| `${since}`") : writer.println("|")

		writer.println()
		writer.println '|==='
	}
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
			def parametersSectionId = "$sectionId-required-parameters"
			writer.println("[[$parametersSectionId]]")
			writer.println("=== Required parameters")
			writeParameters(writer, requiredParameters, mojo.configuration, parametersSectionId)
			writer.println()
		}
		def optionalParameters = parameters.findAll { it.required.text() == 'false' }
		if (optionalParameters.size()) {
			def parametersSectionId = "$sectionId-optional-parameters"
			writer.println("[[$parametersSectionId]]")
			writer.println("=== Optional parameters")
			writeParameters(writer, optionalParameters, mojo.configuration, parametersSectionId)
			writer.println()
		}
	}
}
