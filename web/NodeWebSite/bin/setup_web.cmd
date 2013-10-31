@echo on

if exist "success.txt" (
  echo Application installation was already successful. No need to rerun. Exiting.
  exit /b 0
)

cd /d "%~dp0"

echo Granting permissions for Network Service to the web root directory...
icacls ..\ /grant "Network Service":(OI)(CI)W
if %ERRORLEVEL% neq 0 goto error
echo OK

echo Downloading tools from http://hfdotcom.blob.core.windows.net/deployment-files
curl -O http://hfdotcom.blob.core.windows.net/deployment-files/gitinstall.exe
if %ERRORLEVEL% neq 0 goto error
echo OK

echo Installing Git
start /wait gitinstall.exe /verysilent /nocancel /suppressmsgboxes
if %ERRORLEVEL% neq 0 goto error
echo OK

echo Cloning slideshare
"%ProgramFiles(x86)%\Git\bin\git.exe" clone https://{{GITUSER}}:{{GITPASSWORD}}@github.com/robbear/slideshare.git -b {{GITBRANCH}}
if %ERRORLEVEL% neq 0 goto error
echo OK

echo Copying azureserverbuild.cmd to this directory
copy /y slideshare\web\NodeWebSite\azureserverbuild\azureserverbuild.cmd .\
if %ERRORLEVEL% neq 0 goto error
echo OK

echo Running azureserverbuild.cmd
call azureserverbuild.cmd
if %ERRORLEVEL% neq 0 goto error
echo OK

echo Application installation successful > "success.txt"
echo SUCCESS
exit /b 0

:error

echo FAILED
exit /b -1
