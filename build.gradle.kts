import groovy.json.JsonSlurper
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI

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
    var versionText = json["tag_name"].toString()

    if (versionText.startsWith("v")) {
        versionText = versionText.substringAfter("v")
    }
    return versionText
}

//val testTask = tasks.register<Exec>("get-version") {
//    //workingDir = file("../plantuml-eclipse").absoluteFile
//    commandLine = listOf("", "clean", "package")
//}

tasks.register("printLatestPlantUMLVersion") {
    val latestVersion = readLatestPlantUmlLibReleaseVersion()

    doLast {
        println("Latest PlantUML lib version is $latestVersion.")
    }
}