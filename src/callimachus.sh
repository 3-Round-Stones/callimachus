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

PIDFILE="$BASEDIR/run/$NAME.pid"
SCRIPTNAME=/etc/init.d/$NAME

MAINCLASS=org.callimachusproject.Server
MONITORCLASS=org.callimachusproject.ServerMonitor

# Ensure that any user defined CLASSPATH variables are not used on startup.
CLASSPATH=

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
  DAEMON=`command -v jsvc`
fi
if [ ! -f "$DAEMON" ]; then
  DAEMON=`command -v jsvc`
fi

# Check that target executable exists
if [ ! -f "$DAEMON" ]; then
  log_failure_msg "Cannot find jsvc daemon"
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

if [ -z "$KEYTOOL" ] ; then
  KEYTOOL="$JAVA_HOME/bin/keytool"
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

if [ -z "$JMXRMI" ] ; then
  JMXRMI="$BASEDIR/etc/jmxremote.properties"
fi

if [ -z "$JMXRMI_OPTS" -a -e "$JMXRMI" ] ; then
  JMXRMI_OPTS=$(perl -pe 's/\s*\#.*$//g' "$JMXRMI" 2>/dev/null |perl -pe 's/(\S+)=(.*)/-D$1=$2/' 2>/dev/null |tr -s '\n' ' ')
fi

if [ -z "$REPOSITORY_CONFIG" ] ; then
  REPOSITORY_CONFIG="$BASEDIR/etc/$NAME-repository.ttl"
fi

