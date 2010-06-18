#!/bin/sh

bin="$(dirname "${0}")"
base="$bin/.."
lib="$base/lib"
tmp="$base/tmp"
logging="$bin/logging.properties"
JVM_ARGS="-server -mx512m -Dfile.encoding=UTF-8"
CLASSPATH="$lib/$(ls "$lib"|xargs |sed "s; ;:$lib/;g")"
MAIN="org.callimachusproject.Server"

exec java -Djava.library.path="$bin" -Djava.io.tmpdir="$tmp" -Djava.util.logging.config.file="$logging" $JVM_ARGS -cp "$CLASSPATH" "$MAIN" $*