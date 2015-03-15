## migrate-2-grails3
A Grails 2 plugin that performs a partial migration of a Grails 2 plugin or application to Grails 3

### Instructions

- Create an empty Grails 3 application or plugin (depending on what you're migrating) 
- Install this plugin in the application being migrated
- On the command-line set the current directory to the root of the project being migrated and run the following command

`grails migrate <path> <plugin package name>`
    
#### Arguments

- path: the *relative* path (from the application being migrated) to the empty Grails 3 application
- plugin package name: this argument is optional and only used if a plugin is being migrated. It specifies the package
name that will be used for the plugin descriptor's class. If omitted a package name of the form `grails.plugins.${appName}`
will be used 

### Automatic Migration Tasks

The plugin automatically performs the following tasks:

- copying Java/Groovy source files to the Grails 3 project
- copying unit/integration tests to the Grails 3 project
- copying the contents of `grails-app` and `web-app` to the Grails 3 project
 
### Manual Migration Tasks

There are a number of tasks involved in migrating to Grails 3 that the plugin does *not* perform, either because
it's impossible to automate them, or because automation of these tasks has not been completed yet. These are
described below.

#### Dependencies

The repositories and dependencies defined in `grails-app/conf/BuildConfig.groovy` of the Grails 2 project will 
need to be defined in `build.gradle` of the Grails 3.x project.

#### Modify Grails Package Names

The names of Grails packages have changed in Grails 3. It doesn't appear to be possible to automatically perform
this task as there's no reliable way of deriving the Grails 3 package name from the Grails 2 package name.