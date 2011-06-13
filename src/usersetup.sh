#!/bin/bash

#####################################################
# usersetup.sh
# David Wood (david@3roundstones.com)
# 24 May 2011
#
# Bash script to provide initial account details
# for a Callimachus instance from the command line.
# This script may be used in place of the Callimachus
# User Setup page on remote machines since it
# requires users to be on the same machine.
#####################################################

#### Functions
function assertIsSet() {
    [[ ! ${!1} && ${!1-_} ]] && {
        echo
        echo "ERROR: $1 is not set, aborting." >&2
        usage
        exit 1
    }
}

function assertIsPresent() {
    [[ ! ${!1} && ${!1-_} ]] && {
        echo
        echo "ERROR: the $1 utility was not found in your path, aborting." >&2
        usage
        exit 1
    }
}

function urlencode() {
    varname=$1
    shift
    
    # Concatenate all remaining input into a single variable to allow for spaces.
    input=$1
    shift
    while [ "$1" != "" ]; do
        input=${input}\ $1
        shift
    done
    eval ${varname}=$(echo "${input}" | sed -e 's/%/%25/g' -e 's/ /%20/g' -e 's/!/%21/g' -e 's/"/%22/g' -e 's/#/%23/g' -e 's/\$/%24/g' -e 's/\&/%26/g' -e 's/'\''/%27/g' -e 's/(/%28/g' -e 's/)/%29/g' -e 's/\*/%2a/g' -e 's/+/%2b/g' -e 's/,/%2c/g' -e 's/-/%2d/g' -e 's/\./%2e/g' -e 's/\//%2f/g' -e 's/:/%3a/g' -e 's/;/%3b/g' -e 's//%3e/g' -e 's/?/%3f/g' -e 's/@/%40/g' -e 's/\[/%5b/g' -e 's/\\/%5c/g' -e 's/\]/%5d/g' -e 's/\^/%5e/g' -e 's/_/%5f/g' -e 's/`/%60/g' -e 's/{/%7b/g' -e 's/|/%7c/g' -e 's/}/%7d/g' -e 's/~/%7e/g')
}

function usage
{
  echo
  echo "usage: sh usersetup.sh [-h hostname -u userid -p password -f fullname] | [-?]]"
  echo "e.g."
  echo "  sh usersetup.sh -h localhost -u admin -p \"admin's password\" -n \"Admin User\""
  echo "  sh usersetup.sh -?"
  echo
  echo "Needed parameters:"
  echo "  -h | --hostname    Host name used in [installation dir]/etc/callimachus.conf"
  echo "                     Include a port number here as needed (e.g. localhost:8080)"
  echo "  -u | --userid      User id to be assigned to the initial user (often 'admin')"
  echo "  -p | --password    Password to be assigned to the initial user"
  echo "  -f | --fullname    The full name of the initial user"
  echo "  -? | --help        Get this help information"
  echo
}

#### Main
if [ ! $# -gt 0 ]; then
  echo "ERROR: Needed parameters were not provided."
  usage
  exit 1
fi

while [ "$1" != "" ]; do
    case $1 in
        -h | --hostname )       shift
                                hostname=$1
                                ;;
        -u | --userid )         shift
                                userid=$1
                                ;;
        -p | --password )       shift
                                password=$1
                                ;;
        -f | --fullname )       shift
                                fullname=$1
                                ;;
        -? | --help )           usage
                                exit
                                ;;
        * )                     usage
                                exit 1
    esac
    shift
done

assertIsSet hostname;
assertIsSet userid;
assertIsSet password;
assertIsSet fullname;

# Check dependencies
curlutil=`which curl`
md5sumutil=`which md5sum`
cututil=`which cut`

assertIsPresent curlutil
assertIsPresent md5sumutil
assertIsPresent cututil

# Encode the password.
realm=http://${hostname}/;
passwordtohash=${userid}':'${realm}':'${password}
encodedpassword=`echo -n ${passwordtohash} | ${md5sumutil} | ${cututil} -f1 -d" "`
##echo "Password hash: "${passwordtohash}

# URL encode the vars that may need it.
urlencode userid ${userid}
urlencode fullname ${fullname}

# Finally, do what we came here to do.
cmd="${curlutil} -f --data name=${userid} --data label=${fullname} --data algorithm=MD5 --data encoded=${encodedpassword} http://${hostname}/group/admin?usersetup"

set -x
${cmd} >> usersetup.log
set +x

if [ ! $? ]; then
    echo "ERROR: ${curlutil} exited abnormally.  See usersetup.log to see if any details were captured."
    echo "       exit code: $?"
    echo
fi

## TODO: Instead of capturing an HTML return in usersetup.log, interogate the URL and review HTTP status code for success or failure.
