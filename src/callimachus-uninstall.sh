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

# resolve links - /etc/init.d/callimachus may be a softlink
TARGET="/etc/init.d/$NAME"

while [ -h "$TARGET" ] ; do
  ls=`ls -ld "$TARGET"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    TARGET="$link"
  else
    TARGET=`dirname "$TARGET"`/"$link"
  fi
done

if [ ! "$PRGDIR/$NAME.sh" -ef "$TARGET" -o ! -e "$TARGET" ]; then
  echo "This program is not installed" 2>&1
  exit 0
fi

"/etc/init.d/$NAME" stop  > /dev/null 2>&1
if [ -x /usr/lib/lsb/remove_initd ]; then
  /usr/lib/lsb/install_initd "/etc/init.d/$NAME"
elif [ -x /sbin/chkconfig ]; then
  /sbin/chkconfig --del "$NAME"
elif [ -x /usr/sbin/update-rc.d ]; then
  /usr/sbin/update-rc.d -f "$NAME" remove
else
  rm -f /etc/rc.d/rc?.d/???$NAME
fi

if [ $? -gt 0 ]; then
  exit $?
fi

rm "/etc/init.d/$NAME"

if [ $? -gt 0 ]; then
  exit 4
fi

