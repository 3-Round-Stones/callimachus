#!/bin/sh
#
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
elif [ `uname -m` = "i686" ]; then
  EXECUTABLE="$PRGDIR/$NAME-linux-i686"
else
  EXECUTABLE="jsvc"
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

# Read relative config paths from BASEDIR
cd "$BASEDIR"

# Check that target executable exists
if [ ! -f "$EXECUTABLE" ]; then
  echo "Cannot find $EXECUTABLE"
  echo "This file is needed to run this program"
  exit 1
fi

# Check that target is executable
if [ ! -x "$EXECUTABLE" ]; then
  chmod a+x "$EXECUTABLE"
  # Check that target is executable
  if [ ! -x "$EXECUTABLE" ]; then
    echo "$EXECUTABLE is not executable"
    echo "This file is needed to run this program"
    exit 1
  fi
fi

# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin; then
  [ -n "$JAVA_HOME" ] && JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
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
  JAVA=`which java`
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
    HOME=`echo "$JAVA" | awk '{ print substr($1, 1, length($1)-9); }'`
    if [ -d "$HOME" ] ; then
      JAVA_HOME="$HOME"
    else
      JAVA_HOME=`which java | awk '{ print substr($1, 1, length($1)-9); }'`
    fi
  fi
fi

if [ -z "$JDK_HOME" -a -x "$JAVA_HOME/../lib/tools.jar" ]; then
  JDK_HOME="$JAVA_HOME/.."
elif [ -z "$JDK_HOME" -a -x "$JAVA_HOME/lib/tools.jar" ]; then
  JDK_HOME="$JAVA_HOME"
fi

# use 'which' to search for other possible java candidate
if [ -z "$JDK_HOME" ] ; then
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
      JDK_HOME="$HOME"
    else
      JDK_HOME=`which javac | awk '{ print substr($1, 1, length($1)-10); }'`
    fi
  fi
fi

if [ -z "$PID" ] ; then
  PID="$BASEDIR/run/$NAME.pid"
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

if [ -z "$SSL" ] ; then
  SSL="$BASEDIR/etc/ssl.properties"
fi

if [ -z "$SSL_OPTS" -a -e "$SSL" ] ; then
  SSL_OPTS=$(perl -pe 's/\s*\#.*$//g' "$SSL" 2>/dev/null |perl -pe 's/(\S+)=(.*)/-D$1=$2/' 2>/dev/null |tr -s '\n' ' ')
fi

if [ -z "$REPOSITORY" ] ; then
  REPOSITORY="$BASEDIR/repositories/$NAME"
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

if [ -z "$PORT" -a -z "$SSLPORT" ] ; then
  PORT="8080"
fi

if [ -z "$ORIGIN" ] ; then
  if [ -n "$AUTHORITY" ] ; then
    ORIGIN="http://$AUTHORITY"
  elif [ -n "$PORT" ] ; then
    ORIGIN="http://$(hostname -f)"
    if [ "$PORT" != "80" ] ; then
      ORIGIN="$ORIGIN:$PORT"
    fi
  elif [ -n "$SSLPORT" ] ; then
    ORIGIN="https://$(hostname -f)"
    if [ "$SSLPORT" != "443" ] ; then
      ORIGIN="$ORIGIN:$SSLPORT"
    fi
  fi
fi

PORT_OPTS="$(echo $PORT |perl -pe 's/(^|\s)(\S)/ -p $2/g' 2>/dev/null) $(echo $SSLPORT |perl -pe 's/(^|\s)(\S)/ -s $2/g' 2>/dev/null)"
ORIGIN_OPTS="-o $(echo $ORIGIN |perl -pe 's/(\s)(\S)/ -o $2/g' 2>/dev/null)"

if [ -z "$OPTS" ] ; then
  if [ "$SECURITY_MANAGER" = "false" ]; then
    OPTS="--trust"
  fi
  OPTS="$PORT_OPTS $ORIGIN_OPTS $OPTS"
fi

# For Cygwin, switch paths to Windows format before running java
if $cygwin; then
  JAVA_HOME=`cygpath --absolute --windows "$JAVA_HOME"`
  JDK_HOME=`cygpath --absolute --windows "$JDK_HOME"`
  BASEDIR=`cygpath --absolute --windows "$BASEDIR"`
  TMPDIR=`cygpath --absolute --windows "$TMPDIR"`
  CLASSPATH=`cygpath --path --windows "$CLASSPATH"`
fi

if [ ! -z "$DAEMON_GROUP" ] ; then
  umask 002
fi

if [ ! -z "$DAEMON_USER" ] ; then
  chown -R --from=root "$DAEMON_USER" "$BASEDIR/repositories"
  if [ ! -z "$DAEMON_GROUP" ] ; then
    chown -R --from=:root ":$DAEMON_GROUP" "$BASEDIR/repositories"
  fi
  if [ ! -e "$BASEDIR/log" ] ; then
    mkdir "$BASEDIR/log"
  fi
  chown --from=root "$DAEMON_USER" "$BASEDIR/log"
  if [ ! -z "$DAEMON_GROUP" ] ; then
    chown --from=:root ":$DAEMON_GROUP" "$BASEDIR/log"
  fi
  if [ ! -e "$TMPDIR" ] ; then
    mkdir "$TMPDIR"
    chown "$DAEMON_USER" "$TMPDIR"
    if [ ! -z "$DAEMON_GROUP" ] ; then
      chown ":$DAEMON_GROUP" "$TMPDIR"
    fi
  fi
