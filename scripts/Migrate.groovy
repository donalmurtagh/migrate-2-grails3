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

    String pathSeparator = System.getProperty('file.separator')
    File targetDir = makeFile(baseDir, pathToTargetApp)

    if (!targetDir.directory) {
        console.error "Grails project not found at $targetDir - quitting"
        return
    }

    // copy groovy source
    File groovySrcDir = makeFile(grailsSettings.sourceDir, 'groovy')
    File groovyTargetDir = makeFile(targetDir, ['src', 'main', 'groovy'])

    console.info "Copying files from $groovySrcDir to $groovyTargetDir"
    FileUtils.copyDirectoryToDirectory(groovySrcDir, groovyTargetDir)
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

Properties getApplicationProperties(grailsSettings) {
    File propertiesFile = new File("$grailsSettings.baseDir/application.properties")
    Properties applicationProperties = new Properties()
    applicationProperties.load(propertiesFile.newInputStream())
    applicationProperties
}

setDefaultTarget(migrate)