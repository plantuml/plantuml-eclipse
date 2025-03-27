import groovy.json.JsonSlurper
import org.apache.tools.ant.taskdefs.condition.Os
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import kotlin.io.path.Path
import kotlin.io.path.name

plugins {
    base
}

group = "net.sourceforge.plantuml"
version = "1.0.0-SNAPSHOT"
description = "PlantUML composite update site"


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
val pluginName = "net.sourceforge.plantuml.library"
val featureName = "$pluginName.feature"
val repositoryName = "$pluginName.repository"
val plantUmlLibPluginDir = "$plantUmlLibRootDir/$pluginName"
val plantUmlLibFeatureDir = "$plantUmlLibRootDir/$featureName"
val plantUmlLibRepositoryDir = "$plantUmlLibRootDir/$repositoryName"
val plantUmlLibPluginLibDir = "$plantUmlLibPluginDir/lib"
val latestPlantUmlLibReleaseVersion =  readLatestPlantUmlLibReleaseVersion()
val latestPlantUmlLibReleaseVersionSimple = latestPlantUmlLibReleaseVersion.substringAfter("v")

val buildDirectoyPath = project.layout.buildDirectory.get().toString()

val mvnCmd = if (Os.isFamily(Os.FAMILY_WINDOWS)) { "mvn.cmd" } else { "mvn"}
val gitCmd = if (Os.isFamily(Os.FAMILY_WINDOWS)) { "git.exe" } else { "git"}

val downloadPlantUmlLibsTask = tasks.register("downloadPlantUmlLibs") {
    group = "plantuml-lib"

    doLast {
        println("#################################################################################")
        println("Using PlantUML library version: $latestPlantUmlLibReleaseVersion")
        println("#################################################################################")

        downloadFile("https://github.com/plantuml/plantuml/releases/download/$latestPlantUmlLibReleaseVersion/plantuml-epl-$latestPlantUmlLibReleaseVersionSimple.jar", "build/lib")
        downloadFile("https://github.com/plantuml/plantuml/releases/download/$latestPlantUmlLibReleaseVersion/plantuml-epl-$latestPlantUmlLibReleaseVersionSimple-sources.jar", "build/lib")
    }
}

val deleteObsoleteLibsTask = tasks.register<Delete>("clearEclipsePluginLibDir") {
    group = "plantuml-lib"

    delete(fileTree(plantUmlLibPluginLibDir).include("*.jar"))
}

val copyLibsTask = tasks.register<Copy>("copyLibsToEclipsePlugin") {
    group = "plantuml-lib"

    dependsOn(downloadPlantUmlLibsTask)
    dependsOn(deleteObsoleteLibsTask)

    from("build/lib")
    into(plantUmlLibPluginLibDir)
    include("*.jar")
}

val updateVersionsInManifestTask = tasks.register<Copy>("updateVersionsInManifest") {
    group = "plantuml-lib"

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
    into("build/eclipse-files/$pluginName/META-INF")
    filteringCharset = "UTF-8"
}

val updateVersionsInClasspathTask = tasks.register<Copy>("updateVersionsInClasspath") {
    group = "plantuml-lib"

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
    into("build/eclipse-files/$pluginName")
    filteringCharset = "UTF-8"
}

val updateVersionInFeatureTask = tasks.register<Copy>("updateVersionInFeature") {
    group = "plantuml-lib"

    from(plantUmlLibFeatureDir) {
        include("feature.xml")
        filter { line: String ->
            if (line.trim().startsWith("version=\"") && line.endsWith(".qualifier\"")) {
                line.substring(0, line.indexOf("\"") + 1) + latestPlantUmlLibReleaseVersionSimple + line.substring(line.indexOf(".qualifier"))
            }
            else line
        }
    }
    into("build/eclipse-files/$featureName")
    filteringCharset = "UTF-8"
}

