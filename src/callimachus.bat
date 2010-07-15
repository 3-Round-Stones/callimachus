@echo off
rem callimachus.bat
rem 
rem Copyright (c) 2010 Zepheira LLC, Some Rights Reserved
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

rem Guess BASEDIR if not defined
set "CURRENT_DIR=%cd%"
if not "%BASEDIR%" == "" goto gotHome
set "BASEDIR=%CURRENT_DIR%"
if exist "%BASEDIR%\bin\callimachus.bat" goto okHome
cd ..
set "BASEDIR=%cd%"
cd "%CURRENT_DIR%"
:gotHome
if exist "%BASEDIR%\bin\callimachus.bat" goto okHome
echo The BASEDIR environment variable is not defined correctly
echo This environment variable is needed to run this program
goto end
:okHome

rem Ensure that any user defined CLASSPATH variables are not used on startup.
set CLASSPATH=

rem Get standard environment variables
if exist "%BASEDIR%\etc\callimachus.conf" goto getConfig
goto gotConfig
:getConfig
setlocal EnableDelayedExpansion
IF NOT EXIST "%BASEDIR%\tmp" MKDIR "%BASEDIR%\tmp"
for /f %%i in ('"type "%BASEDIR%\etc\callimachus.conf" | find /V "#""') do (
  set "line=set %%i"
  echo !line!>> "%BASEDIR%\tmp\callimachus-conf.bat"
)
call "%BASEDIR%\tmp\callimachus-conf.bat"
del "%BASEDIR%\tmp\callimachus-conf.bat"
:gotConfig

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
echo The JAVA_HOME environment variable is needed to run this program
goto exit
:gotJdkHome

set "JAVA=%JAVA_HOME%\bin\java"
set "JAVAW=%JAVA_HOME%\bin\javaw"

if not "%PID%" == "" goto gotOut
set "PID=%BASEDIR%\run\callimachus.pid"
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

if not "%LOGGING_CONFIG%" == "" goto gotLogging
set "LOGGING_CONFIG=%BASEDIR%/etc/logging.properties"
:gotLogging

setlocal ENABLEDELAYEDEXPANSION
for /r "%LIB%" %%a IN (*.jar) do set CLASSPATH=!CLASSPATH!;%%a

if not "%JAVA_OPTS%" == "" goto gotJavaOpts
set "JAVA_OPTS=-Xmx512m -Dfile.encoding=UTF-8"
:gotJavaOpts

if not "%PRUNSRV_JVM_OPTS%" == "" goto gotPrunsrvJvmOpts
set "PRUNSRV_JVM_OPTS=-Dfile.encoding=UTF-8"
:gotPrunsrvJvmOpts

if not "%PORT%" == "" goto gotPort
set "PORT=8080"
:gotPort

if not "%AUTHORITY%" == "" goto gotAuthority
for /f %%i in ('hostname') do set "AUTHORITY=%%i"
if "%PORT%" == "80" goto gotAuthority
set "AUTHORITY=%AUTHORITY%:%PORT%"
:gotAuthority

rem ----- Execute The Requested Command ---------------------------------------

set MAINCLASS=org.callimachusproject.Server

if ""%1"" == ""start"" goto doStart
if ""%1"" == ""stop"" goto doStop

echo Using BASEDIR:   "%BASEDIR%"
echo Using PORT:      "%PORT%"
echo Using AUTHORITY: "%AUTHORITY%"
echo Using JAVA_HOME: "%JAVA_HOME%"

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
"%JAVA%" -server "-Duser.home=%BASEDIR%" "-Djava.library.path=%LIB%" "-Djava.io.tmpdir=%TMPDIR%" "-Djava.util.logging.config.file=%LOGGING_CONFIG%" -classpath "%CLASSPATH%" %JAVA_OPTS% %MAINCLASS% -d "%BASEDIR%" -p "%PORT%" -a "%AUTHORITY%" --pid "%PID%" %OPT% %CMD_LINE_ARGS%
goto end

:doStart
echo Using BASEDIR:   "%BASEDIR%"
echo Using PORT:      "%PORT%"
echo Using AUTHORITY: "%AUTHORITY%"
echo Using JAVA_HOME: "%JAVA_HOME%"

IF NOT EXIST "%BASEDIR%\log" MKDIR "%BASEDIR%\log"
IF NOT EXIST "%BASEDIR%\run" MKDIR "%BASEDIR%\run"

rem Execute Java with the applicable properties
start "callimachus" "%JAVAW%" -server "-Duser.home=%BASEDIR%" "-Djava.library.path=%LIB%" "-Djava.io.tmpdir=%TMPDIR%" "-Djava.util.logging.config.file=%LOGGING_CONFIG%" -classpath "%CLASSPATH%" %JAVA_OPTS% %MAINCLASS% -d "%BASEDIR%" -p "%PORT%" -a "%AUTHORITY%" --pid "%PID%" %OPT% %CMD_LINE_ARGS%
goto end

:doStop
IF NOT EXIST "%BASEDIR%\log" MKDIR "%BASEDIR%\log"
IF NOT EXIST "%BASEDIR%\run" MKDIR "%BASEDIR%\run"

rem Execute Java with the applicable properties
"%JAVA%" -server -classpath "%CLASSPATH%;%JAVA_HOME%\lib\tools.jar" %JAVA_OPTS% %MAINCLASS% --stop --pid "%PID%"
goto end

:end

