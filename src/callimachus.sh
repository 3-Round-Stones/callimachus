#!/bin/sh
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

if [ -z "$NAME" ] ; then
  NAME=`basename "$PRG" | sed 's/\.sh$//'`
fi

if $darwin; then
  EXECUTABLE="$PRGDIR/$NAME-darwin"
elif [ `uname -m` = "x86_64" ]; then
  EXECUTABLE="$PRGDIR/$NAME-linux-x86_64"
else
  EXECUTABLE="$PRGDIR/$NAME-linux-i686"
fi

# Only set BASEDIR if not already set
[ -z "$BASEDIR" ] && BASEDIR=`cd "$PRGDIR/.." >/dev/null; pwd`

# Ensure that any user defined CLASSPATH variables are not used on startup.
CLASSPATH=

if [ -z "$CONFIG" ] ; then
  CONFIG="$BASEDIR/etc/$NAME.conf"
fi

if [ -r "$CONFIG" ]; then
  . "$CONFIG" 2>/dev/null
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
  JAVAC=`ls -1 "$BASEDIR"/*/bin/javac | head -n 1`
  if [ -x "$JAVAC" ] ; then
    JAVA_HOME=`echo "$JAVAC" | awk '{ print substr($1, 1, length($1)-10); }'`
    # verify java instance
    if [ ! -z "$JAVA_HOME" ] ; then
      JAVA="$JAVA_HOME/bin/java"
      VERSION=`"$JAVA" -version 2>&1 | grep "java version" | awk '{ print substr($3, 2, length($3)-2); }' | awk '{ print substr($1, 1, 3); }' | sed -e 's;\.;0;g'`
      if [ -z "$VERSION" -o "$VERSION" -lt "$JAVA_VERSION_INT" ] ; then
        JAVA_HOME=
      fi
    fi
  fi
fi

# use 'java_home' to search for other possible java candidate
if [ -z "$JAVA_HOME" -a -x /usr/libexec/java_home ] ; then
  JAVA_HOME=`/usr/libexec/java_home`
  # verify java instance
  if [ ! -z "$JAVA_HOME" ] ; then
    JAVA="$JAVA_HOME/bin/java"
    VERSION=`"$JAVA" -version 2>&1 | grep "java version" | awk '{ print substr($3, 2, length($3)-2); }' | awk '{ print substr($1, 1, 3); }' | sed -e 's;\.;0;g'`
    if [ -z "$VERSION" -o "$VERSION" -lt "$JAVA_VERSION_INT" ] ; then
      JAVA_HOME=
    fi
  fi
fi

# use 'which' to search for other possible java candidate
if [ -z "$JAVA_HOME" ] ; then
  JAVAC=`which javac`
  if [ -x "$JAVAC" ] ; then
    while [ -h "$JAVAC" ] ; do
      ls=`ls -ld "$JAVAC"`
      link=`expr "$ls" : '.*-> \(.*\)$'`
      if expr "$link" : '/.*' > /dev/null; then
        JAVAC="$link"
      else
        JAVAC=`dirname "$JAVAC"`/"$link"
      fi
    done
    HOME=`echo "$JAVAC" | awk '{ print substr($1, 1, length($1)-10); }'`
    if [ -d "$HOME" ] ; then
      JAVA_HOME="$HOME"
    else
      JAVA_HOME=`which javac | awk '{ print substr($1, 1, length($1)-10); }'`
    fi
  fi
  # verify java instance
  if [ -z "$JAVA_HOME" ] ; then
    echo "The JAVA_HOME environment variable is not defined"
    echo "This environment variable is needed to run this server"
    exit 1
  fi
  JAVA="$JAVA_HOME/bin/java"
  VERSION=`"$JAVA" -version 2>&1 | grep "java version" | awk '{ print substr($3, 2, length($3)-2); }' | awk '{ print substr($1, 1, 3); }' | sed -e 's;\.;0;g'`
  if [ ! -x "$JAVA" -o -z "$VERSION" -o "$VERSION" -lt "$JAVA_VERSION_INT" ] ; then
    echo "The JAVA_HOME environment variable does not point to a $JAVA_VERSION JDK installation"
    echo "JDK $JAVA_VERSION is needed to run this server"
    exit 1
  fi
fi

if [ ! -x "$JAVA_HOME/bin/jrunscript" ]; then
    echo "The JAVA_HOME environment variable does not point to a JDK installation with scripting support"
    echo "JDK jrunscript is needed to run this server"
    exit 1
fi

