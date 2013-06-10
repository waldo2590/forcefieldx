@REM ###########################################################################
@REM Title: Force Field X.
@REM
@REM Description: Force Field X - Software for Molecular Biophysics.
@REM
@REM Copyright: Copyright (c) Michael J. Schnieders 2001-2012.
@REM
@REM This file is part of Force Field X.
@REM
@REM Force Field X is free software; you can redistribute it and/or modify it
@REM under the terms of the GNU General Public License version 3 as published by
@REM the Free Software Foundation.
@REM
@REM Force Field X is distributed in the hope that it will be useful, but WITHOUT
@REM ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
@REM FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
@REM details.
@REM
@REM You should have received a copy of the GNU General Public License along with
@REM Force Field X; if not, write to the Free Software Foundation, Inc., 59 Temple
@REM Place, Suite 330, Boston, MA 02111-1307 USA
@REM ###########################################################################

@echo off

set ERROR_CODE=0

:init
@REM Decide how to startup depending on the version of windows

@REM -- Win98ME
if NOT "%OS%"=="Windows_NT" goto Win9xArg

@REM set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" @setlocal

@REM -- 4NT shell
if "%eval[2+2]" == "4" goto 4NTArgs

@REM -- Regular WinNT shell
set CMD_LINE_ARGS=%*
goto WinNTGetScriptDir

@REM The 4NT Shell from jp software
:4NTArgs
set CMD_LINE_ARGS=%$
goto WinNTGetScriptDir

:Win9xArg
@REM Slurp the command line arguments.  This loop allows for an unlimited number
@REM of agruments (up to the command line limit, anyway).
set CMD_LINE_ARGS=
:Win9xApp
if %1a==a goto Win9xGetScriptDir
set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
shift
goto Win9xApp

:Win9xGetScriptDir
set SAVEDIR=%CD%
%0\
cd %0\..\..
set BASEDIR=%CD%
cd %SAVEDIR%
set SAVE_DIR=
goto repoSetup

:WinNTGetScriptDir
set BASEDIR=%~dp0\..

:repoSetup


if "%JAVACMD%"=="" set JAVACMD=java

if "%REPO%"=="" set REPO=%BASEDIR%

set CLASSPATH="%BASEDIR%"\etc;"%BASEDIR%"\bin\ffx-all.jar

set EXTRA_JVM_ARGUMENTS=-Xms1G -Xmx1G -Xss1M 

goto endInit

@REM Reaching here means variables are defined and arguments have been captured
:endInit

%JAVACMD% %JAVA_OPTS% %EXTRA_JVM_ARGUMENTS% -classpath %CLASSPATH_PREFIX%;%CLASSPATH% -Djava.system.class.loader="ffx.FFXClassLoader" -Dapp.name="FFX Job Scheduler" -Dapp.repo="%REPO%" -Dbasedir="%BASEDIR%" edu.rit.pj.cluster.JobScheduler %CMD_LINE_ARGS%
if ERRORLEVEL 1 goto error
goto end

:error
if "%OS%"=="Windows_NT" @endlocal
set ERROR_CODE=1

:end
@REM set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" goto endNT

@REM For old DOS remove the set variables from ENV - we assume they were not set
@REM before we started - at least we don't leave any baggage around
set CMD_LINE_ARGS=
goto postExec

:endNT
@endlocal

:postExec

if "%FORCE_EXIT_ON_ERROR%" == "on" (
  if %ERROR_CODE% NEQ 0 exit %ERROR_CODE%
)

exit /B %ERROR_CODE%
