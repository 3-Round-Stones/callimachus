@echo off
rem 
rem Portions Copyright (c) 2009-10 Zepheira LLC, Some Rights Reserved
rem Portions Copyright (c) 2010-11 Talis Inc, Some Rights Reserved
rem Portions Copyright (c) 2012 3 Round Stones Inc, Some Rights Reserved
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
echo This environment variable is needed to run this program
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

set "EXECUTABLE=%BASEDIR%\bin\%NAME%.bat"

rem Check that target executable exists
if exist "%EXECUTABLE%" goto okExec
echo Cannot find "%EXECUTABLE%"
echo This file is needed to run this program
goto end
:okExec

rem Get remaining unshifted command line arguments and save them in the
set CMD_LINE_ARGS=
:setArgs
if ""%1""=="""" goto doneSetArgs
set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
shift
goto setArgs
:doneSetArgs

"%EXECUTABLE%" reset %CMD_LINE_ARGS%

:end
