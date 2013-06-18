@echo off
rem 
rem Portions Copyright (c) 2012-2013 3 Round Stones Inc., Some Rights Reserved
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
if "%NAME:~-1%" NEQ "-" (
  set "NAME=%NAME:~0,-1%"
  goto name
)
rem strip dash (-)
set "NAME=%NAME:~0,-1%"
:gotName

rem Ensure that any user defined CLASSPATH variables are not used on startup.
set CLASSPATH=

if exist "%CONFIG%" goto gotConfigFile
set "CONFIG=%BASEDIR%/etc/%NAME%.conf"
:gotConfigFile

if exist "%CONFIG%" goto readConfig
setlocal EnableDelayedExpansion
for /f "tokens=1,* delims==" %%i in ('find "=" "%BASEDIR%/etc/%NAME%-defaults.conf"') do (
  set "key=%%i"
  set "value=%%~j"
  ( if "!key!" == "#PORT" ( echo PORT="!value!" ) else  (if "!key!" == "#ORIGIN" ( echo ORIGIN="!value!" ) else  ( echo !key!="!value!" ) ) ) >> "%BASEDIR%/etc/%NAME%.conf"
)
set "CONFIG=%BASEDIR%/etc/%NAME%.conf"
:readConfig

if not "%TMPDIR%" == "" goto gotTmpdir
set "TMPDIR=%BASEDIR%\tmp"
:gotTmpdir

rem Get standard environment variables
if not exist "%CONFIG%" goto okConfig
setlocal EnableDelayedExpansion
IF NOT EXIST "%TMPDIR%" MKDIR "%TMPDIR%"
for /f "tokens=1,* delims==" %%i in ('find /V "#" "%CONFIG%"') do (
  set "line=set "%%i=%%~j"
  if not "!line:~4,1!" == "-" echo !line!>> "%TMPDIR%\%NAME%-conf.bat"
)
call "%TMPDIR%\%NAME%-conf.bat"
del "%TMPDIR%\%NAME%-conf.bat"
:okConfig

rem Read relative config paths from BASEDIR
cd "%BASEDIR%"

rem check for a JDK in the BASEDIR
for /d %%i in ("%BASEDIR%\jdk*") do set JDK_HOME=%%i
for /d %%i in ("%BASEDIR%\jdk*") do set JAVA_HOME=%%i\jre

rem Lookup the JDK in the registry
if exist "%JAVA_HOME%" goto gotJavaHome
set "KeyName=HKEY_LOCAL_MACHINE\SOFTWARE\JavaSoft\Java Development Kit\1.7"
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

if not "%MAIL%" == "" goto gotMail
set "MAIL=%BASEDIR%\etc\mail.properties"
:gotMail

if not "%REPOSITORY_CONFIG%" == "" goto gotRepositoryConfig
set "REPOSITORY_CONFIG=etc/%NAME%-repository.ttl"
:gotRepositoryConfig

setlocal ENABLEDELAYEDEXPANSION
set "CLASSPATH=%BASEDIR%\classes\"
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

IF NOT EXIST "%BASEDIR%\log" MKDIR "%BASEDIR%\log"
IF NOT EXIST "%BASEDIR%\run" MKDIR "%BASEDIR%\run"

rem ----- Execute The Requested Command ---------------------------------------

set MAINCLASS=org.callimachusproject.Setup

echo Using BASEDIR:   %BASEDIR%
echo Using PORT:      %PORT% %SSLPORT%
echo Using ORIGIN:    %ORIGIN%
echo Using JAVA_HOME: %JAVA_HOME%
echo Using JDK_HOME:  %JDK_HOME%

set CMD_LINE_ARGS=
set CMD_LINE_PARAMS=
:setStartArgs
if ""%1""=="""" goto doneSetStartArgs
set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
shift
goto setStartArgs
:doneSetStartArgs

rem Execute Java with the applicable properties
"%JAVA_HOME%\bin\java" "-Djava.io.tmpdir=%TMPDIR%" "-Djava.mail.properties=%MAIL%" "-Dorg.callimachusproject.config.repository=%REPOSITORY_CONFIG%"  -classpath "%CLASSPATH%" -XX:OnOutOfMemoryError="taskkill /F /PID %%p" %JAVA_OPTS% %MAINCLASS% -b "%BASEDIR%" -c "%CONFIG%" -k "%BASEDIR%\backups" -e -l "cmd.exe /c bin\%NAME%-start.bat %CMD_LINE_ARGS%
goto end

:end

