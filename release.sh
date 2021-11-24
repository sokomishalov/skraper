#!/usr/bin/env bash

set -e

# prepare release version
./mvnw build-helper:parse-version versions:set-property \
    -DversionString=\${revision} \
    -Dproperty=revision \
    -DnewVersion=\${parsedVersion.majorVersion}.\${parsedVersion.minorVersion}.\${parsedVersion.incrementalVersion}
NEW_VERSION=$(./mvnw help:evaluate -Dexpression=revision -q -DforceStdout)

# commit release
git add pom.xml

# generate changelog
docker run -it --rm -e CHANGELOG_GITHUB_TOKEN -v "$(pwd)":/usr/local/src/your-app \
    githubchangeloggenerator/github-changelog-generator \
    -u sokomishalov \
    -p skraper \
    --unreleased false \
    --future-release "$NEW_VERSION"
git add CHANGELOG.md

# deploy to maven central
./mvnw -P ossrh -D skipTests clean dokka:javadocJar deploy

# commit changes
git commit -m "$NEW_VERSION - new release"

# add tag to release
git tag "$NEW_VERSION"

# push release
git push origin master "$NEW_VERSION"

# prepare next development iteration
./mvnw build-helper:parse-version versions:set-property \
    -DversionString=\${revision} \
    -Dproperty=revision \
    -DnewVersion=\${parsedVersion.majorVersion}.\${parsedVersion.minorVersion}.\${parsedVersion.nextIncrementalVersion}-SNAPSHOT

NEW_SNAPSHOT=$(./mvnw help:evaluate -Dexpression=revision -q -DforceStdout)

git add pom.xml
git commit -m "$NEW_SNAPSHOT - new development version"
git push origin master