#!/bin/sh
# callimachus.sh
#
# Copyright (c) 2010 Zepheira LLC, Some Rights Reserved
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

cygwin=false
darwin=false
case "`uname`" in
CYGWIN*) cygwin=true;;
Darwin*) darwin=true;;
esac

# resolve links - $0 may be a softlink
PRG="$0"

while [ -h "$PRG" ] ; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done
 
PRGDIR=`dirname "$PRG"`

if $darwin; then
  EXECUTABLE="$PRGDIR/jsvc-darwin"
else
  EXECUTABLE="$PRGDIR/jsvc-linux-i386"
fi

# Only set BASEDIR if not already set
[ -z "$BASEDIR" ] && BASEDIR=`cd "$PRGDIR/.." >/dev/null; pwd`

# Ensure that any user defined CLASSPATH variables are not used on startup.
CLASSPATH=

if [ -r "$BASEDIR/etc/callimachus.conf" ]; then
  . "$BASEDIR/etc/callimachus.conf"
fi

# Check that target executable exists
if [ ! -x "$EXECUTABLE" ]; then
  echo "Cannot find $EXECUTABLE"
  echo "This file is needed to run this program"
  exit 1
fi

# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin; then
  [ -n "$JAVA_HOME" ] && JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
  [ -n "$BASEDIR" ] && BASEDIR=`cygpath --unix "$BASEDIR"`
  [ -n "$CLASSPATH" ] && CLASSPATH=`cygpath --path --unix "$CLASSPATH"`
fi

if [ -z "$JAVA_VERSION" ] ; then
  JAVA_VERSION=1.6
fi

# Transform the required version string into an integer that can be used in comparisons
JAVA_VERSION_INT=`echo "$JAVA_VERSION" | sed -e 's;\.;0;g'`

# Check JAVA_HOME directory to see if Java version is adequate
if [ ! -z "$JAVA_HOME" -a -x "$JAVA_HOME/bin/java" -a -x "$JAVA_HOME/bin/javac" ] ; then
  JAVA="$JAVA_HOME/bin/java"
  VERSION=`"$JAVA" -version 2>&1 | grep "java version" | awk '{ print substr($3, 2, length($3)-2); }' | awk '{ print substr($1, 1, 3); }' | sed -e 's;\.;0;g'`
  if [ -L "$JAVA" -o -z "$VERSION" -o "$VERSION" -lt "$JAVA_VERSION_INT" ] ; then
    JAVA_HOME=
  fi
fi

