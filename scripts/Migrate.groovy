import grails.build.logging.GrailsConsole
import grails.util.Metadata
import org.apache.commons.lang.StringUtils
import  org.apache.commons.io.FileUtils

includeTargets << grailsScript("_GrailsInit")
includeTargets << grailsScript("_GrailsArgParsing")

target(migrate: "Migrates a Grails 2.X app or plugin to Grails 3") {

    depends(parseArguments)

    def console = GrailsConsole.getInstance()

    String pathToTargetApp = argsMap.params[0]

    String baseDir = grailsSettings.baseDir
    File targetDir = makeFile(baseDir, pathToTargetApp)

    if (!targetDir.directory) {
        console.error "Grails 3 project not found at $targetDir - quitting"
        return
    }

    def targetSrcDir = makeFile(targetDir, ['src', 'main'])

    Closure copier = { File src, File target -> FileUtils.copyDirectoryToDirectory(src, target)}

    // copy groovy source
    copyDir(copier, grailsSettings.sourceDir, 'groovy', targetSrcDir)

    // copy grails-app
    copyDir(copier, baseDir, 'grails-app', targetDir, 'grails-app')

    def sourceTestsBase = grailsSettings.testSourceDir
    copier = { File src, File target -> FileUtils.copyDirectory(src, target)}

    // copy java source
    copyDir(copier, grailsSettings.sourceDir, 'java', targetSrcDir, 'groovy')

    // copy the tests
    copyDir(copier, sourceTestsBase, 'unit', targetDir, ['src', 'test', 'groovy'])
    copyDir(copier, sourceTestsBase, 'integration', targetDir, ['src', 'integration-test', 'groovy'])
    copyDir(copier, sourceTestsBase, 'functional', targetDir, ['src', 'integration-test', 'groovy'])

    // copy web-app and scripts
    copyDir(copier, baseDir, 'web-app', targetDir, ['src', 'main', 'webapp'])
    copyDir(copier, baseDir, 'scripts', targetDir, ['src', 'main', 'scripts'])

    // copy various individual files
    copyFile(baseDir, ['grails-app', 'conf', 'UrlMappings.groovy'], targetDir, ['grails-app', 'controllers', 'UrlMappings.groovy'])
    copyFile(baseDir, ['grails-app', 'conf', 'BootStrap.groovy'], targetDir, ['grails-app', 'init', 'BootStrap.groovy'])

    if (grailsSettings.pluginProject) {
        List<String> pluginPackageName = getPluginPackageName(argsMap)
        String pathSeparator = System.getProperty('file.separator')

        // add the package statement to the plugin
        List<String> pluginDescriptorContent = grailsSettings.basePluginDescriptor.readLines()
        pluginDescriptorContent.add(0, "package ${pluginPackageName.join('.')}")
    }
}

def createDirectoryIfNotExists(File dir) {
    if (!dir.directory) {
        assert dir.mkdir()
    }

    dir
}


/**
 * Copies a file
 * @param srcBase File/String indicating the base source dir
 * @param srcRelative String/List<String> indicating the relative path from srcBase to the source file
 * @param targetBase File/String indicating the base target dir
 * @param targetRelative String/List<String> indicating the relative path from targetBase to the target file. If targetBase
 * is the target file, this may be omitted
 */
void copyFile(srcBase, srcRelative, targetBase, targetRelative = StringUtils.EMPTY) {

    def src = makeFile(srcBase, srcRelative)
    def console = GrailsConsole.instance

    if (src.file) {
        def target = makeFile(targetBase, targetRelative)
        console.info "Copying $src to $target"
        FileUtils.copyFile(src, target)

    } else {
        console.warn "File $src not found in Grails 2 project - skipping"
    }
}

/**
 * Copies the contents of one directory to another
 * @param copier performs the copying
 * @param srcBase File/String indicating the base source dir
 * @param srcRelative String/List<String> indicating the relative path from srcBase to the source dir
 * @param targetBase File/String indicating the base target dir
 * @param targetRelative String/List<String> indicating the relative path from targetBase to the target dir. If targetBase
 * is the target dir, this may be omitted
 */
void copyDir(Closure copier, srcBase, srcRelative, targetBase, targetRelative = StringUtils.EMPTY) {
    def src = makeFile(srcBase, srcRelative)
    def console = GrailsConsole.instance

    if (src.directory) {
        def target = makeFile(targetBase, targetRelative)
        console.info "Copying contents of $src to $target"
        copier(src, target)
    } else {
        console.warn "Directory $src not found in Grails 2 project - skipping"
    }
}

/**
 * Returns the package name that will be used for the plugin descriptor class
 * @param argsMap
 * @return
 */
List<String> getPluginPackageName(argsMap) {

    String packageName

    if (argsMap.params.size() > 1) {
        packageName = argsMap.params[1]

    } else {
        String appName = Metadata.current['app.name']

        // dashes aren't allowed in package names
        appName = StringUtils.remove(appName, '-')
        packageName = "grails.plugins.$appName"
    }

    // package name regex stolen from http://stackoverflow.com/a/5205467
    assert packageName ==~ /([\p{L}_\u0024][\p{L}\p{N}_\u0024]*\.)*[\p{L}_\u0024][\p{L}\p{N}_\u0024]*/, "Invalid package name '$packageName' for plugin descriptor"
    packageName.tokenize('.')
}

/**
 * Construct a canonical file from a base file or path and a relative path from it
 * @param base a File or path (String) to a file
 * @param relative a String path or List of path components that are joined to form the relative path
 * @return
 */
File makeFile(base, relative) {

    String pathSeparator = System.getProperty('file.separator')

    if (relative instanceof List) {
        relative = relative.join(pathSeparator)
    }
    String relativePath = [base.toString(), relative].join(pathSeparator)
    new File(relativePath).canonicalFile
}

setDefaultTarget(migrate)