val updateVersionInParentPomTask = tasks.register<Copy>("updateVersionInPom") {
    group = "plantuml-lib"

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

val updateVersionsInEclipseProjectsTask = tasks.register<Copy>("updateVersionsInEclipseProjects") {
    group = "plantuml-lib"

    dependsOn(updateVersionsInManifestTask)
    dependsOn(updateVersionsInClasspathTask)
    dependsOn(updateVersionInFeatureTask)
    dependsOn(updateVersionInParentPomTask)

    from("build/eclipse-files")
    into(plantUmlLibRootDir)
    filteringCharset = "UTF-8"
}

val buildPlantUmlLibUpdateSiteTask = tasks.register<Exec>("buildPlantUmlLibUpdateSite") {
    group = "build"

    dependsOn(copyLibsTask)
    dependsOn(updateVersionsInEclipseProjectsTask)

    workingDir = file(plantUmlLibRootDir).absoluteFile

    // Add --quiet argument?
    commandLine = listOf(mvnCmd, "--batch-mode", "--errors", "clean", "package")
}

//val cloneGhPagesTask = tasks.register<Exec>("cloneGitHubPages") {
//    group = "publish"
//
//    commandLine = listOf(gitCmd, "clone", "-b", "gh-pages", "git@github.com:plantuml/plantuml-eclipse.git", "build/gh-pages")
//}

val checkIfPlantUmlLibIsAlreadyPublishedTask = tasks.register("checkIfPlantUmlLibIsAlreadyPublished") {
    group = "publish"

    //dependsOn(cloneGhPagesTask)

    doLast {
        val ghPagesUpdateSiteTargetDir = File("build/gh-pages/plantuml.lib", latestPlantUmlLibReleaseVersionSimple)
        if (ghPagesUpdateSiteTargetDir.exists()) {
            throw GradleException("The PlantUML library version $latestPlantUmlLibReleaseVersionSimple has already been" +
                    " published. The files were found in directory ${ghPagesUpdateSiteTargetDir}.")
        }
    }
}

val updateGhPagesFilesTask = tasks.register<Copy>("updateGhPagesFiles") {
    group = "publish"

    //dependsOn(cloneGhPagesTask)

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

                indentation + "<child location='plantuml.lib/$latestPlantUmlLibReleaseVersionSimple' />" + System.lineSeparator() + line
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

val addLatestPlantUmlUpdateSiteToGhPagesTask = tasks.register<Copy>("addLatestPlantUmlUpdateSiteToGhPages") {
    group = "publish"

    dependsOn(checkIfPlantUmlLibIsAlreadyPublishedTask)
    dependsOn(buildPlantUmlLibUpdateSiteTask)

    from("$plantUmlLibRepositoryDir/target/repository")
    into("build/gh-pages/plantuml.lib/$latestPlantUmlLibReleaseVersionSimple")
    filteringCharset = "UTF-8"
}

val gitAddPlantUmlLibUpdateSiteToGhPagesTask = tasks.register<Exec>("gitAddPlantUmlLibUpdateSiteToGhPages") {
    group = "publish"

    dependsOn(updateGhPagesFilesTask)
    dependsOn(addLatestPlantUmlUpdateSiteToGhPagesTask)

    commandLine = listOf(gitCmd, "-C", "$buildDirectoyPath/gh-pages", "add", "--all")
}

val gitCommitPlantUmlLibUpdateSiteToGhPagesTask = tasks.register<Exec>("gitCommitPlantUmlLibUpdateSiteToGhPages") {
    group = "publish"

    dependsOn(gitAddPlantUmlLibUpdateSiteToGhPagesTask)

    commandLine = listOf(gitCmd, "-C", "$buildDirectoyPath/gh-pages", "commit", "-m", "Add new PlantUML lib update site version $latestPlantUmlLibReleaseVersionSimple")
}

val gitPushPlantUmlLibUpdateSiteToGhPagesTask = tasks.register<Exec>("gitPushPlantUmlLibUpdateSiteToGhPages") {
    group = "publish"

    dependsOn(gitCommitPlantUmlLibUpdateSiteToGhPagesTask)

    commandLine = listOf(gitCmd, "-C", "$buildDirectoyPath/gh-pages", "push")
}