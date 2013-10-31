@echo off

rem Command line tool to build slideshare web deployment packages
rem buildazuredeployment <test1|test2|test3|production> <gituser> <gitpassword> <gitbranch>

if "%1"=="" goto usage
if "%2"=="" goto usage
if "%3"=="" goto usage
if "%4"=="" goto usage
if "%1"=="production" (
    set package_name=Production
    goto buildit
)
if "%1"=="test1" (
    set package_name=HFTest1
    goto buildit
)
if "%1"=="test2" (
    set package_name=HFTest2
    goto buildit
)
if "%1"=="test3" (
    set package_name=HFTest3
    goto buildit
)

:usage
@echo buildazuredeployment - command line tool to build slideshare web  deployment packages
@echo usage: buildazuredeployment ^<test1^|test2^|test3^|production^> ^<gituser^> ^<gitpassword^> ^<gitbranch^>
@echo.
goto omega

:buildit
@echo ---
@echo Removing any existing build tree
rd /s /q build

@echo ---
@echo Creating build directory
md build

@echo ---
@echo Copying Azure bin directory to build
xcopy NodeWebSite\bin build\NodeWebSite\bin\ /c /e
copy NodeWebSite\bin\roleproperties.txt build\

@echo ---
@echo Copying deployment configuration files to build
copy deployments\%1\*.* build\

@echo ---
@echo Running sed to modify the git clone command in setup_web.cmd
sed -e 's/{{GITUSER}}/%2/g' -e 's/{{GITPASSWORD}}/%3/g' -e 's/{{GITBRANCH}}/%4/g' <build\NodeWebSite\bin\setup_web.cmd >build\NodeWebSite\bin\temp.txt
del build\NodeWebSite\bin\setup_web.cmd
copy build\NodeWebSite\bin\temp.txt build\NodeWebSite\bin\setup_web.cmd
del build\NodeWebSite\bin\temp.txt

@echo ---
@echo Runing cspack.exe to generate the Azure deployment package
cd build
"%programfiles%\Microsoft SDKs\Windows Azure\.NET SDK\2012-10\bin\cspack.exe" ServiceDefinition.csdef /out:%package_name%.cspkg /sitePhysicalDirectories:NodeWebSite;Web;.\NodeWebSite /rolePropertiesFile:NodeWebSite;roleproperties.txt
cd ..
echo.

@echo ---
@echo Done

:omega
