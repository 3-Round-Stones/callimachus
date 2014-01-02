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
NAME=`basename "$PRG" | perl -pe 's/-\w+.sh//' 2>/dev/null`

# Check that target executable exists
if [ ! -f "$PRGDIR/$NAME.sh" -o "$PRG" = "$PRGDIR/$NAME.sh" ]; then
  echo "Cannot find $PRGDIR/$NAME.sh" 2>&1
  echo "This file is needed to run this program" 2>&1
  exit 5
fi

if [ ! -d /etc/init.d ]; then
  echo "Cannot find directory /etc/init.d" 2>&1
  echo "This directory is needed to install this program" 2>&1
  exit 5
fi

# Make sure only root can run our script
if [ "$(id -u)" != "0" ]; then
   echo "This script must be run as root" 1>&2
   exit 1
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

if [ ! -e "$CONFIG" ]; then
  cp "$BASEDIR/etc/$NAME-defaults.conf" "$CONFIG"
fi

if [ -r "$CONFIG" ]; then
  . "$CONFIG" 2>/dev/null
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

if [ "$VERBOSE" != no ]; then
  read -p "Callimachus requires Java JDK 7
Where is the JDK installed?
  [$JDK_HOME]:  " jhome
  if [ -n "$jhome" ] ; then
    JDK_HOME="$jhome"
  fi
  read -p "Where is the JRE installed?
  [$JAVA_HOME]:  " jhome
  if [ -n "$jhome" ] ; then
    JAVA_HOME="$jhome"
  fi
fi

if [ ! -e "$JAVA_HOME/bin/java" ] ; then
    echo "$JAVA_HOME/bin/java is not executable" 1>&2
    echo "This file is needed to run this program" 1>&2
    exit 5
fi

if [ ! -e "$JDK_HOME/bin/javac" ] ; then
    echo "$JDK_HOME/bin/javac is not executable" 1>&2
    echo "This file is needed to run this program" 1>&2
    exit 5
fi

if [ ! -r "$JDK_HOME/lib/tools.jar" ] ; then
    echo "$JDK_HOME/lib/tools.jar is not present" 1>&2
    echo "Check that you have the correct value for JDK_HOME in $CONFIG" 1>&2
    echo "This file is needed to run this program" 1>&2
    exit 5
fi

if [ ! -e "$JDK_HOME/bin/jrunscript" ] ; then
    echo "$JDK_HOME/bin/jrunscript is not executable" 1>&2
    echo "This file is needed to run this program" 1>&2
    exit 5
fi

if [ "$("$JDK_HOME/bin/jrunscript" -e 'if(Array.isArray)println(true)')" != "true" ] ; then
    echo "$JDK_HOME does not include ECMAScript 5 support" 1>&2
    echo "A newer JDK version (with at least ECMAScript 5 support) is required" 1>&2
    exit 5
fi

# Update JAVA_HOME in $CONFIG
sed "s:#\?\s*JAVA_HOME=.*:JAVA_HOME=\"$JAVA_HOME\":;s:#\?\s*JDK_HOME=.*:JDK_HOME=\"$JDK_HOME\":" "$CONFIG" > "$CONFIG.tmp"
if [ $(cat "$CONFIG.tmp" |wc -l) = $(cat "$CONFIG" |wc -l) ] ; then
  cat "$CONFIG.tmp" > "$CONFIG"
  rm "$CONFIG.tmp"
fi

if [ -z "$DAEMON_USER" ] ; then
  DAEMON_USER=callimachus
  sed "s:#\?\s*DAEMON_USER=.*:DAEMON_USER=$DAEMON_USER:" "$CONFIG" > "$CONFIG.tmp"
  if [ $(cat "$CONFIG.tmp" |wc -l) = $(cat "$CONFIG" |wc -l) ] ; then
    cat "$CONFIG.tmp" > "$CONFIG"
    rm "$CONFIG.tmp"
  fi
  if ! grep -qe '^DAEMON_USER=callimachus$' "$CONFIG" ; then
    echo >> "$CONFIG"
    echo "DAEMON_USER=callimachus" >> "$CONFIG"
  fi
