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
### 
# chkconfig: 345 85 60
# description: Linked Data Management System
# processname: callimachus
### BEGIN INIT INFO
# Provides:          callimachus
# Required-Start:    $remote_fs $network $syslog
# Required-Stop:     $remote_fs $syslog
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: Linked Data Management System
# Description:       Callimachus is a framework for data-driven applications based on Linked Data principles.
### END INIT INFO

# Author: James Leigh <james@3roundstones.com>

# PATH should only include /usr/* if it runs after the mountnfs.sh script
PATH=/sbin:/usr/sbin:/bin:/usr/bin
DESC="Linked Data Management System"

# Define LSB log_* functions.
if [ -r /lib/lsb/init-functions ]; then
  . /lib/lsb/init-functions
else
  log_success_msg () {
    echo $1
  }
  log_failure_msg () {
    echo $1 2>&1
  }
  log_warning_msg () {
    echo $1 2>&1
  }
fi

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
  NAME=`basename "$PRG" | sed 's/\.sh$//'`
fi

cygwin=false
darwin=false
case "`uname`" in
CYGWIN*) cygwin=true;;
Darwin*) darwin=true;;
esac

if $darwin; then
  DAEMON="$PRGDIR/$NAME-darwin"
elif [ `uname -m` = "x86_64" ]; then
  DAEMON="$PRGDIR/$NAME-linux-x86_64"
elif [ `uname -m` = "i686" ]; then
  DAEMON="$PRGDIR/$NAME-linux-i686"
else
  DAEMON="jsvc"
fi

# Only set BASEDIR if not already set
[ -z "$BASEDIR" ] && BASEDIR=`cd "$PRGDIR/.." >/dev/null; pwd`

PIDFILE="$BASEDIR/run/$NAME.pid"
SCRIPTNAME=/etc/init.d/$NAME

# Read relative config paths from BASEDIR
cd "$BASEDIR"

# Check that target executable exists
if [ ! -f "$DAEMON" ]; then
  log_failure_msg "Cannot find $DAEMON"
  log_failure_msg "This file is needed to run this program"
  exit 5
fi

# Check that target is executable
if [ ! -x "$DAEMON" ]; then
  chmod a+x "$DAEMON"
  # Check that target is executable
  if [ ! -x "$DAEMON" ]; then
    log_failure_msg "$DAEMON is not executable"
    log_failure_msg "This file is needed to run this program"
    exit 5
  fi
fi

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

if [ ! -r "$CONFIG" ]; then
  CONFIG="$BASEDIR/etc/$NAME-defaults.conf"
fi

if [ -r "$CONFIG" ]; then
  . "$CONFIG" 2>/dev/null
fi

# Ensure that any user defined CLASSPATH variables are not used on startup.
CLASSPATH=

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
    HOME=`echo "$JAVA" | awk '{ print substr($1, 1, length($1)-9); }'`
    if [ -d "$HOME" ] ; then
      JAVA_HOME="$HOME"
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
    HOME=`echo "$JAVAC" | awk '{ print substr($1, 1, length($1)-10); }'`
    if [ -d "$HOME" ] ; then
      JDK_HOME="$HOME"
    else
      JDK_HOME=`which javac 2>/dev/null | awk '{ print substr($1, 1, length($1)-10); }'`
    fi
  fi
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

if [ -z "$PRIMARY_ORIGIN" ] ; then
  PRIMARY_ORIGIN="$ORIGIN"
fi

PORT_OPTS="$(echo $PORT |perl -pe 's/(^|\s)(\S)/ -p $2/g' 2>/dev/null) $(echo $SSLPORT |perl -pe 's/(^|\s)(\S)/ -s $2/g' 2>/dev/null)"
ORIGIN_OPTS="-o $(echo $ORIGIN |perl -pe 's/(\s)(\S)/ -o $2/g' 2>/dev/null)"
PRIMARY_ORIGIN_OPTS="-o $(echo $PRIMARY_ORIGIN |perl -pe 's/(\s)(\S)/ -o $2/g' 2>/dev/null)"

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

mkdir -p "$(dirname "$PIDFILE")"
if [ ! -e "$BASEDIR/log" ] ; then
  mkdir "$BASEDIR/log"
fi

