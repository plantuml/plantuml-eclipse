# plantuml4eclipse/releng/net.sourceforge.plantuml.parent

Parent Maven module for building and releasing new PlantUML Eclipse plug-in versions.

## Build procedure (PlantUML library only)

There two options for building the PlantUML library projects for Eclipse:
- In the directory `plantuml-lib` run `mvn clean package`
- In the git repo root directory run `./gradlew buildPlantUmlLibUpdateSite` (but this will call the Maven/tycho build anayway) 

## Build procedure (PlantUML4Eclipse only)

- Open the .target file in `plantuml4eclipse/releng/net.sourceforge.plantuml.target`, e.g. `eclipse-2023-12.target`. and set it as active target platform.
- Run pre-defined launch configuration *Build PlantUML4Eclipse with Maven* from `plantuml4eclipse/releng/net.sourceforge.plantuml.parent`.

## Run PlantUML in Eclipse

- Run the pre-defined launch configuration *PlantUML4e-2023-12_mac* or *PlantUML4e-2023-12_windows* (or similar version) from `plantuml4eclipse/releng/net.sourceforge.plantuml.parent`.

## Release procedure

### Release new PlantUML library version

This is done completely automatically.
As soon as a new PlantUML library version is released, the GitHub workflow `.github/workflows/release-plantuml-lib.yml` is triggered on the git branch *main*.
That worklfow builds a new version of the `net.sourceforge.plantuml.library` plug-in, the corresponding Eclipse feature, and the corresponding Eclipse update site (p2 repository).
After building the Eclipse projects, the workflow adds the new version to the composite update site (see `composite-repository`)
and commits the changes to this repository's GitHub pages (git branch *gh-pages*).
This way, the changes to the update site are published.


### PlantUML4Eclipse pre-release

- check out git branch `develop`
- update all `pom.xml`, `MANIFEST.MF`, `feature.xml`, etc. to new version, e.g. `1.2.0-SNAPSHOT` or `1.2.0.qualifier`,
  also update dependencies and other version-dependant configurations,
  do this also for the `pom.xml` files in folders `bundles`, `features`, `releng`, and `tests`,
  also update `releaseVersion` property in `plantuml4eclipse/releng/net.sourceforge.plantuml.parent/pom.xml`
- update `composite-repository/compositeArtifacts.xml` and `composite-repository/compositeContent.xml` files to the latest version from *gh-pages* branch (published version)
  in order to add the PlantUML library version(s) that were automatically published in the meanwhile.
- re-calculate / update all features' dependencies (seems to be obsolete now, see https://github.com/eclipse-pde/eclipse.pde/issues/26)
- build and test and build and test...
- run `mvn clean package` on project `plantuml4eclipse/releng/net.sourceforge.plantuml.parent` or just run the launch configuration *Build PlantUML4Eclipse with Maven*
  (that builds and runs all non-UI tests)
- run the plug-ins tests, too (see `net.sourceforge.plantuml.*.tests` and `no.hal.osgi.emf.tests` projects)
- git add, commit and push
- update the `README.md` in git repo root folder, e.g. add release notes
- switch to new branch named `release/<version>`

## Release

### Build artifacts and repository

- build with `mvn clean package` on project `plantuml4eclipse/releng/net.sourceforge.plantuml.parent` 
  or just run the launch configuration *Build PlantUML4Eclipse with Maven*
  (that builds and runs all non-UI unit tests)
- test the release candidate
- check the PlantUML Eclipse update site in `plantuml4eclipse/releng/net.sourceforge.plantuml.repository/target/repository`,
  ensure that you have only the latest plug-in / feature versions there and only one version per plug-in / feature
- add, commit and push

### Update GitHub pages

That is done by a Gradle build script, when you run `./gradlew gitCommitPlantUml4EclipseUpdateSiteToGhPages` in the git repo root directory.
Just check all the changes in the new commit and check if the following steps were successfully done by the build script.

- cloning *gh-pages* branch to `build/gh-pages` directory
- build PlantUML4Eclipse and copy the update site containing the built version to `build/gh-pages/plantuml.eclipse` directory
- add the new version to the files `compositeArtifacts.xml` and `compositeContent.xml` in `build/gh-pages`
  and increment the number of artifacts
- update the contents of `build\gh-pages\README.md` to the contents of the `README.md` from git repo root directory
- create a git commit on *gh-pages* branch with all changes

If everything is as expected, push the changes to GitHub pages (git branch *gh-pages*).

### Release on GitHub

- add a release (create a draft first, fill all the details)
- add release notes from README.md in root folder
- merge the `release` branch back into `main` branch
- tag the new version on `main` branch with e.g. 1.1.28
- choose a git tag (e.g. 1.1.28) and the branch `main`
- publish the release
- close issues that (supposedly) are fixed

### Post-release

- in `plantuml4eclipse`, search and replace <version> with <version+1>-SNAPSHOT in `pom.xml`, and <version> with <version+1>.qualifier in `MANIFEST.MF`, `feature.xml` and `category.xml`
- build, commit and push

(think that's enough)
