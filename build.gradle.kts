import groovy.json.JsonSlurper
import org.apache.tools.ant.taskdefs.condition.Os
import org.w3c.dom.Element
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.io.path.Path
import kotlin.io.path.name

plugins {
    base
}

group = "net.sourceforge.plantuml"
version = "1.0.0-SNAPSHOT"
description = "PlantUML composite update site"


fun readPomProperty(propertyName: String, pomFile: File): String? {
    if (propertyName.isBlank()) {
        throw IllegalArgumentException()
    }

    val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    val pomDocument = docBuilder.parse(pomFile)
    pomDocument.documentElement.normalize()

    val propertiesList = pomDocument.getElementsByTagName("properties")
    if (propertiesList.length != 1) {
        return null
    }

    val propertiesElement = propertiesList.item(0) as Element
    val nodeList = propertiesElement.getElementsByTagName(propertyName)
    if (nodeList.length == 1) {
        return nodeList.item(0).textContent.trim()
    }
    return null
}

fun readLatestPlantUmlLibReleaseVersion(): String {
    var latestReleaseVersion: String? = null

    val connection = URI("https://api.github.com/repos/plantuml/plantuml/releases/latest")
        .toURL().openConnection() as HttpURLConnection
    connection.requestMethod = "GET"

    var responseJson : String? = null
    try {
        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            responseJson = reader.readText()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    if (responseJson != null) {
        latestReleaseVersion = readVersionFromRelease(responseJson)
    }

    if (latestReleaseVersion == null) {
        throw GradleException("Could not read PlantUml library version!")
    }

    return latestReleaseVersion
}

fun readVersionFromRelease(releaseJson: String): String {
    val json = JsonSlurper().parseText(releaseJson) as Map<*, *>
    return json["tag_name"].toString()
}

fun readCurrentPlantUML4EVersionFromPom(): String {
    val parentPom = file("$plantUml4ERootDir/releng/net.sourceforge.plantuml.parent/pom.xml")
    val plantUml4EVersion = readPomProperty("releaseVersion", parentPom)
    return plantUml4EVersion ?: "unknown"
}

fun downloadFile(sourceFileUrl : String, destinationDirectoryPath : String) {
    val uri = URI(sourceFileUrl)
    val fileName = Path(uri.path).name
    val destinationFile = File(destinationDirectoryPath, fileName)

    createDirs(destinationFile.parentFile)

    ant.invokeMethod("get", mapOf("src" to sourceFileUrl, "dest" to destinationFile))
}

// for some reason File.mkDirs() doesn't always work, thus, this work-around is needed
fun createDirs(directory: File) {
    if (directory.parentFile != null && !directory.parentFile.exists()) {
        createDirs(directory.parentFile)
    }
    if (!directory.exists()) {
        directory.mkdir()
    }
}


val plantUmlLibRootDir = "plantuml-lib"
val plantUmlLibPluginName = "net.sourceforge.plantuml.library"
val plantUmlLibFeatureName = "$plantUmlLibPluginName.feature"
val plantUmlLibRepositoryName = "$plantUmlLibPluginName.repository"
val plantUmlLibPluginDir = "$plantUmlLibRootDir/$plantUmlLibPluginName"
val plantUmlLibFeatureDir = "$plantUmlLibRootDir/$plantUmlLibFeatureName"
val plantUmlLibRepositoryDir = "$plantUmlLibRootDir/$plantUmlLibRepositoryName"
val plantUmlLibPluginLibDir = "$plantUmlLibPluginDir/lib"
val latestPlantUmlLibReleaseVersion =  readLatestPlantUmlLibReleaseVersion()
val latestPlantUmlLibReleaseVersionSimple = latestPlantUmlLibReleaseVersion.substringAfter("v")

val plantUml4ERootDir = "plantuml4eclipse"
val plantUml4EParentDir = "$plantUml4ERootDir/releng/net.sourceforge.plantuml.parent"
val plantUml4ERepositoryDir = "$plantUml4ERootDir/releng/net.sourceforge.plantuml.repository"
val plantUml4EVersion = readCurrentPlantUML4EVersionFromPom()

val buildDirectoyPath = project.layout.buildDirectory.get().toString()

val mvnCmd = if (Os.isFamily(Os.FAMILY_WINDOWS)) { "mvn.cmd" } else { "mvn"}
val gitCmd = if (Os.isFamily(Os.FAMILY_WINDOWS)) { "git.exe" } else { "git"}

val setEnvVarPlantUmlLibVersionTask = tasks.register("printPlantUmlLibVersion") {
    group = "plantuml-lib"

    doLast {
        println(latestPlantUmlLibReleaseVersionSimple)
    }
}

// download latest PlantUML EPL lib to build/lib
val downloadPlantUmlLibsTask = tasks.register("downloadPlantUmlLibs") {
    group = "plantuml-lib"

    outputs.dir(project.layout.buildDirectory.dir("lib"))

    doLast {
        println("#################################################################################")
        println("Using PlantUML library version: $latestPlantUmlLibReleaseVersion")
        println("#################################################################################")

        downloadFile("https://github.com/plantuml/plantuml/releases/download/$latestPlantUmlLibReleaseVersion/plantuml-epl-$latestPlantUmlLibReleaseVersionSimple.jar", "build/lib")
        downloadFile("https://github.com/plantuml/plantuml/releases/download/$latestPlantUmlLibReleaseVersion/plantuml-epl-$latestPlantUmlLibReleaseVersionSimple-sources.jar", "build/lib")
    }
}

// delete *.jar in plantuml-lib\net.sourceforge.plantuml.library\lib
val deleteObsoleteLibsTask = tasks.register<Delete>("clearEclipsePluginLibDir") {
    group = "plantuml-lib"

    delete(fileTree(plantUmlLibPluginLibDir).include("*.jar"))
}

// copy build/lib/*.jar to plantuml-lib\net.sourceforge.plantuml.library\lib
val copyLibsTask = tasks.register<Copy>("copyLibsToEclipsePlugin") {
    group = "plantuml-lib"

    dependsOn(downloadPlantUmlLibsTask)
    dependsOn(deleteObsoleteLibsTask)

    inputs.dir(project.layout.buildDirectory.dir("lib"))

    from("build/lib")
    into(plantUmlLibPluginLibDir)
    include("*.jar")
}

// set version in plantuml-lib\net.sourceforge.plantuml.library\META-INF\MANIFEST.MF to that of latest PlantUML lib
// place modified file to build/eclipse-files/
val updateVersionsInManifestTask = tasks.register<Copy>("updateVersionsInManifest") {
    group = "plantuml-lib"

    outputs.dir(project.layout.buildDirectory.dir("eclipse-files"))

    from("$plantUmlLibPluginDir/META-INF") {
        include("MANIFEST.MF")
        filter { line: String ->
            if (line.startsWith("Bundle-Version:")) {
                "Bundle-Version: $latestPlantUmlLibReleaseVersionSimple.qualifier"
            }
            else if (line.startsWith("Bundle-ClassPath: lib/plantuml-epl-")) {
                "Bundle-ClassPath: lib/plantuml-epl-$latestPlantUmlLibReleaseVersionSimple.jar"
            }
            else line
        }
    }
    into("build/eclipse-files/$plantUmlLibPluginName/META-INF")
    filteringCharset = "UTF-8"
}

// set version in plantuml-lib\net.sourceforge.plantuml.library\.classpath & ...\build.properties to that of latest PlantUML lib
// place modified files to build/eclipse-files/
val updateVersionsInClasspathTask = tasks.register<Copy>("updateVersionsInClasspath") {
    group = "plantuml-lib"

    outputs.dir(project.layout.buildDirectory.dir("eclipse-files"))

    val linePrefix = "<classpathentry exported=\"true\" kind=\"lib\" path=\"lib/plantuml-epl-"

    from(plantUmlLibPluginDir) {
        include(".classpath")
        filter { line: String ->
            if (line.contains(linePrefix, false)) {
                val start = line.indexOf(linePrefix) + linePrefix.length
                val end = line.indexOf(".jar\"")
                line.substring(0, start) + latestPlantUmlLibReleaseVersionSimple + line.substring(end)
            }
            else line
        }
    }

    from(plantUmlLibPluginDir) {
        include("build.properties")
        filter { line: String ->
            if (line.startsWith("bin.includes = lib/plantuml-epl-")) {
                "bin.includes = lib/plantuml-epl-$latestPlantUmlLibReleaseVersionSimple.jar,\\"
            }
            else if (line.startsWith("src.includes = lib/plantuml-epl-")) {
                "src.includes = lib/plantuml-epl-$latestPlantUmlLibReleaseVersionSimple-sources.jar,\\"
            }
            else line
        }
    }
    into("build/eclipse-files/$plantUmlLibPluginName")
    filteringCharset = "UTF-8"
}

// set version in plantuml-lib\net.sourceforge.plantuml.library.feature\feature.xml to that of latest PlantUML lib
// place modified file to build/eclipse-files/
val updateVersionInFeatureTask = tasks.register<Copy>("updateVersionInFeature") {
    group = "plantuml-lib"

    outputs.dir(project.layout.buildDirectory.dir("eclipse-files"))

    from(plantUmlLibFeatureDir) {
        include("feature.xml")
        filter { line: String ->
            if (line.trim().startsWith("version=\"") && line.endsWith(".qualifier\"")) {
                line.substring(0, line.indexOf("\"") + 1) + latestPlantUmlLibReleaseVersionSimple + line.substring(line.indexOf(".qualifier"))
            }
            else line
        }
    }
    into("build/eclipse-files/$plantUmlLibFeatureName")
    filteringCharset = "UTF-8"
}

// set version in plantuml-lib\pom.xml to that of latest PlantUML lib
// place modified file to build/eclipse-files/
val updateVersionInParentPomTask = tasks.register<Copy>("updateVersionInPom") {
    group = "plantuml-lib"

    outputs.dir(project.layout.buildDirectory.dir("eclipse-files"))

    val startTag = "<plantuml-lib-version>"
    val endTag = "</plantuml-lib-version>"

    from(plantUmlLibRootDir) {
        include("pom.xml")
        filter { line: String ->
            if (line.trim().startsWith(startTag) && line.endsWith(endTag)) {
                line.substring(0, line.indexOf(startTag) + startTag.length) + latestPlantUmlLibReleaseVersionSimple + line.substring(line.indexOf(endTag))
            }
            else line
        }
    }
    into("build/eclipse-files")
    filteringCharset = "UTF-8"
}

// copy filtered/modified files from build/eclipse-files/ to plantuml-lib/
val updateVersionsInEclipseProjectsTask = tasks.register<Copy>("updateVersionsInEclipseProjects") {
    group = "plantuml-lib"

    inputs.dir(project.layout.buildDirectory.dir("eclipse-files"))

    dependsOn(updateVersionsInManifestTask)
    dependsOn(updateVersionsInClasspathTask)
    dependsOn(updateVersionInFeatureTask)
    dependsOn(updateVersionInParentPomTask)

    from("build/eclipse-files")
    into(plantUmlLibRootDir)
    filteringCharset = "UTF-8"
}

// build PlantUML lib projects (plug-in, feature, and update site / p2 repo) with Maven/Tycho
val buildPlantUmlLibUpdateSiteTask = tasks.register<Exec>("buildPlantUmlLibUpdateSite") {
    group = "build"

    dependsOn(copyLibsTask)
    dependsOn(updateVersionsInEclipseProjectsTask)
	
	// Work-around to force this task to run everytime
	outputs.upToDateWhen { false }

    outputs.dir(project.layout.projectDirectory.dir(plantUmlLibRootDir))

    workingDir = file(plantUmlLibRootDir).absoluteFile

    commandLine = listOf(mvnCmd, "--batch-mode", "--update-snapshots", "--errors", "--quiet", "clean", "package")
}

// git clone gh-pages branch to build/gh-pages
val cloneGhPagesTask = tasks.register<Exec>("cloneGitHubPages") {
    group = "publish"

    outputs.dir(project.layout.buildDirectory.dir("gh-pages"))

    commandLine = listOf(gitCmd, "clone", "-b", "gh-pages", "https://github.com/plantuml/plantuml-eclipse.git", "build/gh-pages")
}

// check if build/gh-pages/plantuml.lib/<latest-PlantUML-lib-version> already exists
val checkIfPlantUmlLibIsAlreadyPublishedTask = tasks.register("checkIfPlantUmlLibIsAlreadyPublished") {
    group = "publish"

    dependsOn(cloneGhPagesTask)

    doLast {
        val ghPagesUpdateSiteTargetDir = File("build/gh-pages/plantuml.lib", latestPlantUmlLibReleaseVersionSimple)
        if (ghPagesUpdateSiteTargetDir.exists()) {
            throw GradleException("The PlantUML library version $latestPlantUmlLibReleaseVersionSimple has already been" +
                    " published. The files were found in directory ${ghPagesUpdateSiteTargetDir}.")
        }
    }
}

// copy composite*.xml published on GH pages to temporary build folder in order to modify them in a next step by filtering
val copyGhPagesFilesForModificationForPlantUmlLibReleaseTask = tasks.register<Copy>("copyGhPagesFilesForModificationForPlantUmlLibRelease") {
    group = "publish"

    dependsOn(cloneGhPagesTask)

    outputs.dir(project.layout.buildDirectory.dir("composite-repository"))

    from("build/gh-pages") {
        include("composite*.xml")
    }
    into("build/composite-repository")
    filteringCharset = "UTF-8"
}

// update composite*.xml: add new PlantUML lib version / update site, copy the files to build/gh-pages
val updateGhPagesFilesAddLatestPlantUmlLibTask = tasks.register<Copy>("updateGhPagesFilesAddLatestPlantUmlLib") {
    group = "publish"

    dependsOn(copyGhPagesFilesForModificationForPlantUmlLibReleaseTask)

    outputs.dir(project.layout.buildDirectory.dir("gh-pages"))

    val prefix = "<children size='"
    val suffix = "'>"
    val childrenEndTag = "</children>"

    from("build/composite-repository") {
        include("composite*.xml")
        filter { line: String ->
            if (line.trim().startsWith(prefix) && line.endsWith(suffix)) {
                val sizeText = line.substring(line.indexOf(prefix) + prefix.length, line.indexOf(suffix))
                val size = Integer.valueOf(sizeText) + 1

                line.substring(0, line.indexOf(prefix)) + prefix + size + suffix
            } else if (line.contains(childrenEndTag)) {
                val indentation = line.substring(0, line.indexOf("<")) + "\t"

                indentation + "<child location='plantuml.lib/$latestPlantUmlLibReleaseVersionSimple' />" + System.lineSeparator() + line
            }
            else line
        }
    }
    into("build/gh-pages")
    filteringCharset = "UTF-8"
}

// TODO Somehow avoid redundant code, compare updateGhPagesFilesAddLatestPlantUmlLibTask and updateGhPagesFilesAddPlantUml4ETask
// update composite-repository/repository/composite*.xml, add current PlantUML4Eclipse version / update site
// copy composite-repository/repository/*.* and README.md to build/gh-pages
val updateGhPagesFilesAddPlantUml4ETask = tasks.register<Copy>("updateGhPagesFilesAddPlantUml4Eclipse") {
    group = "publish"

    dependsOn(cloneGhPagesTask)

    outputs.dir(project.layout.buildDirectory.dir("gh-pages"))

    val prefix = "<children size='"
    val suffix = "'>"
    val childrenEndTag = "</children>"

    from("composite-repository/repository") {
        include("composite*.xml")
        filter { line: String ->
            if (line.trim().startsWith(prefix) && line.endsWith(suffix)) {
                val sizeText = line.substring(line.indexOf(prefix) + prefix.length, line.indexOf(suffix))
                val size = Integer.valueOf(sizeText) + 1

                line.substring(0, line.indexOf(prefix)) + prefix + size + suffix
            } else if (line.contains(childrenEndTag)) {
                val indentation = line.substring(0, line.indexOf("<")) + "\t"

                indentation + "<child location='plantuml.eclipse/$plantUml4EVersion' />" + System.lineSeparator() + line
            }
            else line
        }
    }
    from("composite-repository/repository") {
        include("p2.index")
    }
    from(".") {
        include("README.md")
    }
    into("build/gh-pages")
    filteringCharset = "UTF-8"
}

// copy built PlantUML lib update site to composite update site:
// from plantuml-lib\net.sourceforge.plantuml.library.repository\target\repository to build/gh-pages/plantuml.lib/<latest-PlantUML-lib-version>
val addLatestPlantUmlUpdateSiteToGhPagesTask = tasks.register<Copy>("addLatestPlantUmlUpdateSiteToGhPages") {
    group = "publish"

    dependsOn(checkIfPlantUmlLibIsAlreadyPublishedTask)
    dependsOn(buildPlantUmlLibUpdateSiteTask)

    outputs.dir(project.layout.buildDirectory.dir("gh-pages/plantuml.lib"))

    from("$plantUmlLibRepositoryDir/target/repository")
    into("build/gh-pages/plantuml.lib/$latestPlantUmlLibReleaseVersionSimple")
    filteringCharset = "UTF-8"
}

// Add new PlantUML lib update site to GitHub pages in build/gh-pages (call other tasks to do so)
val updateGhPagesContentsAddLatestPlantUmlLibTask = tasks.register("updateGhPagesContentsAddLatestPlantUmlLib") {
    group = "publish"

    dependsOn(updateGhPagesFilesAddLatestPlantUmlLibTask)
    dependsOn(addLatestPlantUmlUpdateSiteToGhPagesTask)

    doLast {
        val ghPagesUpdateSiteTargetDir = File("build/gh-pages/plantuml.lib", latestPlantUmlLibReleaseVersionSimple)
        if (!ghPagesUpdateSiteTargetDir.exists()) {
            throw GradleException("The new PlantUML library version $latestPlantUmlLibReleaseVersionSimple is missing in the build directory." +
                    " Expected the following directory to exist: $ghPagesUpdateSiteTargetDir.")
        }
    }
}

// git add everything in build/gh-pages/
val gitAddPlantUmlLibUpdateSiteToGhPagesTask = tasks.register<Exec>("gitAddPlantUmlLibUpdateSiteToGhPages") {
    group = "publish"

    dependsOn(updateGhPagesContentsAddLatestPlantUmlLibTask)

    commandLine = listOf(gitCmd, "-C", "$buildDirectoyPath/gh-pages", "add", "--all")
}

// git commit changed files for new PlantUML lib update site
val gitCommitPlantUmlLibUpdateSiteToGhPagesTask = tasks.register<Exec>("gitCommitPlantUmlLibUpdateSiteToGhPages") {
    group = "publish"

    dependsOn(gitAddPlantUmlLibUpdateSiteToGhPagesTask)

    commandLine = listOf(gitCmd, "-C", "$buildDirectoyPath/gh-pages", "commit", "-m", "Add new PlantUML lib update site version $latestPlantUmlLibReleaseVersionSimple")
}

// git push commit with new PlantUML lib update site
val gitPushPlantUmlLibUpdateSiteToGhPagesTask = tasks.register<Exec>("gitPushPlantUmlLibUpdateSiteToGhPages") {
    group = "publish"

    dependsOn(gitCommitPlantUmlLibUpdateSiteToGhPagesTask)

    commandLine = listOf(gitCmd, "-C", "$buildDirectoyPath/gh-pages", "push")
}

// build PlantUML4Eclipse projects (plug-ins, features, and update site / p2 repo) with Maven/Tycho
val buildPlantUml4EUpdateSiteTask = tasks.register<Exec>("buildPlantUml4EUpdateSite") {
    group = "build"

    // Work-around to force this task to run everytime
    outputs.upToDateWhen { false }

    outputs.dir(project.layout.projectDirectory.dir(plantUml4ERootDir))

    workingDir = file(plantUml4EParentDir).absoluteFile

    commandLine = listOf(mvnCmd, "--batch-mode", "--update-snapshots", "--errors", "clean", "package")

    doFirst {
        println("#################################################################################")
        println("Building PlantUML4Eclipse version: $plantUml4EVersion")
        println("#################################################################################")
    }
}

// check if build/gh-pages/plantuml.eclipse/<PlantUML4Eclipse-version> already exists
val checkIfPlantUml4EUpdateSiteAlreadyExistsTask = tasks.register("checkIfPlantUml4EclipseUpdateSiteAlreadyExists") {
    group = "publish"

    dependsOn(cloneGhPagesTask)

    doLast {
        val ghPagesUpdateSiteTargetDir = File("build/gh-pages/plantuml.eclipse", plantUml4EVersion)
        if (ghPagesUpdateSiteTargetDir.exists()) {
            throw GradleException("Target update site directory already exists: $ghPagesUpdateSiteTargetDir")
        }
    }
}

// copy built PlantUML4Eclipse update site to composite update site:
// from plantuml4eclipse\releng\net.sourceforge.plantuml.repository\target\repostiory to build/gh-pages/plantuml.eclipse/<PlantUML4Eclipse-version>
val addPlantUml4EUpdateSiteToGhPagesTask = tasks.register<Copy>("addPlantUml4EclipseUpdateSiteToGhPages") {
    group = "publish"

    dependsOn(buildPlantUml4EUpdateSiteTask)
    dependsOn(cloneGhPagesTask)
    dependsOn(checkIfPlantUml4EUpdateSiteAlreadyExistsTask)

    outputs.dir(project.layout.buildDirectory.dir("gh-pages"))

    val targetDirPath = "build/gh-pages/plantuml.eclipse/$plantUml4EVersion"

    from("$plantUml4ERepositoryDir/target/repository")
    into(targetDirPath)
    filteringCharset = "UTF-8"
}

// TODO Somehow avoid redundant code, compare updateGhPagesContentsAddLatestPlantUmlLibTask and updateGhPagesContentsAddPlantUml4ETask
// Add new PlantUML4Eclipse update site to GitHub pages in build/gh-pages (call other tasks to do so)
val updateGhPagesContentsAddPlantUml4ETask = tasks.register("updateGhPagesContentsAddPlantUml4Eclipse") {
    group = "publish"

    dependsOn(updateGhPagesFilesAddPlantUml4ETask)
    dependsOn(addPlantUml4EUpdateSiteToGhPagesTask)

    doLast {
        val ghPagesUpdateSiteTargetDir = File("build/gh-pages/plantuml.eclipse", plantUml4EVersion)
        if (!ghPagesUpdateSiteTargetDir.exists()) {
            throw GradleException("The new PlantUML4Eclipse version $plantUml4EVersion is missing in the build directory." +
                    " Expected the following directory to exist: $ghPagesUpdateSiteTargetDir.")
        }
    }
}

// TODO Somehow avoid redundant code, compare gitAddPlantUmlLibUpdateSiteToGhPagesTask and gitAddPlantUml4EUpdateSiteToGhPagesTask
// git add everything in build/gh-pages/
val gitAddPlantUml4EUpdateSiteToGhPagesTask = tasks.register<Exec>("gitAddPlantUml4EclipseUpdateSiteToGhPages") {
    group = "publish"

    dependsOn(updateGhPagesContentsAddPlantUml4ETask)

    commandLine = listOf(gitCmd, "-C", "$buildDirectoyPath/gh-pages", "add", "--all")
}

// TODO Somehow avoid redundant code, compare gitCommitPlantUmlLibUpdateSiteToGhPagesTask and gitCommitPlantUml4EUpdateSiteToGhPagesTask
// git commit changed files for new PlantUML4Eclipse update site
val gitCommitPlantUml4EUpdateSiteToGhPagesTask = tasks.register<Exec>("gitCommitPlantUml4EclipseUpdateSiteToGhPages") {
    group = "publish"

    dependsOn(gitAddPlantUml4EUpdateSiteToGhPagesTask)

    commandLine = listOf(gitCmd, "-C", "$buildDirectoyPath/gh-pages", "commit", "-m", "New PlantUML4Eclipse release: $plantUml4EVersion")
}

// We did not add a git push gradle task for PlantUml4Eclipse, since we want the changes to be reviewed before pushing them

tasks.findByName("build")?.dependsOn(
    buildPlantUmlLibUpdateSiteTask,
    buildPlantUml4EUpdateSiteTask
)

// TODO call mvn clean on PlantUML lib and PlantUML4Eclipse projects when gradle task clean is called, similar for build task