CLASSPATH="$BASEDIR/classes/"
for JAR in "$BASEDIR"/lib/*.jar ; do
  CLASSPATH="$CLASSPATH:$JAR"
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

if [ -z "$OPTS" ] ; then
  if [ "$SECURITY_MANAGER" = "false" ]; then
    OPTS="--trust"
  fi
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

# Make sure only root can run our script
if [ -n "$DAEMON_USER" -a "$USER" != "$DAEMON_USER" -a "$(id -u)" != "0" ]; then
 echo "This script must be run as root" 1>&2
 exit 4
elif [ "$(id -u)" = "0" -a -x "$DAEMON" -a -x "$(command -v getcap)" -a -x "$(command -v setcap)" ] && ! getcap "$DAEMON" | grep -q "cap_net_bind_service" ; then
  setcap cap_net_bind_service=ep "$DAEMON"
fi

# setup trust store
if [ -r "$JAVA_HOME/lib/security/cacerts" ] && ( [ ! -e "$SSL" ] || ( [ -r "$SSL" ] && ! grep -q javax.net.ssl.trustStore "$SSL" ) ) ; then
  if [ -z "$KEYTOOL" ] ; then
    KEYTOOL="$JAVA_HOME/bin/keytool"
  fi
  if [ -x "$(command -v md5sum)" ] ; then
    echo 1$$$(date +%s)$RANDOM | md5sum | awk '{print $1}' > "$SSL.password"
  else
    echo $$$(date +%s)$RANDOM | awk '{print $1}' > "$SSL.password"
  fi
  cp "$JAVA_HOME/lib/security/cacerts" "$BASEDIR/etc/truststore"
  "$KEYTOOL" -storepasswd -new "$(cat "$SSL.password")" -keystore "$BASEDIR/etc/truststore" -storepass "changeit"
  echo "javax.net.ssl.trustStore=etc/truststore" >> "$SSL"
  echo "javax.net.ssl.trustStorePassword=$(cat "$SSL.password")" >> "$SSL"
  chmod go-rwx "$SSL"
  rm "$SSL.password"
fi

if [ -z "$SSL_OPTS" -a -e "$SSL" ] ; then
  SSL_OPTS=$(perl -pe 's/\s*\#.*$//g' "$SSL" 2>/dev/null |perl -pe 's/(\S+)=(.*)/-D$1=$2/' 2>/dev/null |tr -s '\n' ' ')
fi

# install jmxremote password
if [ -z "$JMXRMI" ] ; then
  JMXRMI="$BASEDIR/etc/jmxremote.properties"
fi
JMXRIMACCESS=$(grep -E '^com.sun.management.jmxremote.access.file=' "$JMXRMI" |perl -pe 's/^com.sun.management.jmxremote.access.file=(.*)/$1/' 2>/dev/null)
if [ ! -e "$JMXRIMACCESS" -a -r "$JMXRMI" ] ; then
  if [ -r "$JAVA_HOME/lib/management/jmxremote.access" ] ; then
    cp "$JAVA_HOME/lib/management/jmxremote.access" "$JMXRIMACCESS"
  fi
  if ! grep "^monitorRole" "$JMXRIMACCESS" | grep -q "read" ; then
    echo >> "$JMXRIMACCESS"
    echo "monitorRole   readonly" >> "$JMXRIMACCESS"
  fi
  if ! grep "^controlRole" "$JMXRIMACCESS" | grep -q "read" ; then
    echo >> "$JMXRIMACCESS"
    echo "controlRole   readwrite" >> "$JMXRIMACCESS"
  fi
fi
JMXRIMPASS=$(grep -E '^com.sun.management.jmxremote.password.file=' "$JMXRMI" |perl -pe 's/^com.sun.management.jmxremote.password.file=(.*)/$1/' 2>/dev/null)
if [ ! -e "$JMXRIMPASS" -a -r "$JMXRMI" ] ; then
  if [ -r "$JAVA_HOME/lib/management/jmxremote.password" ] ; then
    cp "$JAVA_HOME/lib/management/jmxremote.password" "$JMXRIMPASS"
  elif [ -r "$JAVA_HOME/lib/management/jmxremote.password.template" ] ; then
    cp "$JAVA_HOME/lib/management/jmxremote.password.template" "$JMXRIMPASS"
  fi
  echo >> "$JMXRIMPASS"
  if [ -x "$(command -v md5sum)" ] ; then
    echo 2$$$(date +%s)$RANDOM | md5sum | awk '{print "monitorRole " $1}' >> "$JMXRIMPASS"
    echo 3$$$(date +%s)$RANDOM | md5sum | awk '{print "controlRole " $1}' >> "$JMXRIMPASS"
  else
    echo $$$(date +%s)$RANDOM | awk '{print "monitorRole " $1}' >> "$JMXRIMPASS"
    echo $$$(date +%s)$RANDOM | awk '{print "controlRole " $1}' >> "$JMXRIMPASS"
  fi
  chmod 600 "$JMXRIMPASS"
fi

if [ ! -z "$DAEMON_USER" ] ; then
  if [ ! -e "$MAIL" ]; then
    touch "$MAIL"
  fi
  if [ ! -e "$BASEDIR/repositories" ]; then
    mkdir "$BASEDIR/repositories"
  fi
  if [ ! -e "$BASEDIR/backups" ]; then
    mkdir "$BASEDIR/backups"
  fi
  chown "$DAEMON_USER" "$BASEDIR"
  chown -R "$DAEMON_USER" "$BASEDIR/log"
  chown -R "$DAEMON_USER" "$BASEDIR/repositories"
  chown -R "$DAEMON_USER" "$BASEDIR/backups"
  chown "$DAEMON_USER" "$MAIL"
  chown "$DAEMON_USER" "$CONFIG"
  if [ ! -z "$DAEMON_GROUP" ] ; then
    chown ":$DAEMON_GROUP" "$BASEDIR"
    chown -R ":$DAEMON_GROUP" "$BASEDIR/log"
    chown -R ":$DAEMON_GROUP" "$BASEDIR/repositories"
    chown -R ":$DAEMON_GROUP" "$BASEDIR/backups"
    chown ":$DAEMON_GROUP" "$MAIL"
    chown ":$DAEMON_GROUP" "$CONFIG"
  fi
  if [ -r "$SSL" ]; then
    KEYSTORE=$(grep -E '^javax.net.ssl.keyStore=' $SSL |perl -pe 's/^javax.net.ssl.keyStore=(.*)/$1/' 2>/dev/null)
    if [ -n "$KEYSTORE" -a -e "$KEYSTORE" ]; then
      chown -R "$DAEMON_USER" "$KEYSTORE"
      if [ ! -z "$DAEMON_GROUP" ] ; then
        chown -R ":$DAEMON_GROUP" "$KEYSTORE"
      fi
    fi
    TRUSTSTORE=$(grep -E '^javax.net.ssl.trustStore=' $SSL |perl -pe 's/^javax.net.ssl.trustStore=(.*)/$1/' 2>/dev/null)
    if [ -n "$TRUSTSTORE" -a -e "$TRUSTSTORE" ]; then
      chown -R "$DAEMON_USER" "$TRUSTSTORE"
      if [ ! -z "$DAEMON_GROUP" ] ; then
        chown -R ":$DAEMON_GROUP" "$TRUSTSTORE"
      fi
    fi
  fi
  if [ -r "$JMXRMI" ]; then
    JMXRMIPASS=$(grep -E '^com.sun.management.jmxremote.password.file=' $JMXRMI |perl -pe 's/^com.sun.management.jmxremote.password.file=(.*)/$1/' 2>/dev/null)
    if [ -n "$JMXRMIPASS" -a -e "$JMXRMIPASS" ]; then
      chown -R "$DAEMON_USER" "$JMXRMIPASS"
      if [ ! -z "$DAEMON_GROUP" ] ; then
        chown -R ":$DAEMON_GROUP" "$JMXRMIPASS"
      fi
    fi
  fi
  if [ ! -e "$TMPDIR" ] ; then
    mkdir "$TMPDIR"
    chown "$DAEMON_USER" "$TMPDIR"
    if [ ! -z "$DAEMON_GROUP" ] ; then
      chown ":$DAEMON_GROUP" "$TMPDIR"
    fi
  elif [ "$BASEDIR/tmp" = "$TMPDIR" ] ; then
    chown "$DAEMON_USER" "$TMPDIR"
    if [ ! -z "$DAEMON_GROUP" ] ; then
      chown ":$DAEMON_GROUP" "$TMPDIR"
    fi
  fi
fi

#
# Function that starts the daemon/service
#
do_start()
{
  LSOF="$(which lsof 2>/dev/null)"
  LSOF_OPTS="$(echo $PORT |perl -pe 's/(^|\s)(\S)/ -i :$2/g' 2>/dev/null) $(echo $SSLPORT |perl -pe 's/(^|\s)(\S)/ -i :$2/g' 2>/dev/null)"
  if [ -n "$LSOF" ] && [ -n "$PORT" -o -n "$SSLPORT" ] && "$LSOF" $LSOF_OPTS |grep "LISTEN" ; then
    log_failure_msg "Cannot bind to port $PORT $SSLPORT please ensure nothing is already listening on this port"
    return 150
  fi
  JMXPORT="$(grep -E '^com.sun.management.jmxremote.port=' "$JMXRMI" |perl -pe 's/^com.sun.management.jmxremote.port=(.*)/$1/' 2>/dev/null)"
  if [ -n "$JMXPORT" ] ; then
    LSOF_OPTS="$(echo $JMXPORT |perl -pe 's/(^|\s)(\S)/ -i :$2/g' 2>/dev/null)"
    if [ -n "$LSOF" ] && "$LSOF" $LSOF_OPTS |grep "LISTEN" ; then
      log_failure_msg "Cannot bind to port $JMXPORT please ensure nothing is already listening on this port"
      return 150
    fi
  fi

  # import any new certificates before starting server
  if [ -r "$SSL" ] && grep -q "trustStore" "$SSL" ; then
    KEYTOOL_OPTS=$(perl -pe 's/\s*\#.*$//g' "$SSL" 2>/dev/null |perl -pe 's/(\S+)=(.*)/-J-D$1=$2/' 2>/dev/null |tr -s '\n' ' ')
    grep -E '^javax.net.ssl.trustStorePassword=' "$SSL" |perl -pe 's/^javax.net.ssl.trustStorePassword=(.*)/$1/' 2>/dev/null > "$SSL.password"
    TRUSTSTORE=$(grep -E '^javax.net.ssl.trustStore=' $SSL |perl -pe 's/^javax.net.ssl.trustStore=(.*)/$1/' 2>/dev/null)
    for cert in etc/*.pem etc/*.cer etc/*.crt etc/*.cert etc/*.der ; do
      if [ -r "$cert" -a -r "$TRUSTSTORE" ] ; then
        ALIAS="$(basename "$cert" | sed 's/\.[a-z]\+$//' )"
        if ! "$KEYTOOL" -list -keystore "$TRUSTSTORE" -storepass "$(cat "$SSL.password")" |grep -q "^$ALIAS," ; then
          "$KEYTOOL" -import -alias "$ALIAS" -file "$cert" -noprompt -trustcacerts -keystore "$TRUSTSTORE" -storepass "$(cat "$SSL.password")" $KEYTOOL_OPTS
          if [ $? = 0 ] ; then
            log_success_msg "Imported new trusted certificate $cert into $TRUSTSTORE"
          else
            log_warning_msg "Could not import new trusted certificate $cert into $TRUSTSTORE"
          fi
        elif [ "$cert" -nt "$TRUSTSTORE" ] ; then
          "$KEYTOOL" -delete -alias "$ALIAS" -keystore "$TRUSTSTORE" -storepass "$(cat "$SSL.password")" $KEYTOOL_OPTS
          "$KEYTOOL" -import -alias "$ALIAS" -file "$cert" -noprompt -trustcacerts -keystore "$TRUSTSTORE" -storepass "$(cat "$SSL.password")" $KEYTOOL_OPTS
          if [ $? = 0 ] ; then
            log_success_msg "Imported updated trusted certificate $cert into $TRUSTSTORE"
          else
            log_warning_msg "Could not updated certificate $cert into $TRUSTSTORE"
          fi
        fi
      fi
    done
    rm "$SSL.password"
  fi
  if [ -r "$SSL" ] && grep -q "keyStore" "$SSL" ; then
    KEYTOOL_OPTS=$(perl -pe 's/\s*\#.*$//g' "$SSL" 2>/dev/null |perl -pe 's/(\S+)=(.*)/-J-D$1=$2/' 2>/dev/null |tr -s '\n' ' ')
    grep -E '^javax.net.ssl.keyStorePassword=' "$SSL" |perl -pe 's/^javax.net.ssl.keyStorePassword=(.*)/$1/' 2>/dev/null > "$SSL.password"
    KEYSTORE=$(grep -E '^javax.net.ssl.keyStore=' $SSL |perl -pe 's/^javax.net.ssl.keyStore=(.*)/$1/' 2>/dev/null)
    for cert in etc/*.pem etc/*.cer etc/*.crt etc/*.cert etc/*.der ; do
      if [ -r "$cert" -a -r "$KEYSTORE" ] ; then
        ALIAS="$(basename "$cert" | sed 's/\.[a-z]\+$//' )"
        if ! "$KEYTOOL" -list -keystore "$KEYSTORE" -storepass "$(cat "$SSL.password")" |grep -q "^$ALIAS," ; then
          "$KEYTOOL" -import -alias "$ALIAS" -file "$cert" -noprompt -trustcacerts -keystore "$KEYSTORE" -storepass "$(cat "$SSL.password")" $KEYTOOL_OPTS
          if [ $? = 0 ] ; then
            log_success_msg "Imported $cert into $KEYSTORE"
          else
            log_warning_msg "Could not import $cert into $KEYSTORE"
          fi
        elif "$KEYTOOL" -list -keystore "$KEYSTORE" -storepass "$(cat "$SSL.password")" |grep "^$ALIAS," |grep -q "PrivateKeyEntry," ; then
          if [ "$cert" != "etc/$ALIAS.cer" ] ; then
            "$KEYTOOL" -import -alias "$ALIAS" -file "$cert" -noprompt -trustcacerts -keystore "$KEYSTORE" -storepass "$(cat "$SSL.password")" $KEYTOOL_OPTS
            if [ $? = 0 ] ; then
              log_success_msg "Imported certificate reply $cert into $KEYSTORE"
            else
              log_warning_msg "Could not import certificate reply $cert into $KEYSTORE"
            fi
          fi
        elif [ "$cert" -nt "$KEYSTORE" ] ; then
          "$KEYTOOL" -delete -alias "$ALIAS" -keystore "$KEYSTORE" -storepass "$(cat "$SSL.password")" $KEYTOOL_OPTS
          "$KEYTOOL" -import -alias "$ALIAS" -file "$cert" -noprompt -trustcacerts -keystore "$KEYSTORE" -storepass "$(cat "$SSL.password")" $KEYTOOL_OPTS
          if [ $? = 0 ] ; then
            log_success_msg "Imported $cert into $KEYSTORE"
          else
            log_warning_msg "Could not import $cert into $KEYSTORE"
          fi
        fi
      fi
    done
    rm "$SSL.password"
  fi

  JSVC_LOG="$BASEDIR/log/$NAME-stdout.log"
  if [ -e "$JSVC_LOG" ]; then
    test -e "$JSVC_LOG.8" && mv "$JSVC_LOG.8" "$JSVC_LOG.9"
    test -e "$JSVC_LOG.7" && mv "$JSVC_LOG.7" "$JSVC_LOG.8"
    test -e "$JSVC_LOG.6" && mv "$JSVC_LOG.6" "$JSVC_LOG.7"
    test -e "$JSVC_LOG.5" && mv "$JSVC_LOG.5" "$JSVC_LOG.6"
    test -e "$JSVC_LOG.4" && mv "$JSVC_LOG.4" "$JSVC_LOG.5"
    test -e "$JSVC_LOG.3" && mv "$JSVC_LOG.3" "$JSVC_LOG.4"
    test -e "$JSVC_LOG.2" && mv "$JSVC_LOG.2" "$JSVC_LOG.3"
    test -e "$JSVC_LOG.1" && mv "$JSVC_LOG.1" "$JSVC_LOG.2"
    test -e "$JSVC_LOG.0" && mv "$JSVC_LOG.0" "$JSVC_LOG.1"
    test -e "$JSVC_LOG" && mv "$JSVC_LOG" "$JSVC_LOG.0"
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
    -Dorg.callimachusproject.config.repository="$REPOSITORY_CONFIG" \
    -Dorg.callimachusproject.config.webapp="$(ls $BASEDIR/lib/$NAME-webapp*.car)" \
    -classpath "$CLASSPATH" \
    -user "$DAEMON_USER" \
    -Djava.awt.headless=true \
    -XX:OnOutOfMemoryError="kill %p" \
    $JSVC_OPTS $SSL_OPTS $JMXRMI_OPTS "$MAINCLASS" -q -b "$BASEDIR" -c "$CONFIG" $OPTS "$@"

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

  SLEEP=10
  ID=`cat "$PIDFILE"`
  while [ $SLEEP -ge 0 ] && [ -n "$PORT" -o -n "$SSLPORT" ]; do
    kill -0 $ID >/dev/null 2>&1
    if [ $? -gt 0 ]; then
      log_failure_msg "The server is not running, see log files for details. Start aborted."
      return 7
    fi
    if [ -n "$LSOF" ] && "$LSOF" $LSOF_OPTS |grep -e ":$PORT\b" |grep -qe "\b$ID\b"; then
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
        log_warning_msg "The Web service is not running, check log files for possible errors."
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
    if [ "$SLEEP" = "120" ]; then
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

  JSVC_LOG="$BASEDIR/log/$NAME-dump.log"
  if [ -e "$JSVC_LOG" ]; then
    rm "$JSVC_LOG"
  fi
  "$DAEMON" -nodetach -outfile "$JSVC_LOG" -errfile '&1' -home "$JAVA_HOME" -jvm server -procname "$NAME" \
    -pidfile "$BASEDIR/run/$NAME-dump.pid" \
    -Duser.home="$BASEDIR" \
    -Djava.io.tmpdir="$TMPDIR" \
    -Djava.mail.properties="$MAIL" \
    -Djava.awt.headless=true \
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
  JSVC_LOG="$BASEDIR/log/$NAME-reset.log"
  if [ -e "$JSVC_LOG" ]; then
    rm "$JSVC_LOG"
  fi
  "$DAEMON" -nodetach -outfile "$JSVC_LOG" -errfile '&1' -home "$JAVA_HOME" -jvm server -procname "$NAME" \
    -pidfile "$BASEDIR/run/$NAME-dump.pid" \
    -Duser.home="$BASEDIR" \
    -Djava.io.tmpdir="$TMPDIR" \
    -Djava.mail.properties="$MAIL" \
    -Djava.awt.headless=true \
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
    -Dorg.callimachusproject.config.repository="$REPOSITORY_CONFIG" \
    -Dorg.callimachusproject.config.webapp="$(ls $BASEDIR/lib/$NAME-webapp*.car)" \
    -Djava.awt.headless=true \
    -XX:OnOutOfMemoryError="kill %p" \
    -classpath "$CLASSPATH" \
    -user "$DAEMON_USER" \
    $JSVC_OPTS $SSL_OPTS $JMXRMI_OPTS "$MAINCLASS" -q -b "$BASEDIR" -c "$CONFIG" "$@"
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
      if [ -n "$ORIGIN" ] ; then
        log_success_msg "Using PORT:      $PORT $SSLPORT"
        log_success_msg "Using ORIGIN:    $ORIGIN"
      fi
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

