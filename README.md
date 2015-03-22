[![Build Status](https://travis-ci.org/domurtag/migrate-2-grails3.svg?branch=master)](https://travis-ci.org/domurtag/migrate-2-grails3)

## migrate-2-grails3
A Grails 2 plugin that performs a partial migration of a Grails 2 plugin or application to Grails 3

### Prerequisites
Grails 2.x and 3.x are installed where 2.x is the Grails version of the project being migrated, and 3.x is the target Grails version 

### Usage
- Set the current Grails version to 3.x
- Create an empty Grails 3.x project with the same name and type (application or plugin) as the Grails 2.x project being migrated
- Install this plugin in the Grails 2.x project being migrated, i.e. add the following to `BuildConfig.groovy`
replacing `${version}` with the latest version of this plugin

````
plugins {
    build ':migrate2-grails3:${version}'
    // other plugin dependencies
}
````

- Set the current Grails version to 2.x
- Execute the following command in the Grails 2.x project

`grails migrate [path-to-grails3-project]`

- Once this command has completed, you can remove the plugin from the Grails 2.x app

#### Arguments
- `path-to-grails3-project` the *relative* path from the Grails 2.x project to the Grails 3.x project

#### Example
If the Grails 2.x being migrated is located at `/home/grails2/foo` and the Grails 3.x project is at `/home/grails3/foo` execute the following
on the command-line with the current Grails version set to 2.x and the current directory set to `/home/grails2/foo`

    grails migrate ../../grails3/foo

On Windows, it may be necessary to use backslashes instead

    grails migrate ..\..\grails3\foo

### Tasks Performed Automatically
The plugin performs the tasks listed in the "Project Structure Changes" section of the [migration guide](https://grails.github.io/grails-doc/latest/guide/upgrading.html).
After following the instructions above, if the project being migrated is a plugin, steps 1-3 of the [Upgrading Plugins](https://grails.github.io/grails-doc/latest/guide/upgrading.html#upgradingPlugins)
section will also have been performed. if the project being migrated is an application, steps 1-2 of the 
[Upgrading Applications](https://grails.github.io/grails-doc/latest/guide/upgrading.html#upgradingApps) section will have been performed.

A few of other minor tasks are attempted which aren't explicitly mentioned in the migration guide, e.g. copying of
`README.md` and `LICENSE` files from the root of the Grails 2.x project to the root of the Grails 3.x project.

#### Mandatory Exceptions
Some of the steps described in the aforementioned sections of the migration guide are not performed by this plugin and must be performed manually instead:

- merging `DataSource.groovy` and `Config.groovy` into a single `application.yml` or `application.groovy` config file with log4j config removed
- configuring logging via `logback.groovy`
- migrating dependencies from `BuildConfig.groovy` to `build.gradle`
- setting the application/plugin version in `build.gradle`

#### Optional Exceptions
Some of the steps described in the aforementioned sections of the migration guide are not performed by this plugin and may need to be performed manually instead. In other words, dependending of the contents of the project being migrated, each of the following steps may not be relevant

- migrating dependencies from `lib` directory to `build.gradle`
- migrating Spring beans from `web-app/WEB-INF/applicationContext.xml` to `grails-app/conf/spring/resources.groovy`
- Customizations to `web.xml` that were applied via `src/templates/war/web.xml` must be applied via Spring in Grails 3.x
- migrating `.tld` files in `web-app/WEB-INF/tld`


#### Gant Scripts
Any Gant scripts in the `scripts` directory of the Grails 2.x project will be copied to the `src/main/scripts` directory of the Grails 3.x project. However the contents of these scripts must be migrated from Gant to Gradle, as described in step 7 of the [plugin migration guide](https://grails.github.io/grails-doc/latest/guide/upgrading.html#upgradingPlugins).

### Improvements
There are a few additional migration steps that it may be possible to automate, but I need some feedback before I can implement
them. Check the [issue list](https://github.com/domurtag/migrate-2-grails3/issues) and contribute feedback/code if you can.
