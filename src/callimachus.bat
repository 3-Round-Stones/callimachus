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

if exist "%CONFIG%" goto gotConfigFile
set "CONFIG=%BASEDIR%/etc/%NAME%.conf"
:gotConfigFile

if exist "%CONFIG%" goto readConfig
set "CONFIG=%BASEDIR%/etc/%NAME%-defaults.conf"
setlocal EnableDelayedExpansion
for /f "tokens=1,* delims==" %%i in ('find "=" "%BASEDIR%/etc/%NAME%-defaults.conf"') do (
  set "key=%%i"
  set "value=%%~j"
  ( if "!key!" == "#PORT" ( echo PORT="!value!" ) else  (if "!key!" == "#ORIGIN" ( echo ORIGIN="!value!" ) else  ( echo !key!="!value!" ) ) ) >> "%BASEDIR%/etc/%NAME%.conf"
)
set "CONFIG=%BASEDIR%/etc/%NAME%.conf"
:readConfig

rem Get standard environment variables
if not exist "%CONFIG%" goto okConfig
setlocal EnableDelayedExpansion
IF NOT EXIST "%BASEDIR%\tmp" MKDIR "%BASEDIR%\tmp"
for /f "tokens=1,* delims==" %%i in ('find /V "#" "%CONFIG%"') do (
  set "line=set "%%i=%%~j"
  if not "!line:~4,1!" == "-" echo !line!>> "%BASEDIR%\tmp\%NAME%-conf.bat"
)
call "%BASEDIR%\tmp\%NAME%-conf.bat"
del "%BASEDIR%\tmp\%NAME%-conf.bat"
:okConfig

rem Read relative config paths from BASEDIR
cd "%BASEDIR%"

rem check for a JDK in the BASEDIR
for /d %%i in ("%BASEDIR%\jdk*") do set JDK_HOME=%%i
for /d %%i in ("%BASEDIR%\jdk*") do set JAVA_HOME=%%i\jre

rem Lookup the JDK in the registry
if exist "%JAVA_HOME%" goto gotJavaHome
set "KeyName=HKEY_LOCAL_MACHINE\SOFTWARE\JavaSoft\Java Development Kit\1.6"
set Cmd=reg query "%KeyName%" /s
for /f "tokens=2*" %%i in ('%Cmd% ^| find "JavaHome"') do set JDK_HOME=%%j

if not exist "%JDK_HOME%" goto gotNoHome
set "JAVA_HOME=%JDK_HOME%\jre"
:gotNoHome

rem Make sure prerequisite environment variable is set
if exist "%JAVA_HOME%\bin\java.exe" goto gotJavaHome
echo The JAVA_HOME environment variable "%JAVA_HOME%" is not defined correctly
echo The JAVA_HOME environment variable is needed to run this server
goto end
:gotJavaHome

if exist "%JDK_HOME%" goto gotJdkHomeVar
set "JDK_HOME=%JAVA_HOME%\.."
:gotJdkHomeVar

if exist "%JDK_HOME%\bin\javac.exe" goto gotJdkHome
echo The JDK_HOME environment variable "%JDK_HOME%" is not defined correctly
echo The JDK_HOME environment variable is needed to run this server
goto end
:gotJdkHome

if not "%PID%" == "" goto gotOut
set "PID=%BASEDIR%\run\%NAME%.pid"
:gotOut

if not "%TMPDIR%" == "" goto gotTmpdir
set "TMPDIR=%BASEDIR%\tmp"
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

if not exist "%SSL%" goto okSslOptions
setlocal EnableDelayedExpansion
IF NOT EXIST "%BASEDIR%\tmp" MKDIR "%BASEDIR%\tmp"
for /f "tokens=1,* delims==" %%i in ('find /V "#" "%SSL%"') do (
  set "line=++JvmOptions=-D%%i=%%~j"
  if not "!line:~4,1!" == "-" set "PRUN_SSL_OPTS=!PRUN_SSL_OPTS! !line!"
)
:okSslOptions

