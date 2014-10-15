import org.springframework.boot.config.ConfigMetadataGroup
import org.springframework.boot.config.ConfigMetadataItem
import org.springframework.boot.config.ConfigMetadataRepository
import org.springframework.boot.config.support.ConfigMetadataRepositoryJsonLoader

def generateTable(File outputDir) {
	new File(outputDir, "configuration-items-table.adoc")
			.withPrintWriter {
		generate(it)
	}
}

def generate(PrintWriter writer) {
	writer.println '''[source,properties,indent=0,subs="verbatim,attributes,macros"]
----
\t# ===================================================================
\t# COMMON SPRING BOOT PROPERTIES
\t#
\t# This sample file is provided as a guideline. Do NOT copy it in its
\t# entirety to your own application.               ^^^
\t# ==================================================================='''
	getRootGroups(loadRepository()).each {
		writer.println()
		def title = title(it.id, it.types);
		writer.println('\t' + title)
		getAllItems(it).each {
			writer.println('\t' + item(it))
		}
	}
	writer.println '----'
}


def ConfigMetadataRepository loadRepository() {
	new ConfigMetadataRepositoryJsonLoader().loadFromClasspath();
}


def getRootGroups(ConfigMetadataRepository repository) {
	repository.groups.values()
			.sort { a, b -> a.name.compareTo(b.name) }
}

def getAllItems(ConfigMetadataGroup group) {
	group.allItems.values()
			.sort { a, b -> a.id.compareTo(b.id) }
}

def String title(String id, List<String> types) {
	def autoConfigurePackage = 'org.springframework.boot.autoconfigure'
	def actuatorPackage = 'org.springframework.boot.actuate'
	Collection<String> typeLinks = new ArrayList<String>()
	types.each() {
		// Guess the module
		if (it.startsWith(autoConfigurePackage)) {
			typeLinks.add(createLink('{github-code}/spring-boot-autoconfigure/src/main/java/org/springframework/boot/autoconfigure',
					it.substring(autoConfigurePackage.length() + 1, it.size())));
		} else if (it.startsWith(actuatorPackage)) {
			typeLinks.add(createLink('{github-code}/spring-boot-actuator/src/main/java/org/springframework/boot/actuate',
					it.substring(actuatorPackage.length() + 1, it.size())));
		} else {
			typeLinks.add('`' + className(it) + '`')
		}
	}
	StringBuilder result = new StringBuilder()
	result.append("# ").append(id)
	if (!typeLinks.isEmpty()) {
		result.append(' (').append(typeLinks.join(' - ')).append(')')
	}
	return result.toString();
}

def String createLink(String linkPrefix, String remaining) {
	String name = className(remaining);
	String rootClassLink = rootClassLink(remaining);
	StringBuilder sb = new StringBuilder();
	sb.append(linkPrefix).append('/')
			.append(rootClassLink).append('.{sc-ext}[')
			.append(name).append(']');
	return sb.toString();
}

def String rootClassLink(String remaining) {
	String result = remaining;
	int index = remaining.indexOf('$');
	if (index != -1) {
		result = result.substring(0, index);
	}
	return result.replace('.', '/');
}

def String className(String remaining) {
	// Check for inner classes
	int index = remaining.lastIndexOf('$');
	if (index != -1) {
		return remaining.substring(index + 1, remaining.size());
	}
	index = remaining.lastIndexOf(".");
	if (index != -1) {
		return remaining.substring(index + 1, remaining.size());
	}
	return remaining;
}

def String item(ConfigMetadataItem item) {
	StringBuilder sb = new StringBuilder();
	sb.append(lowerCaseDash(item.id)).append('=')
	if (item.description != null) {
		sb.append(' # ').append(item.tagLine)
	}
	return sb.toString()
}

def String lowerCaseDash(String value) {
	value = value.replaceAll('([^A-Z-])([A-Z])', '$1-$2');
	StringBuilder builder = new StringBuilder();
	for (String field : value.split("-")) {
		if (builder.length() == 0) {
			builder.append(field);
		}
		else {
			builder.append("-").append(field.toLowerCase());
		}
	}
	return builder.toString();
}


def generatedResourcesDir = new File(project.build.directory, 'generated-resources')
generateTable(generatedResourcesDir)