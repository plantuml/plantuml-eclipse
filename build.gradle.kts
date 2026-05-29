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


// ════════════════════════════════════════════════════════════════════════════════
// Helper functions
// ════════════════════════════════════════════════════════════════════════════════

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

// Configure a Copy task to read composite*.xml files from sourceDir, add a new child entry
// with the given childLocation, increment the children count, and include the modified files in the copy.
fun CopySpec.addChildToCompositeXml(sourceDir: String, childLocation: String) {
    val prefix = "<children size='"
    val suffix = "'>"
    val childrenEndTag = "</children>"

    from(sourceDir) {
        include("composite*.xml")
        filter { line: String ->
            if (line.trim().startsWith(prefix) && line.endsWith(suffix)) {
                val sizeText = line.substring(line.indexOf(prefix) + prefix.length, line.indexOf(suffix))
                val size = Integer.valueOf(sizeText) + 1
                line.substring(0, line.indexOf(prefix)) + prefix + size + suffix
            } else if (line.contains(childrenEndTag)) {
                val indentation = line.substring(0, line.indexOf("<")) + "\t"
                indentation + "<child location='$childLocation' />\n" + line
            } else {
                line
            }
        }
    }
}

// Configure an Exec task to stage all changes in build/gh-pages with `git add --all`.
fun Exec.gitAddAllToGhPages() {
    commandLine = listOf(gitCmd, "-C", "$buildDirectoyPath/gh-pages", "add", "--all")
}

// Configure an Exec task to commit all staged changes in build/gh-pages with the given commit message.
fun Exec.gitCommitToGhPages(commitMessage: String) {
    commandLine = listOf(gitCmd, "-C", "$buildDirectoyPath/gh-pages", "commit", "-m", commitMessage)
}

// Configure a Task to verify that the given directory exists at execution time, throwing a GradleException
// with the given error message if it does not.
fun Task.verifyDirectoryExists(directory: File, errorMessage: String) {
    doLast {
        if (!directory.exists()) {
            throw GradleException(errorMessage)
        }
    }
}


// ════════════════════════════════════════════════════════════════════════════════
// Configuration
// ════════════════════════════════════════════════════════════════════════════════

val plantUmlLibRootDir = "plantuml-lib"
val plantUmlLibPluginName = "net.sourceforge.plantuml.library"
val plantUmlLibFeatureName = "$plantUmlLibPluginName.feature"
val plantUmlLibRepositoryName = "$plantUmlLibPluginName.repository"
val plantUmlLibPluginDir = "$plantUmlLibRootDir/$plantUmlLibPluginName"
val plantUmlLibFeatureDir = "$plantUmlLibRootDir/$plantUmlLibFeatureName"
val plantUmlLibRepositoryDir = "$plantUmlLibRootDir/$plantUmlLibRepositoryName"
val plantUmlLibPluginLibDir = "$plantUmlLibPluginDir/lib"

// Prefer explicitly provided version (e.g. -PplantUmlVersion=1.2026.5), fall back to fetching latest from GitHub.
// plantUmlLibReleaseVersion: tag name as used in GitHub release download URLs (e.g. v1.2026.5).
// plantUmlLibReleaseVersionSimple: simple form without "v" prefix (e.g. 1.2026.5), used everywhere except download URLs.
val plantUmlLibReleaseVersion: String = if (project.hasProperty("plantUmlVersion")) {
    // User provides simple form (e.g. 1.2026.5); reconstruct tag form with "v" prefix for download URLs.
    // Assumes the release tag has a "v" prefix (current PlantUML convention).
    // Update if PlantUML changes their tag format.
    val version = project.property("plantUmlVersion") as String
    if (version.startsWith("v")) version else "v$version"
} else {
    // Use the raw tag name from the API — robust against tag format changes.
    readLatestPlantUmlLibReleaseVersion()
}
val plantUmlLibReleaseVersionSimple = plantUmlLibReleaseVersion.removePrefix("v")

