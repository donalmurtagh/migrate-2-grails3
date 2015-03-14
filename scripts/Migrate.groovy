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
    Boolean isPlugin = grailsSettings.pluginProject
    def pluginPackageName

    String baseDir = grailsSettings.baseDir

    if (isPlugin) {
        if (argsMap.params.size() > 1) {
            pluginPackageName = argsMap.params[1]

        } else {
            String appName = Metadata.current['app.name']

            // dashes aren't allowed in package names
            appName = StringUtils.remove(appName, '-')
            pluginPackageName = "grails.plugins.$appName"
            console.info "Package name for plugin desccriptor '$baseDir'"
        }
    } else {
        console.info "Source application name ${Metadata.current['app.name']}"
    }

    File targetDir = makeFile(baseDir, pathToTargetApp)

    if (!targetDir.directory) {
        console.error "Grails project not found at $targetDir - quitting"
        return
    }

    def targetSrcDir = makeFile(targetDir, ['src', 'main'])

    // Copy groovy source
    copyDir(grailsSettings.sourceDir, 'groovy', targetSrcDir)

    // Copy java source - in 3.0.0.RC1 the java dir isn't created by create-app, so we need to make it ourselves.
    // Check for it's existence in case this changes between now and the 3.X final release
    File targetJavaSrcDir = makeFile(targetDir, ['src', 'main', 'java'])
    if (!targetJavaSrcDir.directory) {
        assert targetJavaSrcDir.mkdir()
    }

    copyDir(grailsSettings.sourceDir, 'java', targetSrcDir)
}

def copyDir(srcBase, srcRelative, targetBase, targetRelative = StringUtils.EMPTY) {
    def src = makeFile(srcBase, srcRelative)
    def target = makeFile(targetBase, targetRelative)
    GrailsConsole.getInstance().info "Copying files from $src to $target"
    FileUtils.copyDirectoryToDirectory(src, target)
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