fi

if [ -z "$DAEMON_GROUP" ] ; then
  DAEMON_GROUP=callimachus
  sed "s:#\?\s*DAEMON_GROUP=.*:DAEMON_GROUP=$DAEMON_GROUP:" "$CONFIG" > "$CONFIG.tmp"
  if [ $(cat "$CONFIG.tmp" |wc -l) = $(cat "$CONFIG" |wc -l) ] ; then
    cat "$CONFIG.tmp" > "$CONFIG"
    rm "$CONFIG.tmp"
  fi
  if ! grep -qe '^DAEMON_GROUP=callimachus$' "$CONFIG" ; then
    echo >> "$CONFIG"
    echo "DAEMON_GROUP=callimachus" >> "$CONFIG"
  fi
fi

# install daemon user/group
if ! grep -q "$DAEMON_GROUP" /etc/group ; then
    groupadd -r "$DAEMON_GROUP"
fi
if ! id "$DAEMON_USER" >/dev/null 2>&1 ; then
  useradd -d "$BASEDIR" -g "$DAEMON_GROUP" -r "$DAEMON_USER"
fi

# SSL setup
if [ -z "$SSL" ] ; then
  SSL="$BASEDIR/etc/ssl.properties"
fi
if [ -z "$KEYTOOL" ] ; then
  KEYTOOL="$JAVA_HOME/bin/keytool"
fi
if [ -r "$SSL" ] ; then
  KEYTOOL_OPTS=$(perl -pe 's/\s*\#.*$//g' "$SSL" 2>/dev/null |perl -pe 's/(\S+)=(.*)/-J-D$1=$2/' 2>/dev/null |tr -s '\n' ' ')
fi
if [ ! -e "$SSL" -a "$VERBOSE" != no ] || ( [ -r "$SSL" -a "$VERBOSE" != no ] && ! grep -q "keyStore" "$SSL" ) ; then
  read -p "Would you like to generate a server certificate now? (type 'yes' or 'no')
  [no]:  " genkey
elif [ -r "$SSL" -a "$VERBOSE" != no ]; then
  grep -E '^javax.net.ssl.keyStorePassword=' "$SSL" |perl -pe 's/^javax.net.ssl.keyStorePassword=(.*)/$1/' 2>/dev/null > "$SSL.password"
  KEYSTORE=$(grep -E '^javax.net.ssl.keyStore=' $SSL |perl -pe 's/^javax.net.ssl.keyStore=(.*)/$1/' 2>/dev/null)
  cname=$("$KEYTOOL" -list -v -keystore "$KEYSTORE" -storepass "$(cat "$SSL.password")" $KEYTOOL_OPTS |grep -B 2 PrivateKeyEntry |grep 'Alias' |head -n 1 |awk '{print $3}')
  until=$("$KEYTOOL" -list -v -keystore "$KEYSTORE" -storepass "$(cat "$SSL.password")" $KEYTOOL_OPTS |grep -A 8 -x "Alias name: $cname" |grep "until:" |tail -n 1 |sed 's/.*until: //')
  expires=$(expr $(date --date="$until" +%s) '-' 60 '*' 60 '*' 24 '*' 31)
  if [ $(date +%s) -ge "$expires" ] ; then
    read -p "The certificate $cname will expire on $until.
Would you like to generate a new server certificate now? (type 'yes' or 'no')
  [no]:  " genkey
    if [ "$genkey" = "yes" ] ; then
      "$KEYTOOL" -delete -alias "$cname" -keystore "$KEYSTORE" -storepass "$(cat "$SSL.password")" $KEYTOOL_OPTS
      rm -f "$cname.cer" "$cname.csr"
    fi
  fi
