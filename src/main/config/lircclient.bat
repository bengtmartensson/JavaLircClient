@echo off

REM Wrapper for LircClient as a command line program for Windows/MSDOS.

REM Command to call the desired Java VM
set JAVA=java

REM Where the files are located, change of desired
set APPLICATIONHOME=%~dp0

REM Normally no need to change the rest of the file
set JAR=%APPLICATIONHOME%\${project.name}-${project.version}-jar-with-dependencies.jar

"%JAVA%" -jar "%JAR%" %*
