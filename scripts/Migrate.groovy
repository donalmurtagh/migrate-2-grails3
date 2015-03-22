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

    Closure dirCopier = { File source, File target ->
        console.info "Copying contents of $source to $target"
        FileUtils.copyDirectory(source, target)
    }

    // copy Groovy and Java sources
    ['java', 'groovy'].each { String srcDirName ->
        copyPath(dirCopier, grailsSettings.sourceDir.path, srcDirName, targetGroovySrcDir.path)
    }

    // copy grails-app
    copyPath(dirCopier, baseDirPath, 'grails-app', targetDirPath, 'grails-app')

    String sourceTestsBase = grailsSettings.testSourceDir.path

    // copy the tests
    copyPath(dirCopier, sourceTestsBase, 'unit', targetDirPath, 'src/test/groovy')
    copyPath(dirCopier, sourceTestsBase, 'integration', targetDirPath, 'src/integration-test/groovy')
    copyPath(dirCopier, sourceTestsBase, 'functional', targetDirPath, 'src/integration-test/groovy')

    // copy web-app and scripts
    copyPath(dirCopier, baseDirPath, 'web-app', targetDirPath, 'src/main/webapp')
    copyPath(dirCopier, baseDirPath, 'scripts', targetDirPath, 'src/main/scripts')

    File targetConfigDir = new File(targetDirPath, 'grails-app/conf')

    // move various files
    Closure fileMover = { File source, File target ->
        FileUtils.deleteQuietly(target)
        console.info "Moving $source to $target"
        FileUtils.moveFile(source, target)
    }

    copyPath(fileMover, targetConfigDir.canonicalPath, 'UrlMappings.groovy', targetDirPath, 'grails-app/controllers/UrlMappings.groovy')
    copyPath(fileMover, targetConfigDir.canonicalPath, 'BootStrap.groovy', targetDirPath, 'grails-app/init/BootStrap.groovy')

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
            console.warn "Unable to migrate $file. The filters in this file should be implemented as interceptors in the Grails 3 project"
        }
    }

    Closure fileCopier = { File source, File target ->
        FileUtils.deleteQuietly(target)
        console.info "Copying file $source to $target"
        FileUtils.copyFile(source, target)
    }

    // try to copy various files that are reasonably likely to exist in the project root
    ['README.md', 'LICENSE'].each { String rootFileName ->
        copyPath(fileCopier, baseDirPath, rootFileName, targetDirPath, rootFileName)
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
        List<String> sourceDescriptorContent = grailsSettings.basePluginDescriptor.readLines()

        // add a package declaration, blank line, an import statement, and another blank line to the Grails 2 plugin descriptor
        String targetPackageStatement = targetDescriptorContent.find { it ==~ /\s*package\s+[a-zA-Z_$\.]+\s*/ }
        assert targetPackageStatement, "No package statement found in plugin descriptor $targetPluginDescriptor"
        sourceDescriptorContent.addAll(0, [targetPackageStatement, '', 'import grails.plugins.*',  ''])

        // replace the Grails 2.x plugin class definition, e.g.
        //      class MyGrailsPlugin {

        // with a Grails 3.x plugin class definition, e.g.
        //      class MyGrailsPlugin extends grails.plugins.Plugin {
        Pattern pluginClassDefRegex = Pattern.compile(/.*class.*\s+.*[a-zA-Z_$]+GrailsPlugin.*/)

        String grails3PluginClassName = targetPluginDescriptor.name - '.groovy'
        String grails3PluginClassDef = "$grails3PluginClassName extends Plugin"
        boolean pluginClassDeclarationMigrated = false

        for (int i = 0; i < sourceDescriptorContent.size(); i++) {
            String line = sourceDescriptorContent[i]

            if (pluginClassDefRegex.matcher(line).matches()) {
                sourceDescriptorContent[i] = line.replaceFirst(/[a-zA-Z_$]+GrailsPlugin/, grails3PluginClassDef)
                pluginClassDeclarationMigrated = true
                break
            }
        }

        assert pluginClassDeclarationMigrated, "Plugin class declaration not found in $grailsSettings.basePluginDescriptor"

        targetPluginDescriptor.withWriter { BufferedWriter writer ->
            sourceDescriptorContent.each { writer.writeLine it }
        }
    }
}

/**
 * Copies one path to another
 * @param copyTask performs the copy operation
 * @param sourceBase the base source path
 * @param sourceRelative the relative path from sourceBase to the source path
 * @param targetBase the base target path
 * @param targetRelative the relative path from targetBase to the target path. If targetBase
 * is the target path, this may be omitted
 */
void copyPath(Closure copyTask, String sourceBase, String sourceRelative, String targetBase, String targetRelative = '') {

    File source = canonicalFile(sourceBase, sourceRelative)
    if (!source.exists()) {
        console.info "$source not found in Grails 2 project - skipping"
        return
    }

    File target = canonicalFile(targetBase, targetRelative)
    copyTask(source, target)
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