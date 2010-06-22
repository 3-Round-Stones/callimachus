@echo off

rem Set the lib dir relative to the batch file's directory
set BIN_DIR=%~dp0
set BASE_DIR=%~dp0\..
set LIB_DIR=%~dp0\..\lib
set TMP_DIR=%~dp0\..\tmp
set LOG_CONF=%~dp0\logging.properties
set MAIN=org.callimachusproject.Server
rem echo LIB_DIR = %LIB_DIR%
rem echo LOG_CONF = %LOG_CONF%

rem Slurp the command line arguments. This loop allows for an unlimited number
rem of arguments (up to the command line limit, anyway).
set CMD_LINE_ARGS=%1
if ""%1""=="""" goto setupArgsEnd
shift
:setupArgs
if ""%1""=="""" goto setupArgsEnd
set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
shift
goto setupArgs

:setupArgsEnd

if "%JAVA_HOME%" == "" goto noJavaHome
if not exist "%JAVA_HOME%\bin\java.exe" goto noJavaHome
goto javaHome

:noJavaHome
set JAVA=java
goto javaHomeEnd

:javaHome
set JAVA=%JAVA_HOME%\bin\java

:javaHomeEnd

:checkJdk14
"%JAVA%" -version 2>&1 | findstr "1.4" >NUL
IF ERRORLEVEL 1 goto checkJdk15
echo Java 5 or newer required to run the server
goto end

:checkJdk15
"%JAVA%" -version 2>&1 | findstr "1.5" >NUL
IF ERRORLEVEL 1 goto java6
rem use java.ext.dirs hack
rem echo Using java.ext.dirs to set classpath
"%JAVA%" -server -mx512m -Dfile.encoding=UTF-8 "-Djava.library.path=%BIN_DIR%" "-Djava.io.tmpdir=%TMP_DIR%" "-Djava.util.logging.config.file=%LOG_CONF%" -Djava.ext.dirs="%LIB_DIR%" %MAIN% -d "%BASE_DIR%" %CMD_LINE_ARGS%
goto end

:java6
rem use java 6 wildcard feature
rem echo Using wildcard to set classpath
"%JAVA%" -server -mx512m -Dfile.encoding=UTF-8 "-Djava.library.path=%BIN_DIR%" "-Djava.io.tmpdir=%TMP_DIR%" "-Djava.util.logging.config.file=%LOG_CONF%" -cp "%LIB_DIR%\*" %MAIN% -d "%BASE_DIR%" %CMD_LINE_ARGS%
goto end

:end