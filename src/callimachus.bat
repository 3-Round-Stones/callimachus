@echo off
rem 
rem Portions Copyright (c) 2009-10 Zepheira LLC, Some Rights Reserved
rem Portions Copyright (c) 2010-11 Talis Inc, Some Rights Reserved
rem 
rem Licensed under the Apache License, Version 2.0 (the "License");
rem you may not use this file except in compliance with the License.
rem You may obtain a copy of the License at
rem 
rem   http://www.apache.org/licenses/LICENSE-2.0
rem 
rem Unless required by applicable law or agreed to in writing, software
rem distributed under the License is distributed on an "AS IS" BASIS,
rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
rem See the License for the specific language governing permissions and
rem limitations under the License.
rem 

setlocal

set PRG=%0
rem check if path is quoted
if "%PRG:~-4,3%" == "bat" (
  set "PRG=%PRG:~1,-1%"
)
rem check if path is absolute
if not "%PRG:~1,1%" == ":" (
  set "PRG=%CD%\%PRG%"
)
rem determine DIRNAME and BASENAME
set "DIRNAME=%PRG%"
set BASENAME=
:dirname
if "%DIRNAME:~-1%" NEQ "\" (
  set "BASENAME=%DIRNAME:~-1%%BASENAME%"
  set "DIRNAME=%DIRNAME:~0,-1%"
  goto dirname
)
rem strip backslash (\)
set DIRNAME=%DIRNAME:~0,-1%

rem Guess BASEDIR if not defined
if not "%BASEDIR%" == "" goto okHome
set "BASEDIR=%DIRNAME%"
:basedir
if "%BASEDIR:~-1%" NEQ "\" (
  set "BASEDIR=%BASEDIR:~0,-1%"
  goto basedir
)
set BASEDIR=%BASEDIR:~0,-1%
if exist "%BASEDIR%\bin\%BASENAME%" goto okHome
echo The BASEDIR environment variable is not defined correctly
echo This environment variable is needed to run this server
goto end
:okHome

if not "%NAME%" == "" goto gotName
set "NAME=%BASENAME%"
:name
if "%NAME:~-1%" NEQ "." (
  set "NAME=%NAME:~0,-1%"
  goto name
)
rem strip period (.)
set "NAME=%NAME:~0,-1%"
:gotName

rem Ensure that any user defined CLASSPATH variables are not used on startup.
set CLASSPATH=

if not "%CONFIG%" == "" goto gotConfigFile
set "CONFIG=%BASEDIR%/etc/%NAME%.conf"
:gotConfigFile

rem Get standard environment variables
if not exist "%CONFIG%" goto okConfig
setlocal EnableDelayedExpansion
IF NOT EXIST "%BASEDIR%\tmp" MKDIR "%BASEDIR%\tmp"
for /f "tokens=1,* delims==" %%i in ('find /V "#" "%CONFIG%"') do (
  set "line=set %%i=%%~j"
  if not "!line:~4,1!" == "-" echo !line!>> "%BASEDIR%\tmp\%NAME%-conf.bat"
)
call "%BASEDIR%\tmp\%NAME%-conf.bat"
del "%BASEDIR%\tmp\%NAME%-conf.bat"
:okConfig

rem Read relative config paths from BASEDIR
cd "%BASEDIR%"

rem check for a JDK in the BASEDIR
for /d %%i in ("%BASEDIR%\jdk*") do set JAVA_HOME=%%i

rem Lookup the JDK in the registry
if not "%JAVA_HOME%" == "" goto gotJdkHome
set "KeyName=HKEY_LOCAL_MACHINE\SOFTWARE\JavaSoft\Java Development Kit"
set Cmd=reg query "%KeyName%" /s
for /f "tokens=2*" %%i in ('%Cmd% ^| find "JavaHome"') do set JAVA_HOME=%%j

rem Make sure prerequisite environment variable is set
if not "%JAVA_HOME%" == "" goto gotJdkHome
echo The JAVA_HOME environment variable is not defined
echo The JAVA_HOME environment variable is needed to run this server
goto exit
:gotJdkHome

set "JAVA=%JAVA_HOME%\bin\java"
set "JAVAW=%JAVA_HOME%\bin\javaw"

if not "%PID%" == "" goto gotOut
set "PID=%BASEDIR%\run\%NAME%.pid"
:gotOut

if not "%LIB%" == "" goto gotLib
set "LIB=%BASEDIR%\lib"
:gotLib

if not "%TMPDIR%" == "" goto gotTmpdir
set "TMPDIR=%BASEDIR%\tmp"
:gotTmpdir

if not "%JID%" == "" goto gotTmpdir
set "JID=%BASEDIR%\run\callimathus.jmx"
:gotTmpdir

if not "%LOGGING%" == "" goto gotLogging
set "LOGGING=%BASEDIR%\etc\logging.properties"
:gotLogging

if not "%MAIL%" == "" goto gotMail
set "MAIL=%BASEDIR%\etc\mail.properties"
:gotMail

if not "%SSL%" == "" goto gotSSL
set "SSL=%BASEDIR%\etc\ssl.properties"
:gotSSL

