name: 📦 Release Checklist
description: Track steps for releasing a library
title: "Release: <library-name> <release-version>"
labels: [release]

body:
- type: input
  id: library-name
  attributes:
  label: Library name
  description: The name of the library being released
  placeholder: e.g. maps-server
  validations:
  required: true

- type: input
  id: snapshot-version
  attributes:
  label: Snapshot version
  description: Current snapshot version before release
  placeholder: e.g. 3.3.7-SNAPSHOT
  validations:
  required: true

- type: input
  id: release-version
  attributes:
  label: Release version
  description: Version to be released
  placeholder: e.g. 3.3.7
  validations:
  required: true

- type: checkboxes
  id: steps
  attributes:
  label: Release Steps
  options:
  - label: Pull Request created for release
  - label: PR merged into **main**
  - label: Update `main` `pom.xml` with release version
  - label: Run **release build** and publish
  - label: Create **Git tag** for release version
  - label: Create new **release branch** from `main`
  - label: Delete old **development branch**
  - label: Create new **development branch** from `main`
  - label: Update `pom.xml` with next snapshot version
  - label: Run **snapshot build** to confirm new version
