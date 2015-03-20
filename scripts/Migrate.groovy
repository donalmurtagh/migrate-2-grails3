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

    File targetGroovySrcDir = canonicalFile(targetDirPath, 'src/main/groovy')

    // copy Groovy and Java sources
    ['java', 'groovy'].each { String srcDirName ->
        copyDir(grailsSettings.sourceDir.path, srcDirName, targetGroovySrcDir.path)
    }

    // copy grails-app
    copyDir(baseDirPath, 'grails-app', targetDirPath, 'grails-app')

    String sourceTestsBase = grailsSettings.testSourceDir.path

    // copy the tests
    copyDir(sourceTestsBase, 'unit', targetDirPath, 'src/test/groovy')
    copyDir(sourceTestsBase, 'integration', targetDirPath, 'src/integration-test/groovy')
    copyDir(sourceTestsBase, 'functional', targetDirPath, 'src/integration-test/groovy')

    // copy web-app and scripts
    copyDir(baseDirPath, 'web-app', targetDirPath, 'src/main/webapp')
    copyDir(baseDirPath, 'scripts', targetDirPath, 'src/main/scripts')

    File targetConfigDir = new File(targetDirPath, 'grails-app/conf')

    // move various files
    moveFile(targetConfigDir.canonicalPath, 'UrlMappings.groovy', targetDirPath, 'grails-app/controllers/UrlMappings.groovy')
    moveFile(targetConfigDir.canonicalPath, 'BootStrap.groovy', targetDirPath, 'grails-app/init/BootStrap.groovy')

    // delete WEB-INF and META-INF dirs
    // TODO find out if .tld files in WEB-INF should instead be moved to webapp. See issue #4
    ['src/main/webapp/WEB-INF', 'src/main/webapp/META-INF'].each { String dirName ->
        File dir = new File(targetDirPath, dirName)
        FileUtils.deleteDirectory(dir)
        console.info "Deleted directory $dir"
    }

    // delete filters, these should be replaced by interceptors
    targetConfigDir.eachFileRecurse(FileType.FILES) { File file ->
        if (file.name.endsWith('Filters.groovy')) {
            assert file.delete(), "Failed to delete filter file: $file"
            console.info "Deleted filter file: $file"
        }
    }

    // delete config files that are not used in Grails 3
    ['BuildConfig.groovy', 'Config.groovy', 'DataSource.groovy'].each { String configFileName ->
        FileUtils.deleteQuietly(new File(targetConfigDir, configFileName))
    }

    if (grailsSettings.pluginProject) {

        // migrate the plugin descriptor
        File targetPluginDescriptor
        targetGroovySrcDir.eachFileRecurse(FileType.FILES) {
            if (it.name.endsWith('GrailsPlugin.groovy')) {
                targetPluginDescriptor = it
            }
        }

        assert targetPluginDescriptor, "Plugin descriptor not found under $targetGroovySrcDir"
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
            targetDescriptorContent[0..targetClassDefIndex].each { writer.writeLine it }
            sourceDescriptorContent[sourceClassDefIndex + 1..-1].each { writer.writeLine it }
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
    Pattern pluginClassDefRegex = Pattern.compile(/.*class.*\s+.*[a-zA-Z_$]+GrailsPlugin.*\{.*/)

    for (int i = 0; i < lines.size(); i++) {
        String line = lines[i]

        if (pluginClassDefRegex.matcher(line).matches()) {
            return i
        }
    }

    assert false, "Plugin class definition not found in content ${lines.join(System.getProperty('line.separator'))}"
}

/**
 * Moves a file.
 * @param sourceBase the base source dir
 * @param sourceRelative the relative path from sourceBase to the source file
 * @param targetBase the base target dir
 * @param targetRelative the relative path from targetBase to the target file. If targetBase
 * is the target file, this may be omitted
 */
void moveFile(String sourceBase, String sourceRelative, String targetBase, String targetRelative) {

    File source = canonicalFile(sourceBase, sourceRelative)
    if (!source.file) {
        console.warn "File $source not found in Grails 2 project - skipping"
        return
    }

    File target = canonicalFile(targetBase, targetRelative)
    FileUtils.deleteQuietly(target)
    console.info "Moving $source to $target"
    FileUtils.moveFile(source, target)
}

/**
 * Copies the contents of one directory to another.
 * @param sourceBase the base source dir
 * @param sourceRelative the relative path from sourceBase to the source dir
 * @param targetBase the base target dir
 * @param targetRelative the relative path from targetBase to the target dir. If targetBase
 * is the target dir, this may be omitted
 */
void copyDir(String sourceBase, String sourceRelative, String targetBase, String targetRelative = '') {
    File source = canonicalFile(sourceBase, sourceRelative)
    if (!source.directory) {
        console.warn "Directory $source not found in Grails 2 project - skipping"
        return
    }

    File target = canonicalFile(targetBase, targetRelative)
    console.info "Copying contents of $source to $target"
    FileUtils.copyDirectory(source, target)
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