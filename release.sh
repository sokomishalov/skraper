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
git commit -m "$NEW_VERSION - new release"

# generate changelog
docker run -it --rm -e CHANGELOG_GITHUB_TOKEN -v "$(pwd)":/usr/local/src/your-app \
    githubchangeloggenerator/github-changelog-generator -u sokomishalov -p skraper

./mvnw clean deploy scm:tag -P ossrh -D tag="${NEW_VERSION}" -D pushChanges=false -D skipTests

# add tag to release
git tag "$NEW_VERSION"
git push origin "$NEW_VERSION"

# prepare next development iteration
./mvnw build-helper:parse-version versions:set-property \
    -DversionString=\${revision} \
    -Dproperty=revision \
    -DnewVersion=\${parsedVersion.majorVersion}.\${parsedVersion.minorVersion}.\${parsedVersion.nextIncrementalVersion}-SNAPSHOT -s settings-nexus-new.xml

NEW_SNAPSHOT=$(mvn help:evaluate -Dexpression=revision -q -DforceStdout -s settings-nexus-new.xml)

git add pom.xml
git commit -m "$NEW_SNAPSHOT - new development version"

# push everything
git push origin master