val plantUml4ERootDir = "plantuml4eclipse"
val plantUml4EParentDir = "$plantUml4ERootDir/releng/net.sourceforge.plantuml.parent"
val plantUml4ERepositoryDir = "$plantUml4ERootDir/releng/net.sourceforge.plantuml.repository"
val plantUml4EVersion = readCurrentPlantUML4EVersionFromPom()

val buildDirectoyPath = project.layout.buildDirectory.get().toString()
val buildDir          = "build"
val ghPagesDir        = "$buildDir/gh-pages"
val libDir            = "$buildDir/lib"
val eclipseFilesDir   = "$buildDir/eclipse-files"
val compositeRepoDir  = "$buildDir/composite-repository"

val mvnCmd = if (Os.isFamily(Os.FAMILY_WINDOWS)) { "mvn.cmd" } else { "mvn"}
val gitCmd = if (Os.isFamily(Os.FAMILY_WINDOWS)) { "git.exe" } else { "git"}


// ════════════════════════════════════════════════════════════════════════════════
// Common tasks (used by PlantUML library and PlantUML4Eclipse tasks)
// ════════════════════════════════════════════════════════════════════════════════

// git clone gh-pages branch to build/gh-pages
val cloneGhPagesTask = tasks.register<Exec>("cloneGitHubPages") {
    group = "publish"

    outputs.dir(project.layout.buildDirectory.dir("gh-pages"))

    commandLine = listOf(gitCmd, "clone", "-b", "gh-pages", "https://github.com/plantuml/plantuml-eclipse.git", ghPagesDir)
}


// ════════════════════════════════════════════════════════════════════════════════
// PlantUML library tasks
// ════════════════════════════════════════════════════════════════════════════════

val setEnvVarPlantUmlLibVersionTask = tasks.register("printPlantUmlLibVersion") {
    group = "plantuml-lib"

    doLast {
        println(plantUmlLibReleaseVersionSimple)
    }
}

