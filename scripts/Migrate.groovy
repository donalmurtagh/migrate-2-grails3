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
    def pluginPackageName

    String baseDir = grailsSettings.baseDir

    if (grailsSettings.pluginProject) {
        pluginPackageName = getPluginPackageName(argsMap)
    }

    File targetDir = makeFile(baseDir, pathToTargetApp)

    if (!targetDir.directory) {
        console.error "Grails project not found at $targetDir - quitting"
        return
    }

    def targetSrcDir = makeFile(targetDir, ['src', 'main'])

    Closure copier = { File src, File target -> FileUtils.copyDirectoryToDirectory(src, target)}

    // Copy groovy source
    copyDir(copier, grailsSettings.sourceDir, 'groovy', targetSrcDir)

    // Copy java source - in 3.0.0.RC1 the java dir isn't created by create-app, so we need to make it ourselves.
    // Check for it's existence in case this changes between now and the 3.X final release
    File targetJavaSrcDir = makeFile(targetDir, ['src', 'main', 'java'])
    createDirectoryIfNotExists(targetJavaSrcDir)

    copyDir(copier, grailsSettings.sourceDir, 'java', targetSrcDir)

    // copy grails-app
    copyDir(copier, baseDir, 'grails-app', targetDir, 'grails-app')

    // copy the unit and integration tests
    def sourceTestsBase = grailsSettings.testSourceDir
    copier = { File src, File target -> FileUtils.copyDirectory(src, target)}
    copyDir(copier, sourceTestsBase, 'unit', targetDir, ['src', 'test', 'groovy'])
    copyDir(copier, sourceTestsBase, 'integration', targetDir, ['src', 'integration-test', 'groovy'])

    // copy web-app
    copyDir(copier, baseDir, 'web-app', targetDir, ['src', 'main', 'webapp'])
}

def createDirectoryIfNotExists(File dir) {
    if (!dir.directory) {
        assert dir.mkdir()
    }
}

/**
 * Copies the contents of one directory to another
 * @param copier performs the copying
 * @param srcBase File/String indicating the base source dir
 * @param srcRelative relative path from srcBase to the source dir
 * @param targetBase File/String indicating the base source dir
 * @param targetRelative relative path from targetBase to the target dir. If targetBase is the
 * target dir, this may be omitted
 * @return
 */
def copyDir(Closure copier, srcBase, srcRelative, targetBase, targetRelative = StringUtils.EMPTY) {
    def src = makeFile(srcBase, srcRelative)
    def console = GrailsConsole.instance

    if (src.directory) {
        def target = makeFile(targetBase, targetRelative)
        console.info "Copying files from $src to $target"
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
String getPluginPackageName(argsMap) {
    if (argsMap.params.size() > 1) {
        argsMap.params[1]

    } else {
        String appName = Metadata.current['app.name']

        // dashes aren't allowed in package names
        appName = StringUtils.remove(appName, '-')
        "grails.plugins.$appName"
    }
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