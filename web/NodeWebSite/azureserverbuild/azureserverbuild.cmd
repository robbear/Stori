@echo on

REM - This file gets copied to the bin directory by setup_web.cmd as part of server set up.
REM - Note that the working directory becomes the bin directory.

echo Downloading tools from http://hfdotcom.blob.core.windows.net/deployment-files
curl -O http://hfdotcom.blob.core.windows.net/deployment-files/7z.exe
if %ERRORLEVEL% neq 0 goto error
curl -O http://hfdotcom.blob.core.windows.net/deployment-files/7z.dll
if %ERRORLEVEL% neq 0 goto error
curl -O http://hfdotcom.blob.core.windows.net/deployment-files/iisnode.msi
if %ERRORLEVEL% neq 0 goto error
curl -O http://hfdotcom.blob.core.windows.net/deployment-files/vcredist_x64.exe
if %ERRORLEVEL% neq 0 goto error
echo OK

REM - we'll use the following when Joyent fixes the msi installer for LocalSystem installation.
REM - See issue 4012 (https://github.com/joyent/node/issues/4021)
REM curl -O http://hfdotcom.blob.core.windows.net/deployment-files/node-v0.10.8-x64.msi
REM if %ERRORLEVEL% neq 0 goto error
REM Instead, we'll just unzip our packaged version
curl -O http://hfdotcom.blob.core.windows.net/deployment-files/nodejs.zip
if %ERRORLEVEL% neq 0 goto error
echo Unpacking nodejs to the "%programfiles%\nodejs" directory
7z x -y nodejs.zip -o"%programfiles%"
if %ERRORLEVEL% neq 0 goto error

echo Installing Visual Studio 2010 C++ Redistributable Package...
vcredist_x64.exe /q
if %ERRORLEVEL% neq 0 goto error
echo OK

REM - we'll use the following when Joyent fixes the msi installer for LocalSystem installation.
REM - See issue 4012 (https://github.com/joyent/node/issues/4021)
REM echo Installing Node.js
REM start /wait msiexec.exe /quiet /i node-v0.10.8-x64.msi ADDDEFAULT=NodeRunTime,npm
REM if %ERRORLEVEL% neq 0 goto error
REM echo OK

echo Building version.txt file
cd slideshare/web
"%ProgramFiles(x86)%\Git\bin\git.exe" rev-parse HEAD > NodeWebSite\version.txt
set /p versionstring=<NodeWebSite\version.txt
if %ERRORLEVEL% neq 0 goto error
cd ../..

echo Building timestamp.txt file
echo %date:~-4,4%%date:~-10,2%%date:~-7,2%%time:~0,2%%time:~3,2%%time:~6,2%| "%ProgramFiles(x86)%\Git\bin\sed.exe" 's/ /0/g' > slideshare\web\NodeWebSite\timestamp.txt
set /p timestamp=<slideshare\web\NodeWebSite\timestamp.txt
if %ERRORLEVEL% neq 0 goto error

echo Renaming staticfiles directory to %versionstring%
ren slideshare\web\NodeWebSite\public\staticfiles %versionstring%
if %ERRORLEVEL% neq 0 goto error

echo Copying NodeWebSite to approot
xcopy slideshare\web\NodeWebSite ..\ /c /e /y /EXCLUDE:exclude.txt
copy /y ..\Web.cloud.config ..\Web.config

echo Running npm install
cd ..
echo npm LOG > npmlog.txt
call "%ProgramFiles%\nodejs\npm.cmd" install 1>> npmlog.txt 2>> npmlog_error.txt
cd bin
echo OK

echo Installing iisnode...
start /wait msiexec.exe /quiet /i iisnode.msi
if %ERRORLEVEL% neq 0 goto error
echo OK

echo Removing repository
rd /s /q slideshare

echo AzureServerBuild.cmd - SUCCESS
exit /b 0

:error

echo AzureServerBuild.cmd - FAILED
exit /b -1
