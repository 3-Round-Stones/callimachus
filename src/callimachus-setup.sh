#!/bin/sh
#
# Portions Copyright (c) 2011-2013 3 Round Stones Inc., Some Rights Reserved
# Portions Copyright (c) 2009-10 Zepheira LLC, Some Rights Reserved
# Portions Copyright (c) 2010-11 Talis Inc, Some Rights Reserved
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

# Author: James Leigh <james@3roundstones.com>

# PATH should only include /usr/* if it runs after the mountnfs.sh script
PATH=/sbin:/usr/sbin:/bin:/usr/bin

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

PRGDIR=$(cd `dirname "$PRG"`;pwd)

if [ -z "$NAME" ] ; then
  NAME=`basename "$PRG" | perl -pe 's/-\w+.sh$//' 2>/dev/null`
fi

# Only set BASEDIR if not already set
[ -z "$BASEDIR" ] && BASEDIR=`cd "$PRGDIR/.." >/dev/null; pwd`

# Read relative config paths from BASEDIR
cd "$BASEDIR"

# Read configuration variable file if it is present
[ -r "/etc/default/$NAME" ] && . "/etc/default/$NAME"

# Load the VERBOSE setting and other rcS variables
[ -r /lib/init/vars.sh ] && . /lib/init/vars.sh

if [ "`tty`" != "not a tty" ]; then
  VERBOSE="yes"
fi

if [ -z "$CONFIG" ] ; then
  CONFIG="$BASEDIR/etc/$NAME.conf"
fi

if [ ! -e "$CONFIG" -a -r "$BASEDIR/etc/$NAME-defaults.conf" ]; then
  cp "$BASEDIR/etc/$NAME-defaults.conf" "$CONFIG"
fi

if [ -r "$CONFIG" ]; then
  . "$CONFIG" 2>/dev/null
fi

MAINCLASS=org.callimachusproject.Setup

# Ensure that any user defined CLASSPATH variables are not used on startup.
CLASSPATH=

# Define LSB log_* functions.
log_success_msg () {
  echo $1
}
log_failure_msg () {
  echo $1 2>&1
}
log_warning_msg () {
  echo $1 2>&1
}

cygwin=false
darwin=false
case "`uname`" in
CYGWIN*) cygwin=true;;
Darwin*) darwin=true;;
esac

# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin; then
  [ -n "$JAVA_HOME" ] && JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
  [ -n "$JDK_HOME" ] && BASEDIR=`cygpath --unix "$JDK_HOME"`
  [ -n "$BASEDIR" ] && BASEDIR=`cygpath --unix "$BASEDIR"`
  [ -n "$CLASSPATH" ] && CLASSPATH=`cygpath --path --unix "$CLASSPATH"`
fi

