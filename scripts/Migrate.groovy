import grails.build.logging.GrailsConsole
import grails.util.Metadata
import org.apache.commons.lang.StringUtils

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
        }
    }

    String pathSeparator = System.getProperty('file.separator')
    String targetBaseDir = [baseDir, pathToTargetApp].join(pathSeparator)

    console.info "Source base dir is $baseDir"
    console.info "Target base dir is $targetBaseDir"
    console.info "Source dir is $grailsSettings.sourceDir"

}

Properties getApplicationProperties(grailsSettings) {
    File propertiesFile = new File("$grailsSettings.baseDir/application.properties")
    Properties applicationProperties = new Properties()
    applicationProperties.load(propertiesFile.newInputStream())
    applicationProperties
}

setDefaultTarget(migrate)