if ! "$JAVA_HOME/bin/jrunscript" -q 2>&1 |grep ECMAScript >/dev/null; then
    echo "The JAVA_HOME environment variable does not point to a JDK installation with ECMAScript support"
    echo "JDK with ECMAScript support is needed to run this server"
    exit 1
fi

if [ -z "$JAVA" ] ; then
  JAVA="$JAVA_HOME/bin/java"
fi

if [ -z "$PID" ] ; then
  PID="$BASEDIR/run/$NAME.pid"
fi

if [ -z "$LIB" ] ; then
  LIB="$BASEDIR"/lib
fi

if [ -z "$TMPDIR" ] ; then
  # Define the java.io.tmpdir to use
  TMPDIR="$BASEDIR"/tmp
fi

if [ -z "$LOGGING" ] ; then
  LOGGING="$BASEDIR/etc/logging.properties"
fi

if [ -z "$MAIL" ] ; then
  MAIL="$BASEDIR/etc/mail.properties"
fi

for JAR in "$BASEDIR"/lib/*.jar ; do
  if [ ! -z "$CLASSPATH" ] ; then
    CLASSPATH="$CLASSPATH":
  fi
  CLASSPATH="$CLASSPATH$JAR"
done

if [ -z "$JAVA_OPTS" ] ; then
  JAVA_OPTS="-Xmx512m"
fi

if [ -z "$DAEMON_GROUP" ] ; then
  DAEMON_GROUP="$SUDO_GID"
fi

if [ -z "$DAEMON_USER" ] ; then
  DAEMON_USER="$SUDO_USER"
fi

if [ -z "$DAEMON_USER" ] ; then
  DAEMON_USER="$USER"
fi

if [ -z "$JSVC_OPTS" ] ; then
  JSVC_OPTS="$JAVA_OPTS"
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

if [ -z "$OPTS" ] ; then
  if [ "$SECURITY_MANAGER" = "false" ]; then
    OPTS="--trust"
  fi
  if [ -z "$STORE" ]; then
    OPTS="-d \"$BASEDIR\" -p $PORT -a $AUTHORITY $OPTS"
  else
    OPTS="-d \"$BASEDIR\" -p $PORT -a $AUTHORITY -r \"$STORE\" $OPTS"
  fi
fi

# For Cygwin, switch paths to Windows format before running java
if $cygwin; then
  JAVA_HOME=`cygpath --absolute --windows "$JAVA_HOME"`
  BASEDIR=`cygpath --absolute --windows "$BASEDIR"`
  TMPDIR=`cygpath --absolute --windows "$TMPDIR"`
  CLASSPATH=`cygpath --path --windows "$CLASSPATH"`
fi

if [ ! -z "$DAEMON_GROUP" ] ; then
  umask 002
fi

if [ ! -z "$DAEMON_USER" ] ; then
  if [ ! -e "$BASEDIR/repositories" ] ; then
    mkdir "$BASEDIR/repositories"
    chown "$DAEMON_USER" "$BASEDIR/repositories"
    chmod u+s "$BASEDIR/repositories"
    if [ ! -z "$DAEMON_GROUP" ] ; then
      chown ":$DAEMON_GROUP" "$BASEDIR/repositories"
      chmod g+rwxs "$BASEDIR/repositories"
    fi
  fi
  if [ ! -e "$BASEDIR/log" ] ; then
    mkdir "$BASEDIR/log"
    chown "$DAEMON_USER" "$BASEDIR/log"
    chmod u+s "$BASEDIR/log"
    if [ ! -z "$DAEMON_GROUP" ] ; then
      chown ":$DAEMON_GROUP" "$BASEDIR/log"
      chmod g+rwxs "$BASEDIR/log"
    fi
  fi
  if [ ! -e "$TMPDIR" ] ; then
    mkdir "$TMPDIR"
    chown "$DAEMON_USER" "$TMPDIR"
    chmod u+s "$TMPDIR"
    if [ ! -z "$DAEMON_GROUP" ] ; then
      chown ":$DAEMON_GROUP" "$TMPDIR"
      chmod g+rwxs "$TMPDIR"
    fi
  fi
fi

mkdir -p `dirname "$PID"`
if [ ! -e "$BASEDIR/log" ] ; then
  mkdir "$BASEDIR/log"
fi

MAINCLASS=org.callimachusproject.Server

if [ "$1" = "start" ] ; then ################################

  if [ ! -z "$PID" ]; then
    if [ -f "$PID" ]; then
      echo "PID file ($PID) found. Is the server still running? Start aborted."
      exit 1
    fi
  fi

  if [ "`tty`" != "not a tty" ]; then
    echo "Using BASEDIR:   $BASEDIR"
    echo "Using PORT:      $PORT"
    echo "Using AUTHORITY: $AUTHORITY"
    echo "Using JAVA_HOME: $JAVA_HOME"
  fi

  shift
  "$EXECUTABLE" -home "$JAVA_HOME" -jvm server -procname "$NAME" \
    -pidfile "$PID" \
    -Duser.home="$BASEDIR" \
    -Djava.library.path="$LIB" \
    -Djava.io.tmpdir="$TMPDIR" \
    -Djava.util.logging.config.file="$LOGGING" \
    -Djava.mail.properties="$MAIL" \
    -classpath "$CLASSPATH" \
    -user $DAEMON_USER \
    $JSVC_OPTS $MAINCLASS -q $OPTS "$@"

  RETURN_VAL=$?
  sleep 1

  if [ $RETURN_VAL -gt 0 -o ! -f "$PID" ]; then
    echo "The server did not start, see log files for details. Start aborted."
    exit $RETURN_VAL
  fi

  SLEEP=120
  ID=`cat $PID`
  while [ $SLEEP -ge 0 ]; do
    kill -0 $ID >/dev/null 2>&1
    if [ $? -gt 0 ]; then
      echo "The server is not running, see log files for details. Start aborted."
      exit 1
    fi
    if netstat -ltpn 2>/dev/null |grep -e ":$PORT\b" |grep -qe "\b$ID\b"; then
      break
    fi
    if [ $SLEEP -gt 0 ]; then
      sleep 1
    fi
    if [ $SLEEP -eq 0 ]; then
      if [ "`tty`" != "not a tty" ]; then
        echo "The server is still starting up, check log files for possible errors."
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
    echo "The PID ($PID) does not exist. Is the server running? Stop aborted."
    exit 1
  fi

  ID=`cat $PID`

  "$EXECUTABLE" -home "$JAVA_HOME" -stop \
    -pidfile "$PID" $MAINCLASS

  SLEEP=180
  while [ $SLEEP -ge 0 ]; do 
    kill -0 $ID >/dev/null 2>&1
    if [ $? -gt 0 ]; then
      rm -f $PID
      break
    fi
    if [ "$SLEEP" = "60" ]; then
      kill $ID >/dev/null 2>&1
    fi
    if [ $SLEEP -gt 0 ]; then
      sleep 1
    fi
    SLEEP=`expr $SLEEP - 1 `
  done

  if [ -f "$PID" ]; then
    echo "Killing: $ID"
    rm -f $PID
    kill -9 $ID
	sleep 5
    if [ -f "$PID" ]; then
      "$EXECUTABLE" -home "$JAVA_HOME" -stop \
        -pidfile "$PID" $MAINCLASS >/dev/null 2>&1
    fi
  fi

else ################################

  if [ -f "$PID" ]; then
    echo "PID file ($PID) found. Is the server still running? Run aborted."
    exit 1
   fi

  if [ "`tty`" != "not a tty" ]; then
    echo "Using BASEDIR:   $BASEDIR"
    echo "Using PORT:      $PORT"
    echo "Using AUTHORITY: $AUTHORITY"
    echo "Using JAVA_HOME: $JAVA_HOME"
  fi

  if [ "$PORT" -le 1024 -a \( -z "$DAEMON_USER" -o "$DAEMON_USER" != "root" \) ]; then
    "$EXECUTABLE" -nodetach -home "$JAVA_HOME" -jvm server -procname "$NAME" \
      -pidfile "$PID" \
      -Duser.home="$BASEDIR" \
      -Djava.library.path="$LIB" \
      -Djava.io.tmpdir="$TMPDIR" \
      -Djava.util.logging.config.file="$LOGGING" \
      -Djava.mail.properties="$MAIL" \
      -classpath "$CLASSPATH" \
      -user $DAEMON_USER \
      $JSVC_OPTS $MAINCLASS -q $OPTS "$@"

    RETURN_VAL=$?
    sleep 1

    if [ $RETURN_VAL -gt 0 ]; then
      echo "The server terminated abnormally, see log files for details."
      exit $RETURN_VAL
    fi
  else
    exec "$JAVA" -server \
      -Duser.home="$BASEDIR" \
      -Djava.library.path="$LIB" \
      -Djava.io.tmpdir="$TMPDIR" \
      -Djava.util.logging.config.file="$LOGGING" \
      -classpath "$CLASSPATH" \
      $JAVA_OPTS $MAINCLASS --pid "$PID" $OPTS "$@"
  fi
fi

