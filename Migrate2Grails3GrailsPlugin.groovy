class Migrate2Grails3GrailsPlugin {

    // the plugin version
    def version = "0.1"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "2.0 > *"
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
        "grails-app/views/error.gsp"
    ]

    // TODO Fill in these fields
    def title = "Migrate2 Grails3 Plugin" // Headline display name of the plugin
    def author = "Donal Murtagh"
    def authorEmail = "domurtag@yahoo.co.uk"
    def description = '''Performs a partial migration of a Grails 2 plugin or app to Grails 3'''

    // URL to the plugin's documentation
    def documentation = "https://github.com/domurtag/migrate-2-grails3/blob/master/README.md"

    // License: one of 'APACHE', 'GPL2', 'GPL3'
    def license = "APACHE"

    // Location of the plugin's issue tracker.
    def issueManagement = [ system: "GitHub", url: "https://github.com/domurtag/migrate-2-grails3/issues" ]

    // Online location of the plugin's browseable source code.
    def scm = [ url: "https://github.com/domurtag/migrate-2-grails3" ]

    def doWithWebDescriptor = { xml ->
        // TODO Implement additions to web.xml (optional), this event occurs before
    }

    def doWithSpring = {
        // TODO Implement runtime spring config (optional)
    }

    def doWithDynamicMethods = { ctx ->
        // TODO Implement registering dynamic methods to classes (optional)
    }

    def doWithApplicationContext = { ctx ->
        // TODO Implement post initialization spring config (optional)
    }

    def onChange = { event ->
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    def onConfigChange = { event ->
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }

    def onShutdown = { event ->
        // TODO Implement code that is executed when the application shuts down (optional)
    }
}
