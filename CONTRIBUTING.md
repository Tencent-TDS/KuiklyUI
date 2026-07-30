# Contribution Guide

We highly appreciate your contributions to Kuikly, whether it's raising an issue, fixing a bug, or submitting a new feature. Before submitting your contribution, please take a moment to read the following guidelines:

## Issue Reporting Guidelines

The project collects [issues]() and requirements through Issues.

- Before raising an issue, please check if someone else has already proposed a similar feature or problem.

- When raising an issue, please describe the problem in as much detail as possible, including steps to reproduce, code snippets, screenshots, etc.

### Bug Report
#### Describe the bug
A clear and concise description of what the bug is.

#### To Reproduce
Steps to reproduce the behavior:

- Go to '...'
- Click on '....'
- Scroll down to '....'
- See error

#### Expected behavior
A clear and concise description of what you expected to happen.

#### Screenshots
If applicable, add screenshots to help explain your problem.

#### Smartphone (please complete the following information):

- Device: [e.g. iPhone6]
- OS and version: [e.g. iOS 13.3.1]
- Kuikly  SDK version: [e.g. 2.0.0]
- Additional context Add any other context about the problem here.

### Feature Request
#### Is your feature request related to a problem? Please describe.
A clear and concise description of what the problem is. Ex. I'm always frustrated when [...]

#### Describe the solution you'd like
A clear and concise description of what you want to happen.

#### Describe alternatives you've considered
A clear and concise description of any alternative solutions or features you've considered.

#### Additional context
Add any other context or screenshots about the feature request here.

## Pull Request Guidelines
### Branch Management

This project mainly includes the following three types of branches:
* main: A stable development branch that only accepts fully tested features or bug fixes. Releases are made directly from the main branch by tagging.
* feature: Named according to the feature/** rule, it is a feature development branch.
* bugfix: Named according to the bugfix/** rule, it is a daily bugfix branch.

## Commit Messgage

Please follow the [Angular Convention](https://docs.google.com/document/d/1QrDFcIiPjSLDn3EL15IJygNPiHORgU1_OOAqWjiDU5Y/edit#heading=h.greljkmo14y0) for commit messages:
- feat：New feature
- fix：Bug fix
- docs：Documentation changes
- style： Formatting changes (does not affect code execution)
- refactor：Code refactoring (neither a new feature nor a bug fix)
- test：Adding tests
- chore：Changes to the build process or auxiliary tools

## Merge Reuqest

Please initiate a merge request via [Merge Request](merge_requests/new). Before submitting an MR, please ensure:

- Checkout a topic branch from the relevant branch, e.g. main, and merge back against that branch.
- The code has been thoroughly tested and validated.
- The code complies with the project's coding standards.
- If it involves API changes, please update the API documentation.
- Link relevant issues or requirements.

## Code Standards

This project adheres to the coding style guides provided by Google: [Google's Style Guides](https://google.github.io/styleguide/)

## Version Numbering Convention

Version releases follow a three-part naming rule: `major.minor.fix`, e.g., 1.0.0

- major: Major version number, core baseline version.
- minor: Minor version number, used for releasing small feature modules.
- fix: Stage version number, used for bug fixes or partial code module optimizations.

For releasing alpha, beta, rc, etc., versions, append `-[alpha|beta|rc]` to the version number, e.g., 1.0.0-beta.

> Special Note: Kuikly will use a higher version of Kotlin during the development process to leverage official capabilities, while the Kotlin version in business projects may be lower. Therefore, it is necessary to support multiple versions for business selection. Thus, the version number of the core library needs to include the Kotlin version as a distinction, e.g., 1.0.0-1.7.0, where 1.7.0 is the minimum Kotlin version required by the business project.
