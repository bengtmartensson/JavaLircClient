#!/bin/sh
# Wrapper for LircClient

# Command to call the desired Java VM
JAVA=java

LIRCCLIENTHOME="$(dirname -- "$(readlink -f -- "${0}")" )"
JAR=${LIRCCLIENTHOME}/${project.name}-${project.version}-jar-with-dependencies.jar

${JAVA} -jar "$JAR" "$@"
