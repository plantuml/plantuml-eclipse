# Agent Instructions for plantuml-eclipse

## Project Overview

This is an Eclipse plug-in project that integrates [PlantUML](http://plantuml.com) into Eclipse.
It consists of two independently releasable sub-projects:

- **`plantuml-lib`** — the `net.sourceforge.plantuml.library` Eclipse bundle (wraps the PlantUML JAR)
- **`plantuml4eclipse`** — the Eclipse plug-ins that provide diagram views, editors, and extension points

## Developer Documentation

- **Build, run & release instructions**: `plantuml4eclipse/releng/net.sourceforge.plantuml.parent/README.md`
- **Diagram customization**: `plantuml4eclipse/releng/net.sourceforge.plantuml.parent/Customization.md`

## Build

The build uses Gradle (wrapper) to orchestrate Maven/Tycho. Run from the repo root:

```bash
./gradlew buildPlantUmlLibUpdateSite          # build plantuml-lib Eclipse bundle
./gradlew buildPlantUml4EUpdateSite           # build plantuml4eclipse plug-ins
```

## GitHub Workflows

| Workflow                   | Trigger                                                        | Purpose                                                                                           |
| -------------------------- | -------------------------------------------------------------- | ------------------------------------------------------------------------------------------------- |
| `ci.yml`                   | Push (except `gh-pages`), PR to `main`                         | Builds both sub-projects, runs non-UI tests                                                       |
| `release-plantuml-lib.yml` | `repository_dispatch` from [plantuml/plantuml CI][plantuml-ci] | Builds & publishes a new `plantuml-lib` Eclipse bundle as soon as there is a new PlantUML release |

[plantuml-ci]: https://github.com/plantuml/plantuml/blob/master/.github/workflows/ci.yml

Releases are published to the `gh-pages` branch as a composite p2 update site, managed by Gradle scripts.
Developers may push directly to `gh-pages` to update published content such as `README.md` or fix previously
published artifacts.
