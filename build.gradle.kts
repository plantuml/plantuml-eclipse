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


fun readLatestPlantUmlLibReleaseVersion(): String? {
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
val plantUmlLibPluginDir = "$plantUmlLibRootDir/$pluginName"
val plantUmlLibFeatureDir = "$plantUmlLibRootDir/$featureName"
val plantUmlLibPluginLibDir = "$plantUmlLibPluginDir/lib"
val latestPlantUmlLibReleaseVersion =  readLatestPlantUmlLibReleaseVersion()
val latestPlantUmlLibReleaseVersionSimple = latestPlantUmlLibReleaseVersion?.substringAfter("v")

val mvnCmd = if (Os.isFamily(Os.FAMILY_WINDOWS)) { "mvn.cmd" } else { "mvn"}


val downloadPlantUmlLibsTask = tasks.register("downloadPlantUmlLibs") {
    group = "plantuml-lib"

    doLast {
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

val buildEclipseUpdateSiteTask = tasks.register<Exec>("buildPlantUmlLibUpdateSite") {
    group = "build"

    dependsOn(copyLibsTask)
    dependsOn(updateVersionsInEclipseProjectsTask)

    workingDir = file(plantUmlLibRootDir).absoluteFile

    // Add --quiet argument?
    commandLine = listOf(mvnCmd, "--batch-mode", "--errors", "clean", "package")
}

tasks.register("printLatestPlantUMLVersion") {
    group = "plantuml-lib"

    doLast {
        println("Latest PlantUML lib version is $latestPlantUmlLibReleaseVersion.")
    }
}