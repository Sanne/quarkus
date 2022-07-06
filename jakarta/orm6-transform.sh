#!/bin/bash

set -e -u -o pipefail
shopt -s failglob

# Highly experimental - unlikely to end up compiling

# Make sure WIP changes in parent pom are installed
./mvnw -B install -pl :quarkus-parent 

./mvnw -B -e rewrite:run -Denforcer.skip -Dprotoc.skip -Dmaven.main.skip -Dmaven.test.skip -Dforbiddenapis.skip -Dinvoker.skip -Dquarkus.generate-code.skip -Dquarkus.build.skip -DskipExtensionValidation -DskipCodestartValidation -pl ':quarkus-bom' -Drewrite.pomCacheEnabled=false -Dhibernate6-rewrite