if [ ! -z "$DAEMON_USER" ] ; then
  if [ ! -e "$BASEDIR/log" ] ; then
    mkdir "$BASEDIR/log"
  fi
  chown "$DAEMON_USER" "$BASEDIR/log"
  if [ ! -z "$DAEMON_GROUP" ] ; then
    chown ":$DAEMON_GROUP" "$BASEDIR/log"
  fi
  if [ -e "$BASEDIR/repositories" ]; then
    chown -R "$DAEMON_USER" "$BASEDIR/repositories"
    if [ ! -z "$DAEMON_GROUP" ] ; then
      chown -R ":$DAEMON_GROUP" "$BASEDIR/repositories"
    fi
  fi
  if [ -e "$SSL" ]; then
    chown -R "$DAEMON_USER" "$SSL"
    if [ ! -z "$DAEMON_GROUP" ] ; then
      chown -R ":$DAEMON_GROUP" "$SSL"
    fi
    KEYSTORE=$(grep -E '^javax.net.ssl.keyStore=' $SSL |perl -pe 's/^javax.net.ssl.keyStore=(.*)/$1/' 2>/dev/null)
    if [ -n "$KEYSTORE" -a -e "$BASEDIR/$KEYSTORE" ]; then
      chown -R "$DAEMON_USER" "$BASEDIR/$KEYSTORE"
      if [ ! -z "$DAEMON_GROUP" ] ; then
        chown -R ":$DAEMON_GROUP" "$BASEDIR/$KEYSTORE"
      fi
    fi
  fi
  if [ ! -e "$TMPDIR" ] ; then
    mkdir "$TMPDIR"
    chown "$DAEMON_USER" "$TMPDIR"
    if [ ! -z "$DAEMON_GROUP" ] ; then
      chown ":$DAEMON_GROUP" "$TMPDIR"
    fi
  fi
  if [ -e "$MAIL" ] ; then
    chown "$DAEMON_USER" "$MAIL"
    chown ":$DAEMON_GROUP" "$MAIL"
  fi
fi

MAINCLASS=org.callimachusproject.Server
SETUPCLASS=org.callimachusproject.Setup
MONITORCLASS=org.callimachusproject.ServerMonitor


#
# Function that starts the daemon/service
#
do_start()
{
  LSOF="$(which lsof 2>/dev/null)"
  LSOF_OPTS="$(echo $PORT |perl -pe 's/(^|\s)(\S)/ -i :$2/g' 2>/dev/null) $(echo $SSLPORT |perl -pe 's/(^|\s)(\S)/ -i :$2/g' 2>/dev/null)"
  if [ -n "$LSOF" ] && "$LSOF" $LSOF_OPTS ; then
    log_failure_msg "Cannot bind to port $PORT $SSLPORT please ensure nothing is already listening on this port"
    return 150
  fi

  if [ ! -e "$REPOSITORY" ]; then
    log_failure_msg "The repository $REPOSITORY does not exist, please run the setup script first"
    return 6
  fi

  JSVC_LOG="$BASEDIR/log/callimachus-start.log"
  if [ -e "$JSVC_LOG" ]; then
    rm "$JSVC_LOG"
  fi
  "$DAEMON" -jvm server \
    -outfile "$JSVC_LOG" -errfile '&1' \
    -procname "$NAME" \
    -home "$JAVA_HOME" \
    -pidfile "$PIDFILE" \
    -Duser.home="$BASEDIR" \
    -Djava.io.tmpdir="$TMPDIR" \
    -Djava.util.logging.config.file="$LOGGING" \
    -Djava.mail.properties="$MAIL" \
    -classpath "$CLASSPATH" \
    -user "$DAEMON_USER" \
    -XX:OnOutOfMemoryError="kill -9 %p" \
    $JSVC_OPTS $SSL_OPTS "$MAINCLASS" -q -d "$BASEDIR" -r "$REPOSITORY" $OPTS "$@"

  RETURN_VAL=$?
  sleep 1
  cat "$JSVC_LOG"

  if [ $RETURN_VAL -gt 0 -o ! -f "$PIDFILE" ]; then
    if [ "$(ls -A "$BASEDIR/log")" ]; then
      log_failure_msg "The server did not start, see log files for details. Start aborted."
    else
      log_failure_msg "The server did not start properly. Ensure it is not running and run $0"
    fi
    return $RETURN_VAL
  fi

  SLEEP=120
  ID=`cat "$PIDFILE"`
  while [ $SLEEP -ge 0 ]; do
    kill -0 $ID >/dev/null 2>&1
    if [ $? -gt 0 ]; then
      log_failure_msg "The server is not running, see log files for details. Start aborted."
      return 7
    fi
    if [ -n "$LSOF" ] && "$LSOF" $LSOF_OPTS |grep -qe "\b$ID\b"; then
      sleep 4
      break
    elif [ -n "$PORT" ]; then
      if netstat -ltpn 2>/dev/null |grep -e ":$PORT\b" |grep -qe "\b$ID\b"; then
        sleep 4
        break
      fi
    elif [ -n "$SSLPORT" ]; then
      if netstat -ltpn 2>/dev/null |grep -e ":$SSLPORT\b" |grep -qe "\b$ID\b"; then
        sleep 4
        break
      fi
    fi
    if [ $SLEEP -gt 0 ]; then
      sleep 2
    fi
    if [ $SLEEP -eq 0 ]; then
      if [ "`tty`" != "not a tty" ]; then
        log_warning_msg "The server is still starting up, check log files for possible errors."
      fi
      break
    fi
    SLEEP=`expr $SLEEP - 1 `
  done
}