fi

mkdir -p "$(dirname "$PID")"
if [ ! -e "$BASEDIR/log" ] ; then
  mkdir "$BASEDIR/log"
fi

MAINCLASS=org.callimachusproject.Server
MONITORCLASS=org.callimachusproject.ServerMonitor

if [ "$1" = "start" ] ; then ################################

  if [ ! -z "$PID" ]; then
    if [ -f "$PID" ]; then
      echo "PID file ($PID) found. Is the server still running? Start aborted." 2>&1
      exit 1
    fi
  fi

  LSOF_OPTS="$(echo $PORT |perl -pe 's/(^|\s)(\S)/ -i :$2/g' 2>/dev/null) $(echo $SSLPORT |perl -pe 's/(^|\s)(\S)/ -i :$2/g' 2>/dev/null)"
  if lsof $LSOF_OPTS ; then
    echo "Cannot bind to port $PORT $SSLPORT please ensure nothing is already listening on this port" 2>&1
    exit 2
  fi

  if [ "`tty`" != "not a tty" ]; then
    echo "Using BASEDIR:   $BASEDIR"
    echo "Using PORT:      $PORT $SSLPORT"
    echo "Using ORIGIN:    $ORIGIN"
    echo "Using JAVA_HOME: $JAVA_HOME"
    echo "Using JDK_HOME:  $JDK_HOME"
  fi

  JSVC_LOG="$BASEDIR/log/callimachus-start.log"
  if [ -e "$JSVC_LOG" ]; then
    rm "$JSVC_LOG"
  fi
  shift
  "$EXECUTABLE" -jvm server \
    -outfile "$JSVC_LOG" -errfile '&1' \
    -procname "$NAME" \
    -home "$JAVA_HOME" \
    -pidfile "$PID" \
    -Duser.home="$BASEDIR" \
    -Djava.io.tmpdir="$TMPDIR" \
    -Djava.util.logging.config.file="$LOGGING" \
    -Djava.mail.properties="$MAIL" \
    -classpath "$CLASSPATH" \
    -user "$DAEMON_USER" \
    $JSVC_OPTS $SSL_OPTS "$MAINCLASS" -q -d "$BASEDIR" -r "$REPOSITORY" $OPTS "$@"

  RETURN_VAL=$?
  sleep 1
  cat "$JSVC_LOG"

  if [ $RETURN_VAL -gt 0 -o ! -f "$PID" ]; then
    if [ "$(ls -A "$BASEDIR/log")" ]; then
      echo "The server did not start, see log files for details. Start aborted." 2>&1
    else
      echo "The server did not start properly. Ensure it is not running and run $0" 2>&1
    fi
    exit $RETURN_VAL
  fi

  SLEEP=120
  ID=`cat "$PID"`
  while [ $SLEEP -ge 0 ]; do
    kill -0 $ID >/dev/null 2>&1
    if [ $? -gt 0 ]; then
      echo "The server is not running, see log files for details. Start aborted." 2>&1
      exit 1
    fi
    if lsof $LSOF_OPTS |grep -qe "\b$ID\b"; then
      break
    fi
    if [ $SLEEP -gt 0 ]; then
      sleep 2
    fi
    if [ $SLEEP -eq 0 ]; then
      if [ "`tty`" != "not a tty" ]; then
        echo "The server is still starting up, check log files for possible errors." 2>&1
      fi
      break
    fi
    SLEEP=`expr $SLEEP - 1 `
  done

elif [ "$1" = "stop" ] ; then ################################

  if [ -f "$PID" -a -r "$PID" ]; then
    kill -0 `cat "$PID"` >/dev/null 2>&1
    if [ $? -gt 0 ]; then
      rm -f "$PID"
      if [ $? -gt 0 ]; then
        echo "PID file ($PID) found but no matching process was found. Stop aborted." 2>&1
        exit $?
      fi
      exit $?
    fi
  elif [ -f "$PID" ]; then
    echo "The PID ($PID) exist, but it cannot be read. Stop aborted." 2>&1
    exit 1
  else
    echo "The PID ($PID) does not exist. Is the server running? Stop aborted." 2>&1
    exit 1
  fi

  ID=`cat "$PID"`

  "$EXECUTABLE" -home "$JAVA_HOME" -stop \
    -pidfile "$PID" "$MAINCLASS"

  SLEEP=180
  while [ $SLEEP -ge 0 ]; do 
    kill -0 $ID >/dev/null 2>&1
    if [ $? -gt 0 ]; then
      rm -f "$PID"
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
    rm -f "$PID"
    kill -9 "$ID"
	sleep 5
    if [ -f "$PID" ]; then
      "$EXECUTABLE" -home "$JAVA_HOME" -stop \
        -pidfile "$PID" "$MAINCLASS" >/dev/null 2>&1
    fi
  fi

