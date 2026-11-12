# CHIMERA / CODEMERA #

## Quick Summary ##

For Android Studio, simply open the root repo folder. Confirm "Build Variant" before building. Note, you may need to setup [artifactory credentials](https://jhtnaee.atlassian.net/wiki/spaces/DEV/pages/1846149188/Generate+build.gradle+and+gradle.properties+Files+for+a+Repo) in your user gradle settings if you have not previously for other projects.

We currently have a hybrid of java/xml and kotlin/compose as we start to modernize certain systems.

This repo has expanded to cover a few product lines:

- matrix commercial (chimera, low-cost-gui)
- matrix retail (parker)
- lite commercial (vision)
- premium "Project One"

[More on Console Standardization](https://jhtnaee.atlassian.net/wiki/spaces/CT/pages/778338317/Building)

## Project Documentation ##

See relevant confluence pages for more detailed information.
(Add specific links here if you like)

## Android Product Flavors / Build Variants ##

Some of the projects use [product flavors](https://developer.android.com/studio/build/build-variants) in gradle to customize certain builds.
In Android Studio, product flavors can be selected in the "Build Variants" pop-out pane usually in the lower left border of the UI.

For updated info on the current build flavor system refer to https://jhtnaee.atlassian.net/wiki/spaces/CT/pages/1095991586/Build+Flavors

For now functional variation is managed via ProductConfig class and methods it exposes. It leverages flavor checks, OS build string checks, other BuildConfig fields, etc.

At this time we have three actual flavor dimensions, but this may be consolidated into market segment flavors as "UI" was for defunct Planet Fitness theming and "Hardware" has been mostly replaced by OS and run time hardware checking.

## Modules in this Project ##

- "Chimera" is the main console app (produces an apk)
- "UpdateService" is the console update service (produces an apk)
- "GATTServer" is the BLE server used for FTMS or other exposed services by main app
	- "gatt" contains constants and characteristic classes for data handling
	- "gattserver" is the original server based on direct android API
	- "gattserver2" is the new server based on the nordic BLE library
- "Core" contains common code for UI modularization
	- "Resources" is shared android resource files
	- "Interfaces" is the collection of classes to decouple UI from system (acts like an app API of sorts)
- "UI" contains decoupled UI modules as we create them
- "Library" contains some submodules used by main app
	- "RFID2" is the code for 'RFID 2.0'

## Architecture Notes ##

- In addition to some modules in the repo, there are [artifacts](https://jhtnaee.atlassian.net/wiki/spaces/CT/pages/739311624/Android+Artifacts) used from the company's [artifactory.](https://jht-artifacts.com:8081/artifactory/webapp/#/home)
- Main app heavily relies on the "DataMatrix" library for IPC and application state. It is a content provider that acts a subscriber-publisher system for data and events. It is imported as an artifact.
- There is a "translations" artifact that provides user facing [translated string resources.](https://jhtnaee.atlassian.net/wiki/spaces/CT/pages/1188954319/Strings+Translation+Management+Android)
- App versioning comes from ./buildSrc/src/main/groovy/com/jht/Master.groovy -> getVersion() which attempts to pull the version properties file used by deploy scripting. You should not need to touch it unless you need custom version strings for some internal purpose. See the deploy system for handling deploy versioning.

## Deploy Scripting ##

- "./Deploy/" contains files used in packaging such as update shell scripts and external assets
- "./buildSrc/" contains the groovy classes used to generate tasks used by build server
	- DO NOT run the scripts locally unless you have [configured local properties file](https://jhtnaee.atlassian.net/wiki/spaces/CT/pages/1095991586/Build+Flavors#Deploy-Scripts) with overrides to Z path and S3 bypass to avoid altering the public releases!
	- Main build tasks:
		- Compute version - updates version numbers in build number tracking properties file
		- Build - assembles the our apks
		- Publish - constructs the zip packages and XML manifests, uploads them, optional deploys to CORE
	- "FlavorDeployBase" contains base version numbers. Three field strings use auto incrementing build numbers tracked by properties file, and four field strings are used as-is.
	- The "deployments" subfolder contains flavor specific classes for assembling manifests and packages.
	- SDC is currently determined by the first two fields of the app version string.
	- Other gradle tasks
		- There are "install" tasks to help get certain things installed onto a console via ADB like the assets or third party apps (TPAs)
		- There are "deployPurge" tasks to help garbage collect the builds stored on the network releases location by moving versions not covered by a matching tracking prefix in its flavor deployment class to another folder for deletion.
		- "TPA manifest" tasks are for the optional "strict TPA" management which ensures packages are what are expected and populates a JSON file in the deployment for enhanced tracking and testing options.

## Interfacing API Documentation ##

The "Core/Interfaces" module contains model classes that are to be used as a sort of "app API" to decouple our "apps" from the rest of the system. As this API must be documented, we are presently using KDoc and dokka to document it in comment blocks to ensure that this process is not skipped and reflects up-to-date information. At present, the priority is the base model classes themselves.

Dokka adds doc generation gradle tasks under the "documentation" category of the top level "Tasks" category. By default it provides generators for git-flavored-markdown and HTML (as well as Javadoc and jekyll). The generated output (unless reconfigured) is placed under the modules "build" folder in a subfolder called "dokka".

The official filing process of this documentation is TBD. We might copy markdown into confluence pages, or consider placing the HTML somewhere. We could even go so far as to devise some clever scripting or a custom dokka generator to streamline a doc release process.

For module/package level documentation, see "module.md" in the root folder of the module. The custom logo for HTML generation is also stored there.

## Other Notes ##

Tools folder contains helpful Windows and Linux developer/tester scripts.

### PERS / NO PERS ###

If you see the red PERS text on the top bar, it means that persistent process is enabled, so every time you push, you have to reboot. You can run the "persistent_off" ADB script from ./Tools/ to disable persistent processes (it requires a reboot). The persistence basically keeps our processes running all the time. Note that some parts of our formal update process assume that this is running. The red indicator might show only under certain conditions in "release" builds as it is meant more for debug purposes.

### Emulator ###

There are some confluence pages available for setting up emulators to run these projects.
If using the stock android emulator, keep in mind that most third party apps do not support x86.
At present, you will need to run console app in demo mode unless an LCB simulator is available to it.
Code that cannot run on emulator should probably have an OS/hardware check or "isEmulator" check or something protecting it.