#
# Function that stops the daemon/service
#
do_stop()
{
  ID=`cat "$PIDFILE"`

  "$DAEMON" -home "$JAVA_HOME" -stop \
    -pidfile "$PIDFILE" "$MAINCLASS"

  SLEEP=180
  while [ $SLEEP -ge 0 ]; do 
    kill -0 $ID >/dev/null 2>&1
    if [ $? -gt 0 ]; then
      rm -f "$PIDFILE"
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

  if [ -f "$PIDFILE" ]; then
    log_warning_msg "Killing: $ID"
    rm -f "$PIDFILE"
    kill -9 "$ID"
    sleep 5
    if [ -f "$PIDFILE" ]; then
      "$DAEMON" -home "$JAVA_HOME" -stop \
        -pidfile "$PIDFILE" "$MAINCLASS" >/dev/null 2>&1
    fi
  fi
}

#
# Function that loads the configuration and prompts for a password
#
do_setup() {
  "$JAVA_HOME/bin/java" \
    -Duser.home="$BASEDIR" \
    -Djava.io.tmpdir="$TMPDIR" \
    -Djava.mail.properties="$MAIL" \
    -classpath "$CLASSPATH" \
    -XX:OnOutOfMemoryError="kill -9 %p" \
    $JAVA_OPTS $SSL_OPTS "$SETUPCLASS" \
    $PRIMARY_ORIGIN_OPTS -c "$REPOSITORY_CONFIG" -f "/callimachus/=$(ls lib/$NAME*.car)" -l \
    -e "$EMAIL" -n "$FULLNAME" "$@"
  return $?
}

#
# Function that dumps the current state of the VM
#
do_dump() {
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
  "$DAEMON" -nodetach -outfile "$JSVC_LOG" -errfile '&1' -home "$JAVA_HOME" -jvm server -procname "$NAME" \
    -pidfile "$BASEDIR/run/$NAME-dump.pid" \
    -Duser.home="$BASEDIR" \
    -Djava.io.tmpdir="$TMPDIR" \
    -Djava.mail.properties="$MAIL" \
    -XX:OnOutOfMemoryError="kill -9 %p" \
    -classpath "$CLASSPATH:$JDK_HOME/lib/tools.jar:$JDK_HOME/../Classes/classes.jar" \
    -user "$DAEMON_USER" \
    $JSVC_OPTS $SSL_OPTS "$MONITORCLASS" --pid "$PIDFILE" --dump "$DIR"
  RETURN_VAL=$?
  cat "$JSVC_LOG"
  return $RETURN_VAL
}

#
# Function that resets the internal cache
#
do_reset() {
  JSVC_LOG="$BASEDIR/log/callimachus-reset.log"
  if [ -e "$JSVC_LOG" ]; then
    rm "$JSVC_LOG"
  fi
  "$DAEMON" -nodetach -outfile "$JSVC_LOG" -errfile '&1' -home "$JAVA_HOME" -jvm server -procname "$NAME" \
    -pidfile "$BASEDIR/run/$NAME-dump.pid" \
    -Duser.home="$BASEDIR" \
    -Djava.io.tmpdir="$TMPDIR" \
    -Djava.mail.properties="$MAIL" \
    -XX:OnOutOfMemoryError="kill -9 %p" \
    -classpath "$CLASSPATH:$JDK_HOME/lib/tools.jar:$JDK_HOME/../Classes/classes.jar" \
    -user "$DAEMON_USER" \
    $JSVC_OPTS $SSL_OPTS "$MONITORCLASS" --pid "$PIDFILE" --reset
  RETURN_VAL=$?
  cat "$JSVC_LOG"
  return $RETURN_VAL
}