if not "%REPOSITORY%" == "" goto gotRepository
set "REPOSITORY=repositories/%NAME%"
:gotRepository

if not "%REPOSITORY_CONFIG%" == "" goto gotRepositoryConfig
set "REPOSITORY_CONFIG=etc/%NAME%-repository.ttl"
:gotRepositoryConfig

setlocal ENABLEDELAYEDEXPANSION
for /r "lib" %%a IN (*.jar) do set CLASSPATH=!CLASSPATH!;%%a

if not "%JAVA_OPTS%" == "" goto gotJavaOpts
set "JAVA_OPTS=-Xmx512m"
:gotJavaOpts

if not "%PORT%" == "" goto gotPort
if not "%SSLPORT%" == "" goto gotPort
set "PORT=8080"
:gotPort

if not "%ORIGIN%" == "" goto gotOrigin
set "ORIGIN=http://localhost"
if "%PORT%" == "80" goto gotOrigin
set "ORIGIN=%ORIGIN%:%PORT%"
:gotOrigin

if not "%PRIMARY_ORIGIN%" == "" goto gotPrimaryOrigin
set "PRIMARY_ORIGIN=%ORIGIN%"
:gotPrimaryOrigin

if not "%OPTS%" == "" goto gotOpts
if not "%SECURITY_MANAGER%" == "true" goto gotOpts
set "OPTS=--trust"
:gotOpts

IF NOT EXIST "%BASEDIR%\log" MKDIR "%BASEDIR%\log"
IF NOT EXIST "%BASEDIR%\run" MKDIR "%BASEDIR%\run"

rem ----- Execute The Requested Command ---------------------------------------

set MAINCLASS=org.callimachusproject.Server
set SETUPCLASS=org.callimachusproject.Setup
set MONITORCLASS=org.callimachusproject.ServerMonitor

echo Using BASEDIR:   %BASEDIR%
echo Using PORT:      %PORT% %SSLPORT%
echo Using ORIGIN:    %ORIGIN%
echo Using JAVA_HOME: %JAVA_HOME%
echo Using JDK_HOME:  %JDK_HOME%

if ""%1"" == ""start"" goto doStart
if ""%1"" == ""stop"" goto doStop
if ""%1"" == ""setup"" goto doSetup
if ""%1"" == ""dump"" goto doDump
if ""%1"" == ""reset"" goto doReset

:doRun
rem ---- Run -------------------------------------------------------------------
if not exist "%REPOSITORY%" goto runSetup

rem Get remaining command line arguments
shift
set CMD_LINE_ARGS=
set CMD_LINE_PARAMS=
:setStartArgs
if ""%1""=="""" goto doneSetStartArgs
set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
set "PRUN_CMD_LINE_PARAMS=%PRUN_CMD_LINE_PARAMS%;%1"
shift
goto setStartArgs
:doneSetStartArgs

rem Execute Java with the applicable properties
"%JAVA_HOME%\bin\java" -server "-Duser.home=%BASEDIR%" "-Djava.io.tmpdir=%TMPDIR%" "-Djava.util.logging.config.file=%LOGGING%" "-Djava.mail.properties=%MAIL%" -classpath "%CLASSPATH%" -XX:OnOutOfMemoryError="taskkill /F /PID %%p" %JAVA_OPTS% %SSL_OPTS% %MAINCLASS% --pid "%PID%" -b "%BASEDIR%" %OPTS% %CMD_LINE_ARGS%
goto end

:doStart
rem ---- Start -----------------------------------------------------------------
if not exist "%REPOSITORY%" goto runSetup

rem Get remaining command line arguments
shift
set CMD_LINE_ARGS=
set CMD_LINE_PARAMS=
:setStartArgs
if ""%1""=="""" goto doneSetStartArgs
set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
shift
goto setStartArgs
:doneSetStartArgs