# check the base dir for possible java candidates
if [ -z "$JDK_HOME" ] && ls "$BASEDIR"/*/lib/tools.jar >/dev/null 2>&1
then
  TOOLS=`ls -1 "$BASEDIR"/*/lib/tools.jar | head -n 1`
  if [ -e "$TOOLS" ] ; then
    JDK_HOME=$(dirname "$(dirname "$TOOLS")")
    # verify java instance
    if [ -x "$JDK_HOME/jre/bin/java" ] ; then
      JAVA_HOME="$JDK_HOME/jre"
    elif [ -x "$JDK_HOME/bin/java" ] ; then
      JAVA_HOME="$JDK_HOME"
    fi
  fi
fi

# use 'java_home' to search for other possible java candidate
if [ -z "$JAVA_HOME" -a -x /usr/libexec/java_home ] ; then
  JAVA_HOME=`/usr/libexec/java_home`
fi

# use 'which' to search for other possible java candidate
if [ -z "$JAVA_HOME" ] ; then
  JAVA=`which java 2>/dev/null`
  if [ -x "$JAVA" ] ; then
    while [ -h "$JAVA" ] ; do
      ls=`ls -ld "$JAVA"`
      link=`expr "$ls" : '.*-> \(.*\)$'`
      if expr "$link" : '/.*' > /dev/null; then
        JAVA="$link"
      else
        JAVA=`dirname "$JAVA"`/"$link"
      fi
    done
    JHOME=`echo "$JAVA" | awk '{ print substr($1, 1, length($1)-9); }'`
    if [ -d "$JHOME" ] ; then
      JAVA_HOME="$JHOME"
    else
      JAVA_HOME=`which java 2>/dev/null | awk '{ print substr($1, 1, length($1)-9); }'`
    fi
  fi
fi

if [ -z "$JDK_HOME" -a -x "$JAVA_HOME/../lib/tools.jar" ]; then
  JDK_HOME="$JAVA_HOME/.."
elif [ -z "$JDK_HOME" -a -x "$JAVA_HOME/lib/tools.jar" ]; then
  JDK_HOME="$JAVA_HOME"
elif [ -z "$JDK_HOME" -a -x "$JAVA_HOME/../Classes/classes.jar" ]; then
  JDK_HOME="$JAVA_HOME"
fi

# use 'which' to search for other possible java candidate
if [ -z "$JDK_HOME" ] ; then
  JAVAC=`which javac 2>/dev/null`
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
    JHOME=`echo "$JAVAC" | awk '{ print substr($1, 1, length($1)-10); }'`
    if [ -d "$JHOME" ] ; then
      JDK_HOME="$JHOME"
    else
      JDK_HOME=`which javac 2>/dev/null | awk '{ print substr($1, 1, length($1)-10); }'`
    fi
  fi
fi

if [ ! -e "$JAVA_HOME/bin/java" ] ; then
  log_failure_msg "$JAVA_HOME/bin/java is not executable"
  log_failure_msg "This file is needed to run this program"
  exit 5
fi

if [ ! -e "$JDK_HOME/bin/javac" ] ; then
  log_failure_msg "$JDK_HOME/bin/javac is not executable"
  log_failure_msg "This file is needed to run this program"
  exit 5
fi

if [ -z "$MAIL" ] ; then
  MAIL="$BASEDIR/etc/mail.properties"
fi

if [ -z "$REPOSITORY_CONFIG" ] ; then
  REPOSITORY_CONFIG="$BASEDIR/etc/$NAME-repository.ttl"
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

if [ -z "$JSVC_OPTS" ] ; then
  JSVC_OPTS="$JAVA_OPTS"
fi

# For Cygwin, switch paths to Windows format before running java
if $cygwin; then
  JAVA_HOME=`cygpath --absolute --windows "$JAVA_HOME"`
  JDK_HOME=`cygpath --absolute --windows "$JDK_HOME"`
  BASEDIR=`cygpath --absolute --windows "$BASEDIR"`
  CLASSPATH=`cygpath --path --windows "$CLASSPATH"`
fi

if [ -z "$PORT" -a -z "$SSLPORT" ] ; then
  PORT="8080"
  sed -i "s:#\?\s*PORT=.*:PORT=$PORT:" "$CONFIG"
fi

if [ -z "$ORIGIN" ] ; then
  if [ -n "$AUTHORITY" ] ; then
    ORIGIN="http://$AUTHORITY"
  elif [ -n "$PORT" ] ; then
    ORIGIN="http://localhost"
    if [ "$PORT" != "80" ] ; then
      ORIGIN="$ORIGIN:$PORT"
    fi
  elif [ -n "$SSLPORT" ] ; then
    ORIGIN="https://localhost"
    if [ "$SSLPORT" != "443" ] ; then
      ORIGIN="$ORIGIN:$SSLPORT"
    fi
  fi
  sed -i "s%#\?\s*ORIGIN=.*%ORIGIN=$ORIGIN%" "$CONFIG"
fi

if [ "$VERBOSE" != no ]; then
  log_success_msg "Using BASEDIR:   $BASEDIR"
  log_success_msg "Using PORT:      $PORT $SSLPORT"
  log_success_msg "Using ORIGIN:    $ORIGIN"
  log_success_msg "Using JAVA_HOME: $JAVA_HOME"
  log_success_msg "Using JDK_HOME:  $JDK_HOME"
fi

exec "$JAVA_HOME/bin/java" \
    -Djava.mail.properties="$MAIL" \
    -Dorg.callimachusproject.config.repository="$REPOSITORY_CONFIG" \
    -Dorg.callimachusproject.config.webapp="$(ls $BASEDIR/lib/$NAME-webapp*.car)" \
    -classpath "$CLASSPATH" \
    -XX:OnOutOfMemoryError="kill -9 %p" \
    $JAVA_OPTS "$MAINCLASS" \
    -b "$BASEDIR" -c "$CONFIG" -k "$BASEDIR/backups" -e -l "/bin/sh bin/$NAME-start.sh" "$@"