#
# Function that runs the daemon in the foreground
#
do_run() {
  exec "$DAEMON" -debug -showversion -nodetach -home "$JAVA_HOME" -jvm server -procname "$NAME" \
    -pidfile "$PIDFILE" \
    -Duser.home="$BASEDIR" \
    -Djava.io.tmpdir="$TMPDIR" \
    -Djava.util.logging.config.file="$LOGGING" \
    -Djava.mail.properties="$MAIL" \
    -XX:OnOutOfMemoryError="kill -9 %p" \
    -classpath "$CLASSPATH" \
    -user "$DAEMON_USER" \
    $JSVC_OPTS $SSL_OPTS "$MAINCLASS" -q -d "$BASEDIR" -r "$REPOSITORY" $OPTS "$@"
}

case "$1" in
  start)
    # Return
    #  0    if the action was successful
    #  0    service is already running
    #  1    generic or unspecified error (current practice)
    #  2    invalid or excess argument(s)
    #  3    unimplemented feature (for example, "reload")
    #  4    user had insufficient privilege
    #  5    program is not installed
    #  6    program is not configured
    #  7    program is not running
    #  8-99    reserved for future LSB use
    #  100-149    reserved for distribution use
    #  150-199    reserved for application use
    #  200-254    reserved
    if [ ! -z "$PIDFILE" ]; then
      if [ -f "$PIDFILE" ]; then
        log_warning_msg "PID file ($PIDFILE) found. Is the server still running? Start aborted."
        exit 0
      fi
    fi
    if [ "$VERBOSE" != no ]; then
      log_success_msg "Using BASEDIR:   $BASEDIR"
      log_success_msg "Using PORT:      $PORT $SSLPORT"
      log_success_msg "Using ORIGIN:    $ORIGIN"
      log_success_msg "Using JAVA_HOME: $JAVA_HOME"
      log_success_msg "Using JDK_HOME:  $JDK_HOME"
    fi
    [ "$VERBOSE" != no ] && log_success_msg "Starting $NAME"
    shift
    do_start "$@"
    exit $?
    ;;
  stop)
    # Return
    #  0    if the action was successful
    #  0    service is already stopped or not running
    #  1    generic or unspecified error (current practice)
    #  2    invalid or excess argument(s)
    #  3    unimplemented feature (for example, "reload")
    #  4    user had insufficient privilege
    #  5    program is not installed
    #  6    program is not configured
    #  7    program is not running
    #  8-99    reserved for future LSB use
    #  100-149    reserved for distribution use
    #  150-199    reserved for application use
    #  200-254    reserved
    if [ -f "$PIDFILE" -a -r "$PIDFILE" ]; then
      kill -0 `cat "$PIDFILE"` >/dev/null 2>&1
      if [ $? -gt 0 ]; then
        rm -f "$PIDFILE"
        if [ $? -gt 0 ]; then
          log_failure_msg "PID file ($PIDFILE) found but no matching process was found. Stop aborted."
          exit 4
        fi
        exit 0
      fi
    elif [ -f "$PIDFILE" ]; then
      log_failure_msg "The PID ($PIDFILE) exist, but it cannot be read. Stop aborted."
      exit 4
    else
      log_warning_msg "The PID ($PIDFILE) does not exist. Is the server running? Stop aborted."
      exit 0
    fi
    [ "$VERBOSE" != no ] && log_success_msg "Stopping $NAME"
    shift
    do_stop "$@"
    exit $?
    ;;
  status)
    # Return
    #  0    program is running or service is OK
    #  1    program is dead and pid file exists
    #  2    program is dead and lock file exists
    #  3    program is not running
    #  4    program or service status is unknown
    #  5-99    reserved for future LSB use
    #  100-149    reserved for distribution use
    #  150-199    reserved for application use
    #  200-254    reserved
    if [ -f "$PIDFILE" -a -r "$PIDFILE" ]; then
      kill -0 `cat "$PIDFILE"` >/dev/null 2>&1
      if [ $? -gt 0 ]; then
        [ "$VERBOSE" != no ] && log_failure_msg "$NAME is dead and pid file exists"
        exit 1
      fi
      [ "$VERBOSE" != no ] && log_success_msg "$NAME is running"
      exit 0
    elif [ -f "$PIDFILE" ]; then
      [ "$VERBOSE" != no ] && log_failure_msg "$NAME pid file exists, but cannot be read"
      exit 4
    else
      [ "$VERBOSE" != no ] && log_warning_msg "$NAME is not running"
      exit 3
    fi
    ;;
  probe)
	## Optional: Probe for the necessity of a reload,
	## give out the argument which is required for a reload.

    if [ -r "$PIDFILE" -a "$CONFIG" -nt "$PIDFILE" ]; then
      echo restart
    fi
    exit 0
	;;
  restart|force-reload)
    if [ -f "$PIDFILE" ]; then
      [ "$VERBOSE" != no ] && log_success_msg "Restarting $NAME"
      do_stop
      if [ $? -gt 0 ]; then
        exit $?
      fi
      sleep 2
    else
      [ "$VERBOSE" != no ] && log_success_msg "Starting $NAME"
    fi
    shift
    do_start "$@"
    exit $?
    ;;
  try-restart)
    if [ -f "$PIDFILE" ]; then
      [ "$VERBOSE" != no ] && log_success_msg "Restarting $NAME"
      do_stop
      if [ $? -gt 0 ]; then
        exit $?
      fi
      sleep 2
      shift
      do_start "$@"
      exit $?
    fi
    [ "$VERBOSE" != no ] && log_warning_msg "$NAME is not running"
    exit 0
    ;;
  reload)
    log_failure_msg "Usage: $SCRIPTNAME {start|stop|status|restart|force-reload|try-restart|probe}"
    exit 3
    ;;
  setup)
    if [ "$VERBOSE" != no ]; then
      log_success_msg "Using BASEDIR:   $BASEDIR"
      log_success_msg "Using PORT:      $PORT $SSLPORT"
      log_success_msg "Using ORIGIN:    $ORIGIN"
      log_success_msg "Using JAVA_HOME: $JAVA_HOME"
      log_success_msg "Using JDK_HOME:  $JDK_HOME"
    fi
    AUTO_START=
    if [ -f "$PIDFILE" -a -r "$PIDFILE" ]; then
      kill -0 `cat "$PIDFILE"` >/dev/null 2>&1
      if [ $? -eq 0 ]; then
        [ "$VERBOSE" != no ] && log_success_msg "Stopping $NAME"
        do_stop
        if [ $? -eq 0 ]; then
          AUTO_START=true
        fi
      fi
    fi
    [ "$VERBOSE" != no ] && log_success_msg "Setting up $NAME"
    if [ "$USERNAME" = "root" -a -n "$SUDO_USER" ]; then
      USERNAME="$SUDO_USER"
    fi
    shift
    do_setup -u "$USERNAME" "$@"
    if [ $? -eq 0 -a -n "$AUTO_START" ]; then
      [ "$VERBOSE" != no ] && log_success_msg "Starting $NAME"
      do_start
    fi
    exit $?
    ;;
  dump)
    if [ -f "$PIDFILE" -a -r "$PIDFILE" ]; then
      kill -0 `cat "$PIDFILE"` >/dev/null 2>&1
      if [ $? -gt 0 ]; then
        log_failure_msg "PID file ($PIDFILE) found but no matching process was found. Dump aborted."
        exit 7
      fi
    elif [ -f "$PIDFILE" ]; then
      log_failure_msg "The PID ($PIDFILE) exist, but it cannot be read. Dump aborted."
      exit 4
    else
      log_failure_msg "The PID ($PIDFILE) does not exist. Is the server running? Dump aborted."
      exit 7
    fi
    [ "$VERBOSE" != no ] && log_success_msg "Dumping $NAME internal state"
    shift
    do_dump "$@"
    exit $?
    ;;
  reset)
    if [ -f "$PIDFILE" -a -r "$PIDFILE" ]; then
      kill -0 `cat "$PIDFILE"` >/dev/null 2>&1
      if [ $? -gt 0 ]; then
        log_failure_msg "PID file ($PIDFILE) found but no matching process was found. Dump aborted."
        exit 7
      fi
    elif [ -f "$PIDFILE" ]; then
      log_failure_msg "The PID ($PIDFILE) exist, but it cannot be read. Dump aborted."
      exit 4
    else
      log_failure_msg "The PID ($PIDFILE) does not exist. Is the server running? Dump aborted."
      exit 7
    fi
    [ "$VERBOSE" != no ] && log_success_msg "Resetting $NAME internal cache"
    shift
    do_reset "$@"
    exit $?
    ;;
  *)
    if [ -f "$PIDFILE" ]; then
      log_failure_msg "PID file ($PIDFILE) found. Is the server still running? Run aborted."
      exit 152
    fi
    if [ "$VERBOSE" != no ]; then
      log_success_msg "Using BASEDIR:   $BASEDIR"
      log_success_msg "Using PORT:      $PORT $SSLPORT"
      log_success_msg "Using ORIGIN:    $ORIGIN"
      log_success_msg "Using JAVA_HOME: $JAVA_HOME"
      log_success_msg "Using JDK_HOME:  $JDK_HOME"
    fi
    [ "$VERBOSE" != no ] && log_success_msg "Running $NAME"
    do_run "$@"
    exec $?
    ;;
esac

