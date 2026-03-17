@echo off
setlocal
cd /d "%~dp0"
start "" /b runtime\bin\javaw.exe --module-path "app\lib" --add-modules javafx.controls -cp "subtitle-redactor-1.0-SNAPSHOT.jar" com.farabia.Main