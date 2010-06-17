#!/bin/sh

base="$(dirname "${0}")/.."
lib="$base/lib"
logging="$(dirname "${0}")/logging.properties"
JVM_ARGS="-mx512m -Djava.util.logging.config.file='$logging'"
CLASSPATH="$lib/$(ls "$lib"|xargs |sed "s; ;:$lib/;g")"

exec java $JVM_ARGS -cp "$CLASSPATH" org.callimachusproject.Server $*