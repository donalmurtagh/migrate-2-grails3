import grails.build.logging.GrailsConsole
import groovy.io.FileType
import org.apache.commons.io.FileUtils

import java.util.regex.Pattern

includeTargets << grailsScript("_GrailsArgParsing")

console = GrailsConsole.instance

target(migrate: "Migrates a Grails 2.X app or plugin to Grails 3") {

    depends(parseArguments)

    String pathToTargetApp = argsMap.params[0]
    String baseDirPath = grailsSettings.baseDir.path

    File targetDir = canonicalFile(baseDirPath, pathToTargetApp)
    String targetDirPath = targetDir.path
    if (!targetDir.directory) {
        console.error "Grails 3 project not found at $targetDirPath - quitting"
        return
    }

    String targetSrcDirPath = canonicalFile(targetDirPath, 'src/main').path

    Closure copier = { File source, File target -> FileUtils.copyDirectoryToDirectory(source, target) }

    // copy Groovy source
    copyDir(copier, grailsSettings.sourceDir.path, 'groovy', targetSrcDirPath)

    // copy grails-app
    copyDir(copier, baseDirPath, 'grails-app', targetDirPath, 'grails-app')

    String sourceTestsBase = grailsSettings.testSourceDir.path
    copier = { File source, File target -> FileUtils.copyDirectory(source, target) }

    // copy Java source
    File targetGroovySrcDir = canonicalFile(targetSrcDirPath, 'groovy')
    String targetGroovySrcDirPath = targetGroovySrcDir.path
    copyDir(copier, grailsSettings.sourceDir.path, 'java', targetGroovySrcDirPath)

    // copy the tests
    copyDir(copier, sourceTestsBase, 'unit', targetDirPath, 'src/test/groovy')
    copyDir(copier, sourceTestsBase, 'integration', targetDirPath, 'src/integration-test/groovy')
    copyDir(copier, sourceTestsBase, 'functional', targetDirPath, 'src/integration-test/groovy')

    // copy web-app and scripts
    copyDir(copier, baseDirPath, 'web-app', targetDirPath, 'src/main/webapp')
    copyDir(copier, baseDirPath, 'scripts', targetDirPath, 'src/main/scripts')

    // copy various individual files
    copyFile(baseDirPath, 'grails-app/conf/UrlMappings.groovy', targetDirPath, 'grails-app/controllers/UrlMappings.groovy')
    copyFile(baseDirPath, 'grails-app/conf/BootStrap.groovy', targetDirPath, 'grails-app/init/BootStrap.groovy')

    if (grailsSettings.pluginProject) {

        // migrate the plugin descriptor
        File targetPluginDescriptor
        targetGroovySrcDir.eachFileRecurse(FileType.FILES) {
            if (it.name.endsWith('GrailsPlugin.groovy')) {
                targetPluginDescriptor = it
            }
        }

        assert targetPluginDescriptor, "Plugin descriptor not found under $targetGroovySrcDirPath"
        List<String> targetDescriptorContent = targetPluginDescriptor.readLines()

        // complete migration of plugin descriptor by replacing every line after
        // class ExampleGrailsPlugin extends Plugin {
        // in the target plugin descriptor with every line after
        // class ExamplePlugin {
        // in the source plugin descriptor
        List<String> sourceDescriptorContent = grailsSettings.basePluginDescriptor.readLines()
        int sourceClassDefIndex = getPluginClassDefinitionIndex(sourceDescriptorContent)
        int targetClassDefIndex = getPluginClassDefinitionIndex(targetDescriptorContent)

        targetPluginDescriptor.withWriter { BufferedWriter writer ->
            sourceDescriptorContent[sourceClassDefIndex + 1..-1].each { writer.writeLine it }
            targetDescriptorContent[0..targetClassDefIndex].each { writer.writeLine it }
        }
    }
}

/**
 * Find the line number that contains the plugin class definition.
 * @param lines the plugin class content
 * @return the index
 */
int getPluginClassDefinitionIndex(List<String> lines) {

    // a real programmer would use ANTLR
    Pattern regex = Pattern.compile(/.*class.*\s+.*[a-zA-Z_$]+GrailsPlugin.*\{.*/)

    for (int i = 0; i < lines.size(); i++) {
        String line = lines[i]

        if (regex.matcher(line).matches()) {
            return i
        }
    }

    throw new IllegalStateException("Plugin class definition not found in content ${lines.join(System.getProperty('line.separator'))}")
}

/**
 * Copies a file.
 * @param sourceBase the base source dir
 * @param sourceRelative the relative path from sourceBase to the source file
 * @param targetBase the base target dir
 * @param targetRelative the relative path from targetBase to the target file. If targetBase
 * is the target file, this may be omitted
 */
void copyFile(String sourceBase, String sourceRelative, String targetBase, String targetRelative = '') {

    File source = canonicalFile(sourceBase, sourceRelative)
    if (!source.file) {
        console.warn "File $source not found in Grails 2 project - skipping"
        return
    }

    File target = canonicalFile(targetBase, targetRelative)
    console.info "Copying $source to $target"
    FileUtils.copyFile(source, target)
}

/**
 * Copies the contents of one directory to another.
 * @param copier performs the copying
 * @param sourceBase the base source dir
 * @param sourceRelative the relative path from sourceBase to the source dir
 * @param targetBase the base target dir
 * @param targetRelative the relative path from targetBase to the target dir. If targetBase
 * is the target dir, this may be omitted
 */
void copyDir(Closure copier, String sourceBase, String sourceRelative, String targetBase, String targetRelative = '') {
    File source = canonicalFile(sourceBase, sourceRelative)
    if (!source.directory) {
        console.warn "Directory $source not found in Grails 2 project - skipping"
        return
    }

    File target = canonicalFile(targetBase, targetRelative)
    console.info "Copying contents of $source to $target"
    copier(source, target)
}

/**
 * Construct a canonical file from a base path and a relative path from it.
 * @param base path to a file
 * @param relative the relative path
 * @return the file
 */
File canonicalFile(String base, String relative) {
    new File(base, relative).canonicalFile
}

setDefaultTarget(migrate)