elif [ "$1" = "dump" ] ; then ################################

  if [ -f "$PID" -a -r "$PID" ]; then
    kill -0 `cat "$PID"` >/dev/null 2>&1
    if [ $? -gt 0 ]; then
      rm -f "$PID"
      if [ $? -gt 0 ]; then
        echo "PID file ($PID) found but no matching process was found. Dump aborted." 2>&1
        exit $?
      fi
      exit $?
    fi
  elif [ -f "$PID" ]; then
    echo "The PID ($PID) exist, but it cannot be read. Dump aborted." 2>&1
    exit 1
  else
    echo "The PID ($PID) does not exist. Is the server running? Dump aborted." 2>&1
    exit 1
  fi

  DATE=`date +%Y-%m-%d`
  DIR="$BASEDIR/log/$DATE"
  if [ ! -e "$DIR" ] ; then
    mkdir "$DIR"
    if [ ! -z "$DAEMON_USER" ] ; then
      chown "$DAEMON_USER" "$DIR"
      if [ ! -z "$DAEMON_GROUP" ] ; then
        chown ":$DAEMON_GROUP" "$DIR"
      fi
    fi
  fi

  JSVC_LOG="$BASEDIR/log/callimachus-dump.log"
  if [ -e "$JSVC_LOG" ]; then
    rm "$JSVC_LOG"
  fi
  "$EXECUTABLE" -nodetach -outfile "$JSVC_LOG" -errfile '&1' -home "$JAVA_HOME" -jvm server -procname "$NAME" \
    -pidfile "$BASEDIR/run/$NAME-dump.pid" \
    -Duser.home="$BASEDIR" \
    -Djava.io.tmpdir="$TMPDIR" \
    -Djava.mail.properties="$MAIL" \
    -classpath "$CLASSPATH:$JDK_HOME/lib/tools.jar" \
    -user "$DAEMON_USER" \
    $JSVC_OPTS $SSL_OPTS "$MONITORCLASS" --pid "$PID" --dump "$DIR"
  RETURN_VAL=$?
  cat "$JSVC_LOG"
  exit $RETURN_VAL

elif [ "$1" = "reset" ] ; then ################################

  if [ -f "$PID" -a -r "$PID" ]; then
    kill -0 `cat "$PID"` >/dev/null 2>&1
    if [ $? -gt 0 ]; then
      rm -f "$PID"
      if [ $? -gt 0 ]; then
        echo "PID file ($PID) found but no matching process was found. Reset aborted." 2>&1
        exit $?
      fi
      exit $?
    fi
  elif [ -f "$PID" ]; then
    echo "The PID ($PID) exist, but it cannot be read. Reset aborted." 2>&1
    exit 1
  else
    echo "The PID ($PID) does not exist. Is the server running? Reset aborted." 2>&1
    exit 1
  fi

  JSVC_LOG="$BASEDIR/log/callimachus-reset.log"
  if [ -e "$JSVC_LOG" ]; then
    rm "$JSVC_LOG"
  fi
  "$EXECUTABLE" -nodetach -outfile "$JSVC_LOG" -errfile '&1' -home "$JAVA_HOME" -jvm server -procname "$NAME" \
    -pidfile "$BASEDIR/run/$NAME-dump.pid" \
    -Duser.home="$BASEDIR" \
    -Djava.io.tmpdir="$TMPDIR" \
    -Djava.mail.properties="$MAIL" \
    -classpath "$CLASSPATH:$JDK_HOME/lib/tools.jar" \
    -user "$DAEMON_USER" \
    $JSVC_OPTS $SSL_OPTS "$MONITORCLASS" --pid "$PID" --reset
  RETURN_VAL=$?
  cat "$JSVC_LOG"
  exit $RETURN_VAL

else ################################

  if [ -f "$PID" ]; then
    echo "PID file ($PID) found. Is the server still running? Run aborted." 2>&1
    exit 1
   fi

  if [ "`tty`" != "not a tty" ]; then
    echo "Using BASEDIR:   $BASEDIR"
    echo "Using PORT:      $PORT $SSLPORT"
    echo "Using ORIGIN:    $ORIGIN"
    echo "Using JAVA_HOME: $JAVA_HOME"
    echo "Using JDK_HOME:  $JDK_HOME"
  fi

  exec "$EXECUTABLE" -debug -showversion -nodetach -home "$JAVA_HOME" -jvm server -procname "$NAME" \
    -pidfile "$PID" \
    -Duser.home="$BASEDIR" \
    -Djava.io.tmpdir="$TMPDIR" \
    -Djava.util.logging.config.file="$LOGGING" \
    -Djava.mail.properties="$MAIL" \
    -classpath "$CLASSPATH" \
    -user "$DAEMON_USER" \
    $JSVC_OPTS $SSL_OPTS "$MAINCLASS" -q -d "$BASEDIR" -r "$REPOSITORY" $OPTS "$@"
fi