rem Execute Java with the applicable properties
start "%NAME%" "%JAVA_HOME%\bin\javaw" -server "-Duser.home=%BASEDIR%" "-Djava.io.tmpdir=%TMPDIR%" "-Djava.util.logging.config.file=%LOGGING%" "-Djava.mail.properties=%MAIL%" -classpath "%CLASSPATH%" -XX:OnOutOfMemoryError="taskkill /F /PID %%p" %JAVA_OPTS% %SSL_OPTS% %MAINCLASS% --pid "%PID%" -q -b "%BASEDIR%" %OPTS% %CMD_LINE_ARGS%
goto end

:doStop
rem ---- Stop ------------------------------------------------------------------

rem Get remaining command line arguments
shift
set CMD_LINE_ARGS=
set CMD_LINE_PARAMS=
:setStartArgs
if ""%1""=="""" goto doneSetStartArgs
set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
shift
goto setStartArgs
:doneSetStartArgs

rem Execute Java with the applicable properties
"%JAVA_HOME%\bin\java" -server "-Duser.home=%BASEDIR%" "-Djava.io.tmpdir=%TMPDIR%" -classpath "%CLASSPATH%;%JDK_HOME%\lib\tools.jar" %MONITORCLASS% --pid "%PID%" --stop %CMD_LINE_ARGS%
goto end

:doSetup
rem ---- Setup -----------------------------------------------------------------

rem Get remaining command line arguments
shift
set CMD_LINE_ARGS=
set CMD_LINE_PARAMS=
:setStartArgs
if ""%1""=="""" goto doneSetStartArgs
set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
shift
goto setStartArgs
:doneSetStartArgs

rem Execute Java with the applicable properties
FOR /F "tokens=1 delims=" %%A in ('dir /b lib\callimachus-webapp*.car') do SET "CAR_FILE=%%A"
"%JAVA_HOME%\bin\java" -server "-Duser.home=%BASEDIR%" "-Djava.io.tmpdir=%TMPDIR%" "-Djava.util.logging.config.file=%LOGGING%" "-Djava.mail.properties=%MAIL%" -classpath "%CLASSPATH%" -XX:OnOutOfMemoryError="taskkill /F /PID %%p" %JAVA_OPTS% %SETUPCLASS% -o %PRIMARY_ORIGIN: = -o % -c "%REPOSITORY_CONFIG%" -w "lib\%CAR_FILE%" -u "%USERNAME%" -e "%EMAIL%" -n "%FULLNAME%" %CMD_LINE_ARGS%
goto end

:doDump
rem ---- Dump ------------------------------------------------------------------

rem Get remaining command line arguments
shift
set CMD_LINE_ARGS=
set CMD_LINE_PARAMS=
:setStartArgs
if ""%1""=="""" goto doneSetStartArgs
set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
shift
goto setStartArgs
:doneSetStartArgs

rem Execute Java with the applicable properties
"%JAVA_HOME%\bin\java" -server "-Duser.home=%BASEDIR%" "-Djava.io.tmpdir=%TMPDIR%" -classpath "%CLASSPATH%;%JDK_HOME%\lib\tools.jar" %MONITORCLASS% --pid "%PID%" --dump "%BASEDIR%\log" %CMD_LINE_ARGS%
goto end

:doReset
rem ---- Reset -----------------------------------------------------------------

rem Get remaining command line arguments
shift
set CMD_LINE_ARGS=
set CMD_LINE_PARAMS=
:setStartArgs
if ""%1""=="""" goto doneSetStartArgs
set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
shift
goto setStartArgs
:doneSetStartArgs

rem Execute Java with the applicable properties
"%JAVA_HOME%\bin\java" -server "-Duser.home=%BASEDIR%" "-Djava.io.tmpdir=%TMPDIR%" -classpath "%CLASSPATH%;%JDK_HOME%\lib\tools.jar" %MONITORCLASS% --pid "%PID%" --reset %CMD_LINE_ARGS%
goto end

:runSetup
rem ---- Setup Required --------------------------------------------------------
echo The repository does not exist, please run the setup script first
goto end

:end

