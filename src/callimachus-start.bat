@echo off
rem callimachus-start.bat
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

set "EXECUTABLE=%BASEDIR%\bin\callimachus.bat"

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

call "%EXECUTABLE%" start %CMD_LINE_ARGS%

:end