fi
if [ "$genkey" = "yes" -a "$VERBOSE" != no ] ; then
  if [ -z "$cname" ] ; then
    cname=$(hostname -f |tr '[A-Z]' '[a-z]')
  fi
  orgunit="Unknown"
  orgname="Unknown"
  city="Unknown"
  state="Unknown"
  country="Unknown"
  cont="no"
  while [ "$cont" != "yes" ] ; do
    read -p "What is the secure domain or server name?
  [$cname]:  " pcname
    read -p "What is the name of your organizational unit?
  [$orgunit]:  " porgunit
    read -p "What is the name of your organization?
  [$orgname]:  " porgname
    read -p "What is the name of your City or Locality?
  [$city]:  " pcity
    read -p "What is the name of your State or Province?
  [$state]:  " pstate
    read -p "What is the two-letter country code for this unit?
  [$country]:  " pcountry
    cname="${pcname:-$cname}"
    orgunit="${porgunit:-$orgunit}"
    orgname="${porgname:-$orgname}"
    city="${pcity:-$city}"
    state="${pstate:-$state}"
    country="${pcountry:-$country}"
    dname="CN=$(echo "$cname" |sed 's/,/\\,/g'), OU=$(echo "$orgunit" |sed 's/,/\\,/g'), O=$(echo "$orgname" |sed 's/,/\\,/g'), L=$(echo "$city" |sed 's/,/\\,/g'), ST=$(echo "$state" |sed 's/,/\\,/g'), C=$(echo "$country" |sed 's/,/\\,/g')"
    read -p "Is $dname correct? (type 'yes' or 'no')
  [no]:  " cont
  done
  if [ -z "$KEYSTORE" ] ; then
    KEYSTORE=".keystore"
    echo "javax.net.ssl.keyStore=$KEYSTORE" >> "$SSL"
  fi
  if [ ! -e "$SSL.password" ] ; then
    echo $$$(date +%s)$RANDOM | md5sum | awk '{print $1}' > "$SSL.password"
    echo "javax.net.ssl.keyStorePassword=$(cat "$SSL.password")" >> "$SSL"
  fi
  "$KEYTOOL" -genkey -alias "$cname" -keyalg "RSA" -keysize "2048" -dname "$dname" -keypass "$(cat "$SSL.password")" -validity "192" -keystore "$KEYSTORE" -storepass "$(cat "$SSL.password")" $KEYTOOL_OPTS
  "$KEYTOOL" -export -alias "$cname" -keystore "$KEYSTORE" -storepass "$(cat "$SSL.password")" -rfc -file "etc/$cname.cer" $KEYTOOL_OPTS
  if [ -e "etc/$cname.cer" ] ; then
    echo "Distribute etc/$cname.cer to all remote management agents"
  fi
  "$KEYTOOL" -certreq -alias "$cname" -keypass "$(cat "$SSL.password")" -keystore "$KEYSTORE" -storepass "$(cat "$SSL.password")" -file "etc/$cname.csr" $KEYTOOL_OPTS
  chmod go-rwx "$SSL"
  chmod go-rwx ".keystore"
  rm "$SSL.password"
fi

# install init.d files
chmod 755 "$PRGDIR/$NAME.sh"

ln -sf "$PRGDIR/$NAME.sh" "/etc/init.d/$NAME"

if [ $? -gt 0 ]; then
  exit 4
fi

if [ -x /usr/lib/lsb/install_initd ]; then
  /usr/lib/lsb/install_initd "/etc/init.d/$NAME" 1>&2
elif [ -x /sbin/chkconfig ]; then
  /sbin/chkconfig --add "$NAME" 1>&2
elif [ -x /usr/sbin/update-rc.d ]; then
  /usr/sbin/update-rc.d "$NAME" defaults 90 10 1>&2
else
   for i in 2 3 4 5; do
        ln -sf "/etc/init.d/$NAME" "/etc/rc.d/rc${i}.d/S90$NAME"
   done
   for i in 1 6; do
        ln -sf "/etc/init.d/$NAME" "/etc/rc.d/rc${i}.d/K10$NAME"
   done
fi