// download latest PlantUML EPL lib to build/lib
val downloadPlantUmlLibsTask = tasks.register("downloadPlantUmlLibs") {
    group = "plantuml-lib"

    inputs.property("plantUmlVersion", plantUmlLibReleaseVersionSimple)
    outputs.dir(project.layout.buildDirectory.dir("lib"))

    doLast {
        println("#################################################################################")
        println("Using PlantUML library version: $plantUmlLibReleaseVersionSimple")
        println("#################################################################################")

        downloadFile("https://github.com/plantuml/plantuml/releases/download/$plantUmlLibReleaseVersion/plantuml-epl-$plantUmlLibReleaseVersionSimple.jar", libDir)
        downloadFile("https://github.com/plantuml/plantuml/releases/download/$plantUmlLibReleaseVersion/plantuml-epl-$plantUmlLibReleaseVersionSimple-sources.jar", libDir)
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

    from(libDir)
    into(plantUmlLibPluginLibDir)
    include("*.jar")
}

// set version in plantuml-lib\net.sourceforge.plantuml.library\META-INF\MANIFEST.MF to that of latest PlantUML lib
// place modified file to build/eclipse-files/
val updateVersionsInManifestTask = tasks.register<Copy>("updateVersionsInManifest") {
    group = "plantuml-lib"

    inputs.property("plantUmlVersion", plantUmlLibReleaseVersionSimple)
    outputs.dir(project.layout.buildDirectory.dir("eclipse-files"))

    from("$plantUmlLibPluginDir/META-INF") {
        include("MANIFEST.MF")
        filter { line: String ->
            if (line.startsWith("Bundle-Version:")) {
                "Bundle-Version: $plantUmlLibReleaseVersionSimple.qualifier"
            }
            else if (line.startsWith("Bundle-ClassPath: lib/plantuml-epl-")) {
                "Bundle-ClassPath: lib/plantuml-epl-$plantUmlLibReleaseVersionSimple.jar"
            }
            else line
        }
    }
    into("$eclipseFilesDir/$plantUmlLibPluginName/META-INF")
    filteringCharset = "UTF-8"
}

// set version in plantuml-lib\net.sourceforge.plantuml.library\.classpath & ...\build.properties to that of latest PlantUML lib
// place modified files to build/eclipse-files/
val updateVersionsInClasspathTask = tasks.register<Copy>("updateVersionsInClasspath") {
    group = "plantuml-lib"

    inputs.property("plantUmlVersion", plantUmlLibReleaseVersionSimple)
    outputs.dir(project.layout.buildDirectory.dir("eclipse-files"))

    val linePrefix = "<classpathentry exported=\"true\" kind=\"lib\" path=\"lib/plantuml-epl-"

    from(plantUmlLibPluginDir) {
        include(".classpath")
        filter { line: String ->
            if (line.contains(linePrefix, false)) {
                val start = line.indexOf(linePrefix) + linePrefix.length
                val end = line.indexOf(".jar\"")
                line.substring(0, start) + plantUmlLibReleaseVersionSimple + line.substring(end)
            }
            else line
        }
    }

    from(plantUmlLibPluginDir) {
        include("build.properties")
        filter { line: String ->
            if (line.startsWith("bin.includes = lib/plantuml-epl-")) {
                "bin.includes = lib/plantuml-epl-$plantUmlLibReleaseVersionSimple.jar,\\"
            }
            else if (line.startsWith("src.includes = lib/plantuml-epl-")) {
                "src.includes = lib/plantuml-epl-$plantUmlLibReleaseVersionSimple-sources.jar,\\"
            }
            else line
        }
    }
    into("$eclipseFilesDir/$plantUmlLibPluginName")
    filteringCharset = "UTF-8"
}

// set version in plantuml-lib\net.sourceforge.plantuml.library.feature\feature.xml to that of latest PlantUML lib
// place modified file to build/eclipse-files/
val updateVersionInFeatureTask = tasks.register<Copy>("updateVersionInFeature") {
    group = "plantuml-lib"

    inputs.property("plantUmlVersion", plantUmlLibReleaseVersionSimple)
    outputs.dir(project.layout.buildDirectory.dir("eclipse-files"))

    from(plantUmlLibFeatureDir) {
        include("feature.xml")
        filter { line: String ->
            if (line.trim().startsWith("version=\"") && line.endsWith(".qualifier\"")) {
                line.substring(0, line.indexOf("\"") + 1) + plantUmlLibReleaseVersionSimple + line.substring(line.indexOf(".qualifier"))
            }
            else line
        }
    }
    into("$eclipseFilesDir/$plantUmlLibFeatureName")
    filteringCharset = "UTF-8"
}

// set version in plantuml-lib\pom.xml to that of latest PlantUML lib
// place modified file to build/eclipse-files/
val updateVersionInParentPomTask = tasks.register<Copy>("updateVersionInPom") {
    group = "plantuml-lib"

    inputs.property("plantUmlVersion", plantUmlLibReleaseVersionSimple)
    outputs.dir(project.layout.buildDirectory.dir("eclipse-files"))

    val startTag = "<plantuml-lib-version>"
    val endTag = "</plantuml-lib-version>"

    from(plantUmlLibRootDir) {
        include("pom.xml")
        filter { line: String ->
            if (line.trim().startsWith(startTag) && line.endsWith(endTag)) {
                line.substring(0, line.indexOf(startTag) + startTag.length) + plantUmlLibReleaseVersionSimple + line.substring(line.indexOf(endTag))
            }
            else line
        }
    }
    into(eclipseFilesDir)
    filteringCharset = "UTF-8"
}

// update the list of exported packages in MANIFEST.MF, i.e.
// scan the downloaded PlantUML JAR and regenerate the Export-Package block in
// build/eclipse-files/.../MANIFEST.MF (produced by updateVersionsInManifestTask)
val updateExportedPackagesInManifestTask = tasks.register("updateExportedPackagesInManifest") {
    group = "plantuml-lib"

    dependsOn(downloadPlantUmlLibsTask)
    dependsOn(updateVersionsInManifestTask)

    inputs.property("plantUmlVersion", plantUmlLibReleaseVersionSimple)
    inputs.file("$libDir/plantuml-epl-$plantUmlLibReleaseVersionSimple.jar")
    outputs.file("$eclipseFilesDir/$plantUmlLibPluginName/META-INF/MANIFEST.MF")

    doLast {
        val jarFile = file("$libDir/plantuml-epl-$plantUmlLibReleaseVersionSimple.jar")
        val manifestFile = file("$eclipseFilesDir/$plantUmlLibPluginName/META-INF/MANIFEST.MF")

        val packages = java.util.jar.JarFile(jarFile).use { jar ->
            jar.entries().asSequence()
                .filter { !it.isDirectory && it.name.startsWith("net/sourceforge/plantuml/") && it.name.endsWith(".class") }
                .map { it.name.substringBeforeLast("/").replace("/", ".") }
                .toSortedSet()
        }

        if (packages.isEmpty()) {
            throw GradleException(
                "No net.sourceforge.plantuml packages found in ${jarFile.name} - aborting release."
            )
        }

        val exportPackageBlock = packages
            .mapIndexed { index, pkg -> if (index == 0) "Export-Package: $pkg," else " $pkg," }
            .toMutableList()
            .also { it[it.lastIndex] = it.last().trimEnd(',') }
            .joinToString("\n")

        val originalLines = manifestFile.readLines(Charsets.UTF_8)
        val updatedContent = buildString {
            var inExportPackage = false
            for (line in originalLines) {
                when {
                    line.startsWith("Export-Package:") -> {
                        inExportPackage = true
                        appendLine(exportPackageBlock)
                    }
                    inExportPackage && line.startsWith(" ") -> { /* skip old continuation lines */ }
                    else -> {
                        inExportPackage = false
                        appendLine(line)
                    }
                }
            }
        }
        manifestFile.writeText(updatedContent, Charsets.UTF_8)

        println("Export-Package updated with ${packages.size} packages from ${jarFile.name}.")
    }
}

// copy filtered/modified files from build/eclipse-files/ to plantuml-lib/
val updateVersionsInEclipseProjectsTask = tasks.register<Copy>("updateVersionsInEclipseProjects") {
    group = "plantuml-lib"

    inputs.dir(project.layout.buildDirectory.dir("eclipse-files"))

    dependsOn(updateVersionsInManifestTask)
    dependsOn(updateVersionsInClasspathTask)
    dependsOn(updateVersionInFeatureTask)
    dependsOn(updateVersionInParentPomTask)
    dependsOn(updateExportedPackagesInManifestTask)

    from(eclipseFilesDir)
    into(plantUmlLibRootDir)
    filteringCharset = "UTF-8"
}

// build PlantUML lib projects (plug-in, feature, and update site / p2 repo) with Maven/Tycho
val buildPlantUmlLibUpdateSiteTask = tasks.register<Exec>("buildPlantUmlLibUpdateSite") {
    group = "build"

    dependsOn(copyLibsTask)
    dependsOn(updateVersionsInEclipseProjectsTask)

    inputs.files(fileTree(plantUmlLibRootDir).exclude("**/target/**"))
    inputs.property("plantUmlVersion", plantUmlLibReleaseVersionSimple)

    outputs.dir("$plantUmlLibRepositoryDir/target")

    workingDir = file(plantUmlLibRootDir).absoluteFile

    commandLine = listOf(mvnCmd, "--batch-mode", "--update-snapshots", "--errors", "--quiet", "clean", "package")
}

// check if build/gh-pages/plantuml.lib/<latest-PlantUML-lib-version> already exists
val checkIfPlantUmlLibIsAlreadyPublishedTask = tasks.register("checkIfPlantUmlLibIsAlreadyPublished") {
    group = "publish"

    dependsOn(cloneGhPagesTask)

    doLast {
        val ghPagesUpdateSiteTargetDir = File("$ghPagesDir/plantuml.lib", plantUmlLibReleaseVersionSimple)
        if (ghPagesUpdateSiteTargetDir.exists()) {
            throw GradleException("The PlantUML library version $plantUmlLibReleaseVersionSimple has already been" +
                    " published. The files were found in directory ${ghPagesUpdateSiteTargetDir}.")
        }
    }
}

// copy composite*.xml published on GH pages to temporary build folder in order to modify them in a next step by filtering
val copyGhPagesFilesForModificationForPlantUmlLibReleaseTask = tasks.register<Copy>("copyGhPagesFilesForModificationForPlantUmlLibRelease") {
    group = "publish"

    dependsOn(cloneGhPagesTask)

    outputs.dir(project.layout.buildDirectory.dir("composite-repository"))

    from(ghPagesDir) {
        include("composite*.xml")
    }
    into(compositeRepoDir)
    filteringCharset = "UTF-8"
}

// update composite*.xml: add new PlantUML lib version / update site, copy the files to build/gh-pages
// Do not update README.md and p2.index . These will be updated only when PlantUML4eclipse is released.
val updateGhPagesFilesAddLatestPlantUmlLibTask = tasks.register<Copy>("updateGhPagesFilesAddLatestPlantUmlLib") {
    group = "publish"

    dependsOn(copyGhPagesFilesForModificationForPlantUmlLibReleaseTask)

    outputs.dir(project.layout.buildDirectory.dir("gh-pages"))

    addChildToCompositeXml(compositeRepoDir, "plantuml.lib/$plantUmlLibReleaseVersionSimple")
    into(ghPagesDir)
    filteringCharset = "UTF-8"
}

// update composite-repository/repository/composite*.xml, add current PlantUML4Eclipse version / update site
// copy composite-repository/repository/*.* and README.md to build/gh-pages
val updateGhPagesFilesAddPlantUml4ETask = tasks.register<Copy>("updateGhPagesFilesAddPlantUml4Eclipse") {
    group = "publish"

    dependsOn(cloneGhPagesTask)

    outputs.dir(project.layout.buildDirectory.dir("gh-pages"))

    addChildToCompositeXml("composite-repository/repository", "plantuml.eclipse/$plantUml4EVersion")
    from("composite-repository/repository") {
        include("p2.index")
    }
    from(".") {
        include("README.md")
    }
    into(ghPagesDir)
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
    into("$ghPagesDir/plantuml.lib/$plantUmlLibReleaseVersionSimple")
    filteringCharset = "UTF-8"
}

// Add new PlantUML lib update site to GitHub pages in build/gh-pages (call other tasks to do so)
val updateGhPagesContentsAddLatestPlantUmlLibTask = tasks.register("updateGhPagesContentsAddLatestPlantUmlLib") {
    group = "publish"

    dependsOn(updateGhPagesFilesAddLatestPlantUmlLibTask)
    dependsOn(addLatestPlantUmlUpdateSiteToGhPagesTask)

    val ghPagesUpdateSiteTargetDir = File("$ghPagesDir/plantuml.lib", plantUmlLibReleaseVersionSimple)
    verifyDirectoryExists(ghPagesUpdateSiteTargetDir,
        "The new PlantUML library version $plantUmlLibReleaseVersionSimple is missing in the build directory." +
        " Expected the following directory to exist: $ghPagesUpdateSiteTargetDir.")
}

// git add everything in build/gh-pages/
val gitAddPlantUmlLibUpdateSiteToGhPagesTask = tasks.register<Exec>("gitAddPlantUmlLibUpdateSiteToGhPages") {
    group = "publish"
    dependsOn(updateGhPagesContentsAddLatestPlantUmlLibTask)
    gitAddAllToGhPages()
}

// git commit changed files for new PlantUML lib update site
val gitCommitPlantUmlLibUpdateSiteToGhPagesTask = tasks.register<Exec>("gitCommitPlantUmlLibUpdateSiteToGhPages") {
    group = "publish"
    dependsOn(gitAddPlantUmlLibUpdateSiteToGhPagesTask)
    gitCommitToGhPages("Add new PlantUML lib update site version $plantUmlLibReleaseVersionSimple")
}

// git push commit with new PlantUML lib update site
val gitPushPlantUmlLibUpdateSiteToGhPagesTask = tasks.register<Exec>("gitPushPlantUmlLibUpdateSiteToGhPages") {
    group = "publish"

    dependsOn(gitCommitPlantUmlLibUpdateSiteToGhPagesTask)

    commandLine = listOf(gitCmd, "-C", "$buildDirectoyPath/gh-pages", "push")
}


// ════════════════════════════════════════════════════════════════════════════════
// PlantUML4Eclipse tasks
// ════════════════════════════════════════════════════════════════════════════════

// build PlantUML4Eclipse projects (plug-ins, features, and update site / p2 repo) with Maven/Tycho
val buildPlantUml4EUpdateSiteTask = tasks.register<Exec>("buildPlantUml4EUpdateSite") {
    group = "build"

    inputs.files(fileTree(plantUml4ERootDir).exclude("**/target/**"))
    inputs.property("plantUml4EVersion", plantUml4EVersion)

    outputs.dir("$plantUml4ERepositoryDir/target")

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
        val ghPagesUpdateSiteTargetDir = File("$ghPagesDir/plantuml.eclipse", plantUml4EVersion)
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

    val targetDirPath = "$ghPagesDir/plantuml.eclipse/$plantUml4EVersion"

    from("$plantUml4ERepositoryDir/target/repository")
    into(targetDirPath)
    filteringCharset = "UTF-8"
}

// Add new PlantUML4Eclipse update site to GitHub pages in build/gh-pages (call other tasks to do so)
val updateGhPagesContentsAddPlantUml4ETask = tasks.register("updateGhPagesContentsAddPlantUml4Eclipse") {
    group = "publish"

    dependsOn(updateGhPagesFilesAddPlantUml4ETask)
    dependsOn(addPlantUml4EUpdateSiteToGhPagesTask)

    val ghPagesUpdateSiteTargetDir = File("$ghPagesDir/plantuml.eclipse", plantUml4EVersion)
    verifyDirectoryExists(ghPagesUpdateSiteTargetDir,
        "The new PlantUML4Eclipse version $plantUml4EVersion is missing in the build directory." +
        " Expected the following directory to exist: $ghPagesUpdateSiteTargetDir.")
}

// git add everything in build/gh-pages/
val gitAddPlantUml4EUpdateSiteToGhPagesTask = tasks.register<Exec>("gitAddPlantUml4EclipseUpdateSiteToGhPages") {
    group = "publish"
    dependsOn(updateGhPagesContentsAddPlantUml4ETask)
    gitAddAllToGhPages()
}

// git commit changed files for new PlantUML4Eclipse update site
val gitCommitPlantUml4EUpdateSiteToGhPagesTask = tasks.register<Exec>("gitCommitPlantUml4EclipseUpdateSiteToGhPages") {
    group = "publish"
    dependsOn(gitAddPlantUml4EUpdateSiteToGhPagesTask)
    gitCommitToGhPages("New PlantUML4Eclipse release: $plantUml4EVersion")
}

// We did not add a git push gradle task for PlantUml4Eclipse, since we want the changes to be reviewed before pushing them


// ════════════════════════════════════════════════════════════════════════════════
// General task dependencies
// ════════════════════════════════════════════════════════════════════════════════

tasks.named("build") {
    dependsOn(
        buildPlantUmlLibUpdateSiteTask,
        buildPlantUml4EUpdateSiteTask
    )
}

val mvnCleanPlantUmlLibTask = tasks.register<Exec>("mvnCleanPlantUmlLib") {
    group = "build"
    workingDir = file(plantUmlLibRootDir).absoluteFile
    commandLine = listOf(mvnCmd, "--batch-mode", "--quiet", "clean")
}

val mvnCleanPlantUml4ETask = tasks.register<Exec>("mvnCleanPlantUml4Eclipse") {
    group = "build"
    workingDir = file(plantUml4EParentDir).absoluteFile
    commandLine = listOf(mvnCmd, "--batch-mode", "--quiet", "clean")
}

tasks.named("clean") {
    dependsOn(mvnCleanPlantUmlLibTask, mvnCleanPlantUml4ETask)
}