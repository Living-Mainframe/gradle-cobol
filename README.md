# Gradle COBOL Plugin #

COBOL continues to power many core and critical applications at banks, insurers, federal agencies (just to name a few).
Building a COBOL application requires custom scripts or commercial products. To this date no open build system exists 
which lets one easily build the source into an executable application. This project is going to change this.

This plugin is a work in progress and not suitable for a productive development process. Some initial features exist and
work and many others are planned.

## Installation ##

At this point, the plugin can only be included from a local Maven repository. On the same machine that you want to test
the functionality, clone this repository and deploy the plugin to the local repository by running:
`./gradlew publishToMavenLocal`

Once a stable-enough version exists we are going to try to publish it to the Gradle Plugin Portal for easier
distribution.

## Variants ##

The plugin is built in a way that it should be easy to extend it to different COBOL compilers. For
testing and local development the GNU COBOL compiler can be used. For that purpose the `gnu-cobol` variant of the plugin
is created. A version for the IBM Enterprise COBOL for z/OS compiler also exists: `ibm-enterprise-cobol`. 
The idea is that the core functionality (like identifying dependencies, setting up build tasks, etc.) is coded once. 
Specifics, like compiler options, executable locations, etc. can be added through the specific variants.

## Usage ##

To use this plugin, create a new Gradle project that contains your COBOl code and apply the plugin in the
`build.gradle.kts`:

```kotlin
plugins {
    id("de.living-mainframe.gnu-cobol") version ("0.1.0")
}

repositories {
    mavenLocal()
    mavenCentral()
}
```

The plugin expects the source code in `src/cobol` and the copybooks in `src/copy`. These locations can be changed or
others may be added. Note: The `cobol {}` is an extension which can be coded in the `build.gradle.kts` to modify the
plugin. IntelliJ supports content assist and type-checking to make sure the typed input is correct. VS Code
unfortunately doesn't support this.

```kotlin
cobol {
    sourcePaths.add(providers.provider {
        layout.projectDirectory.dir("src").dir("cobolprograms").asFile.toPath()
    })
    sourceIncludePaths.add(providers.provider {
        layout.projectDirectory.dir("src/dclgen").asFile.toPath()
    })
}
```

A build task is generated for each COBOL program:
`./gradlew build-hellocob`

So far, a `./gradlew build` doesn't work out of the box. This can simply be added by registering the build tasks with
the base plugin's `assemble` task: 
```kotlin
plugins {
    base
    id("de.living-mainframe.gnu-cobol") version ("0.1.0")
}

tasks.named("assemble").configure{
    dependsOn(tasks.withType<de.livingmainframe.plugins.cobol.core.tasks.DynamicBuildTask>())
}
```

## Contributing to this Plugin ##

We value your interest in this project and your contributions are highly welcome. Because this is a new open source
project and also the first Gradle plugin developed by the original authors, things may not be very smooth in the
beginning when it comes to contributions.

All feedback is welcome at this point. The main focus will be on creating a foundation that we can build new features
on.

We are specifically looking for contributions that address:
- Improving lazy evaluation of the project and its configuration
- Establishing a clear (and eventually stable) interface between the core functionality and the variants
- Simplifying the code where applicable
- Removing code smells
- Migrating to JSpecify annotations and cleaning up nullability issues that now show up with Gradle 9
- General feature request from COBOL developers that want to use this plugin

Please always open an issue to discuss your changes first.

Comments and TODOs exist in the source code which hint possible changes that you can work on.

## z/OS and Wazi-as-a-Service Setup ##

Gradle requires some z/OS-specific configuration to properly function.

To make sure the Gradle daemon and client start in UTF-8, set the file encoding. This is necessary because Gradle
internally requires UTF-8 or something that's compatible. This has been addressed at Gradle but there is no timeline
for implementation: https://github.com/gradle/gradle/issues/24498#issuecomment-2180552016

The `OPENSHIFT_IP` is necessary because of how the network stack is configured on z/OS. This may be specific for
Wazi-as-a-Service, but I had to misuse the `OPENSHIFT_IP` configuration option to let Gradle know where the daemon is
running. Else, the client can't connect to the daemon.

```shell
export GRADLE_OPTS="-Dfile.encoding=UTF-8" OPENSHIFT_IP="127.0.0.1"
```

I haven't dug deeper into this, but it seems that the following tags/encodings are required for the build gradle files.
This can (and should) be specified in an appropriate `.gitattributes`.

```shell
chtag -t -c UTF-8 gradlew
chtag -t -c ISO8859-1 settings.gradle.kts build.gradle.kts 
```

Gradle takes up more disk space than what is available on Wazi-as-a-Service by default. Thus, it is necessary to create
a file system with more space that is mounted at `~/.gradle`.

A ZFS must be allocated:

````shell
zfsadm define --agregate OMVS.USER.IBMUSER.GRADLE.ZFS -megabytes 1000
zfsadm format --aggregate OMVS.USER.IBMUSER.GRADLE.ZFS
mkdir /u/ibmuser/.gradle
mount -t ZFS -f OMVS.USER.IBMUSER.GRADLE.ZFS /u/ibmuser/.gradle
````
