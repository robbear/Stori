@echo off

rem Command line tool to build all slideshare web deployment packages
rem azuredeployments.cmd <gituser> <gitpassword>

if "%1"=="" goto usage
if "%2"=="" goto usage
goto buildit

:usage
@echo azuredeployments - command line tool to build all slideshare web deployment packages
@echo usage: azuredeployments ^<gituser^> ^<gitpassword^>
@echo.
goto omega

:buildit
echo ---
echo Building HFTest1.cspkg
call buildazuredeployment test1 %1 %2 deploy-to-hftest1
copy /y build\HFTest1.cspkg deployments\test1
echo.

echo ---
echo Building HFTest2.cspkg
call buildazuredeployment test2 %1 %2 deploy-to-hftest2
copy /y build\HFTest2.cspkg deployments\test2
echo.

echo ---
echo Building HFTest3.cspkg
call buildazuredeployment test3 %1 %2 deploy-to-hftest3
copy /y build\HFTest3.cspkg deployments\test3
echo.

echo ---
echo Building Production.cspkg
call buildazuredeployment production %1 %2 deploy-to-production
copy /y build\Production.cspkg deployments\production
echo.

@echo ---
@echo Done

:omega