rem Get system properties from file
if not exist "%SSL%" goto okSslOpts
setlocal EnableDelayedExpansion
IF NOT EXIST "%BASEDIR%\tmp" MKDIR "%BASEDIR%\tmp"
for /f "tokens=1,* delims==" %%i in ('find /V "#" "%SSL%"') do (
  set "line=-D%%i=%%~j"
  if not "!line:~4,1!" == "-" set "SSL_OPTS=!SSL_OPTS! !line!"
)
:okSslOpts

if not "%REPOSITORY%" == "" goto gotRepository
set "REPOSITORY=repositories/%NAME%"
:gotRepository

if not "%REPOSITORY_CONFIG%" == "" goto gotRepositoryConfig
set "REPOSITORY_CONFIG=etc/%NAME%-repository.ttl"
:gotRepositoryConfig

setlocal ENABLEDELAYEDEXPANSION
for /r "%LIB%" %%a IN (*.jar) do set CLASSPATH=!CLASSPATH!;%%a

if not "%JAVA_OPTS%" == "" goto gotJavaOpts
set "JAVA_OPTS=-Xmx512m"
:gotJavaOpts

if not "%PORT%" == "" goto gotPort
if not "%SSLPORT%" == "" goto gotPort
set "PORT=8080"
:gotPort

if not "%ORIGIN%" == "" goto gotOrigin
for /f %%i in ('hostname') do set "ORIGIN=%%i"
if "%PORT%" == "80" goto gotOrigin
if "%SSLPORT%" == "443" goto gotOrigin
set "ORIGIN=%ORIGIN%:%PORT%"
:gotOrigin

if not "%OPT%" == "" goto gotOpt
set "OPT=-d "%BASEDIR%" -o %ORIGIN% -r %REPOSITORY% -c %REPOSITORY_CONFIG%"
if not "%SECURITY_MANAGER%" == "false" goto gotOpt
set "OPT=%OPT% --trust"
:gotOpt

if "%PORT%" == "" goto gotPortOpt
set "OPT=%OPT% -p %PORT%"
:gotPortOpt

if "%SSLPORT%" == "" goto gotSslPortOpt
set "OPT=%OPT% -s %SSLPORT%"
:gotSslPortOpt

rem ----- Execute The Requested Command ---------------------------------------

set MAINCLASS=org.callimachusproject.Server

if ""%1"" == ""start"" goto doStart
if ""%1"" == ""stop"" goto doStop

echo Using BASEDIR:   %BASEDIR%
echo Using PORT:      %PORT% %SSLPORT%
echo Using ORIGIN:    %ORIGIN%
echo Using JAVA_HOME: %JAVA_HOME%

rem Get remaining unshifted command line arguments and save them in the
set CMD_LINE_ARGS=
:setRunArgs
if ""%1""=="""" goto doneSetRunArgs
set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
shift
goto setRunArgs
:doneSetRunArgs

IF NOT EXIST "%BASEDIR%\log" MKDIR "%BASEDIR%\log"
IF NOT EXIST "%BASEDIR%\run" MKDIR "%BASEDIR%\run"

rem Execute Java with the applicable properties
"%JAVA%" -server "-Duser.home=%BASEDIR%" "-Djava.library.path=%LIB%" "-Djava.io.tmpdir=%TMPDIR%" "-Djava.util.logging.config.file=%LOGGING%" "-Djava.mail.properties=%MAIL%" -classpath "%CLASSPATH%" %JAVA_OPTS% %SSL_OPTS% %MAINCLASS% --pid "%PID%" %OPT% %CMD_LINE_ARGS%
goto end

:doStart
echo Using BASEDIR:   %BASEDIR%
echo Using PORT:      %PORT% %SSLPORT%
echo Using ORIGIN:    %ORIGIN%
echo Using JAVA_HOME: %JAVA_HOME%

rem Get remaining command line arguments
shift
set CMD_LINE_ARGS=
:setStartArgs
if ""%1""=="""" goto doneSetStartArgs
set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
shift
goto setStartArgs
:doneSetStartArgs

IF NOT EXIST "%BASEDIR%\log" MKDIR "%BASEDIR%\log"
IF NOT EXIST "%BASEDIR%\run" MKDIR "%BASEDIR%\run"

rem Execute Java with the applicable properties
start "%NAME%" "%JAVAW%" -server "-Duser.home=%BASEDIR%" "-Djava.library.path=%LIB%" "-Djava.io.tmpdir=%TMPDIR%" "-Djava.util.logging.config.file=%LOGGING%" "-Djava.mail.properties=%MAIL%" -classpath "%CLASSPATH%" %JAVA_OPTS% %SSL_OPTS% %MAINCLASS% --pid "%PID%" -q %OPT% %CMD_LINE_ARGS%
goto end

:doStop
rem Execute Java with the applicable properties
"%JAVA%" -server -classpath "%CLASSPATH%;%JAVA_HOME%\lib\tools.jar" %MAINCLASS% --stop --pid "%PID%"
goto end

:end