# check the base dir for possible java candidates
if [ -z "$JAVA_HOME" ] && ls "$BASEDIR"/*/bin/javac >/dev/null 2>&1
then
  for JAVAC in `ls -1 "$BASEDIR"/*/bin/javac | xargs -d '\n'` ; do
    JAVA_HOME=`echo "$JAVAC" | awk '{ print substr($1, 1, length($1)-10); }'`
    JAVA="$JAVA_HOME/bin/java"
    if [ -L "$JAVA" ] ; then
      continue
    fi
    VERSION=`"$JAVA" -version 2>&1 | grep "java version" | awk '{ print substr($3, 2, length($3)-2); }' | awk '{ print substr($1, 1, 3); }' | sed -e 's;\.;0;g'`
    if [ ! -z "$JAVA_HOME" -a ! -L "$JAVA" -a ! -z "$VERSION" -a "$VERSION" -ge "$JAVA_VERSION_INT" ] ; then
      break
    fi
  done
  # verify java instance
  if [ ! -z "$JAVA_HOME" ] ; then
    JAVA="$JAVA_HOME/bin/java"
    VERSION=`"$JAVA" -version 2>&1 | grep "java version" | awk '{ print substr($3, 2, length($3)-2); }' | awk '{ print substr($1, 1, 3); }' | sed -e 's;\.;0;g'`
    if [ -z "$VERSION" -o "$VERSION" -lt "$JAVA_VERSION_INT" ] ; then
      JAVA_HOME=
    fi
  fi
fi

# use 'locate' to search for other possible java candidates
if [ -z "$JAVA_HOME" ]
then
  for JAVAC in `locate bin/javac |grep -e javac$ | xargs -d '\n'` ; do
    JAVA_HOME=`echo "$JAVAC" | awk '{ print substr($1, 1, length($1)-10); }'`
    JAVA="$JAVA_HOME/bin/java"
    if [ -L "$JAVA" ] ; then
      continue
    fi
    VERSION=`"$JAVA" -version 2>&1 | grep "java version" | awk '{ print substr($3, 2, length($3)-2); }' | awk '{ print substr($1, 1, 3); }' | sed -e 's;\.;0;g'`
    if [ ! -z "$JAVA_HOME" -a ! -L "$JAVA" -a ! -z "$VERSION" -a "$VERSION" -ge "$JAVA_VERSION_INT" ] ; then
      break
    fi
  done
  # verify java instance
  if [ -z "$JAVA_HOME" ] ; then
    echo "The JAVA_HOME environment variable is not defined"
    echo "This environment variable is needed to run this program"
    exit 1
  fi
  JAVA="$JAVA_HOME/bin/java"
  VERSION=`"$JAVA" -version 2>&1 | grep "java version" | awk '{ print substr($3, 2, length($3)-2); }' | awk '{ print substr($1, 1, 3); }' | sed -e 's;\.;0;g'`
  if [ -z "$VERSION" -o "$VERSION" -lt "$JAVA_VERSION_INT" ] ; then
    echo "The JAVA_HOME environment variable does not point to a $JAVA_VERSION JDK installation"
    echo "JDK $JAVA_VERSION is needed to run this program"
    exit 1
  fi
fi

if [ -z "$JAVA" ] ; then
  JAVA="$JAVA_HOME/bin/java"
fi

if [ -z "$OUT" ] ; then
  OUT="$BASEDIR"/log/callimachus.out
fi

if [ -z "$LIB" ] ; then
  LIB="$BASEDIR"/lib
fi

if [ -z "$TMPDIR" ] ; then
  # Define the java.io.tmpdir to use for Callimachus
  TMPDIR="$BASEDIR"/tmp
fi

if [ -z "$LOGGING_CONFIG" ] ; then
  LOGGING_CONFIG="$BASEDIR"/etc/logging.properties
fi

if [ -z "$PID" ] ; then
  PID="$BASEDIR/run/callimachus.pid"
fi

for JAR in `ls -1 "$BASEDIR"/lib/*.jar | xargs -d '\n'` ; do
  if [ ! -z "$CLASSPATH" ] ; then
    CLASSPATH="$CLASSPATH":
  fi
  CLASSPATH="$CLASSPATH$JAR"
done

if [ -z "$JAVA_OPTS" ] ; then
  JAVA_OPTS="-Xmx512m -Dfile.encoding=UTF-8"
fi

if [ -z "$JSVC_OPTS" ] ; then
  if [ -z "$CALLIMACHUS_USER" ] ; then
    JSVC_OPTS="-Xmx512m -Dfile.encoding=UTF-8"
  else
    JSVC_OPTS="-user $CALLIMACHUS_USER -Xmx512m -Dfile.encoding=UTF-8"
  fi
fi

if [ -z "$PORT" ] ; then
  PORT="8080"
fi

if [ -z "$AUTHORITY" ] ; then
  AUTHORITY="$(hostname -f)"
  if [ "$PORT" != "80" ] ; then
    AUTHORITY="$AUTHORITY:$PORT"
  fi
fi

# For Cygwin, switch paths to Windows format before running java
if $cygwin; then
  JAVA_HOME=`cygpath --absolute --windows "$JAVA_HOME"`
  BASEDIR=`cygpath --absolute --windows "$BASEDIR"`
  TMPDIR=`cygpath --absolute --windows "$TMPDIR"`
  CLASSPATH=`cygpath --path --windows "$CLASSPATH"`
fi

if [ "$1" = "start" ] ; then ################################

  if [ ! -z "$PID" ]; then
    if [ -f "$PID" ]; then
      echo "PID file ($PID) found. Is Callimachus still running? Start aborted."
      exit 1
    fi
  fi

  if [ "`tty`" != "not a tty" ]; then
    echo "Using BASEDIR:   $BASEDIR"
    echo "Using PORT:      $PORT"
    echo "Using AUTHORITY: $AUTHORITY"
    echo "Using JAVA_HOME: $JAVA_HOME"
  fi

  if [ ! -z "$CALLIMACHUS_USER" ] ; then
    if [ ! -e "$BASDIR/repositories" ] ; then
      mkdir "$BASDIR/repositories"
      chown "$CALLIMACHUS_USER" "$BASDIR/repositories"
      chmod u+s "$BASDIR/repositories"
    fi
    if [ ! -e "$BASEDIR/log" ] ; then
      mkdir "$BASEDIR/log"
      chown "$CALLIMACHUS_USER" "$BASEDIR/log"
      chmod u+s "$BASEDIR/log"
    fi
  fi

  mkdir -p `dirname "$PID"`
  if [ ! -e `dirname "$OUT"` ] ; then
    mkdir `dirname "$OUT"`
  fi
  echo -n > "$OUT"

  "$EXECUTABLE" -home "$JAVA_HOME" -jvm server -procname callimachus \
    -pidfile "$PID" \
    -outfile "$OUT" -errfile '&1' \
    -Duser.home="$BASEDIR" \
    -Djava.library.path="$LIB" \
    -Djava.io.tmpdir="$TMPDIR" \
    -Djava.util.logging.config.file="$LOGGING_CONFIG" \
    -classpath "$CLASSPATH" \
    $JSVC_OPTS org.callimachusproject.Server -d "$BASEDIR" -p "$PORT" -a "$AUTHORITY" $OPTS

  RETURN_VAL=$?

  sleep 1

  if [ $? -gt 0 -o ! -f "$PID" ]; then
    cat "$OUT" 1>&2
    echo
    echo "Callimachus did not start. Start aborted."
    exit $RETURN_VAL
  fi

  SLEEP=60
  ID=`cat $PID`
  while [ $SLEEP -ge 0 ]; do
    kill -0 $ID >/dev/null 2>&1
    if [ $? -gt 0 ]; then
      cat "$OUT" 1>&2
      echo
      echo "Callimachus is not running. Start aborted."
      exit 1
    fi
    if netstat -ltpn 2>/dev/null |grep -qe "\b$ID\b"; then
      break
    fi
    if [ $SLEEP -gt 0 ]; then
      sleep 1
    fi
    if [ $SLEEP -eq 0 ]; then
      if [ "`tty`" != "not a tty" ]; then
        echo "Callimachus is still starting up."
      fi
      break
    fi
    SLEEP=`expr $SLEEP - 1 `
  done

elif [ "$1" = "stop" ] ; then ################################

  if [ -f "$PID" -a -r "$PID" ]; then
    kill -0 `cat $PID` >/dev/null 2>&1
    if [ $? -gt 0 ]; then
      rm -f $PID
      if [ $? -gt 0 ]; then
        echo "PID file ($PID) found but no matching process was found. Stop aborted."
        exit $?
      fi
      exit $?
    fi
  elif [ -f "$PID" ]; then
    echo "The PID ($PID) exist, but it cannot be read. Stop aborted."
    exit 1
  else
    echo "The PID ($PID) does not exist. Is Callimachus running? Stop aborted."
    exit 1
  fi

  ID=`cat $PID`

  "$EXECUTABLE" -home "$JAVA_HOME" -stop \
    -pidfile "$PID" org.callimachusproject.Server

  SLEEP=60
  while [ $SLEEP -ge 0 ]; do 
    kill -0 $ID >/dev/null 2>&1
    if [ $? -gt 0 ]; then
      rm -f $PID
      break
    fi
    if [ $SLEEP -gt 0 ]; then
      sleep 1
    fi
    if [ "$SLEEP" = "30" ]; then
      kill $ID >/dev/null 2>&1
    fi
    SLEEP=`expr $SLEEP - 1 `
  done
    
  if [ -f "$PID" ]; then
    echo "Killing: $ID"
    kill -9 $ID
    rm -f $PID
  fi

else ################################

  if [ -f "$PID" ]; then
    echo "PID file ($PID) found. Is Callimachus still running? Run aborted."
    exit 1
   fi

  if [ "`tty`" != "not a tty" ]; then
    echo "Using BASEDIR:   $BASEDIR"
    echo "Using PORT:      $PORT"
    echo "Using AUTHORITY: $AUTHORITY"
    echo "Using JAVA_HOME: $JAVA_HOME"
  fi

  exec "$JAVA" -server \
    -Duser.home="$BASEDIR" \
    -Djava.library.path="$LIB" \
    -Djava.io.tmpdir="$TMPDIR" \
    -Djava.util.logging.config.file="$LOGGING_CONFIG" \
    -classpath "$CLASSPATH" \
    $JAVA_OPTS org.callimachusproject.Server -d "$BASEDIR" -p "$PORT" -a "$AUTHORITY" --pid "$PID" $OPTS "$@"

fi

