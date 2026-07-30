@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem

<<<<<<< HEAD
@if "%DEBUG%"=="" @echo off
=======
@if "%DEBUG%" == "" @echo off
>>>>>>> fac3e1bf76900eca384d895d842c13066d9bcd67
@rem ##########################################################################
@rem
@rem  Gradle startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
<<<<<<< HEAD
if "%DIRNAME%"=="" set DIRNAME=.
@rem This is normally unused
=======
if "%DIRNAME%" == "" set DIRNAME=.
>>>>>>> fac3e1bf76900eca384d895d842c13066d9bcd67
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

@rem Resolve any "." and ".." in APP_HOME to make it shorter.
for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

@rem Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
<<<<<<< HEAD
set DEFAULT_JVM_OPTS=-Dfile.encoding=UTF-8 "-Xmx64m" "-Xms64m"
=======
set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"
>>>>>>> fac3e1bf76900eca384d895d842c13066d9bcd67

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
<<<<<<< HEAD
if %ERRORLEVEL% equ 0 goto execute
=======
if "%ERRORLEVEL%" == "0" goto execute
>>>>>>> fac3e1bf76900eca384d895d842c13066d9bcd67

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto execute

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar


@rem Execute Gradle
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*

:end
@rem End local scope for the variables with windows NT shell
<<<<<<< HEAD
if %ERRORLEVEL% equ 0 goto mainEnd
=======
if "%ERRORLEVEL%"=="0" goto mainEnd
>>>>>>> fac3e1bf76900eca384d895d842c13066d9bcd67

:fail
rem Set variable GRADLE_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
<<<<<<< HEAD
set EXIT_CODE=%ERRORLEVEL%
if %EXIT_CODE% equ 0 set EXIT_CODE=1
if not ""=="%GRADLE_EXIT_CONSOLE%" exit %EXIT_CODE%
exit /b %EXIT_CODE%
=======
if  not "" == "%GRADLE_EXIT_CONSOLE%" exit 1
exit /b 1
>>>>>>> fac3e1bf76900eca384d895d842c13066d9bcd67

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
