#!/bin/bash

# Command line build tool to build slideshare web
# usage: jitsubuild.sh <test1|test2|test3|production>

EXPECTED_ARGS=1
E_BADARGS=65
SUBDOMAIN_NAME=""
BUILD_TARGET=""
DOMAINS_KEY=""

set -o pipefail     # trace ERR through pipes
set -o errtrace     # trace ERR through 'time command' and other functions

error_handler()
{
  JOB="$0"          # job name
  LASTLINE="$1"     # line of error occurrence
  LASTERR="$2"      # error code
  echo "ERROR in ${JOB} : line ${LASTLINE} with exit code ${LASTERR}"
  exit 1
}

trap 'error_handler ${LINENO} ${$?}' ERR

usage()
{
  echo "jitsubuild - command line tool to build slideshare web"
  echo "usage: ./jitsubuild.sh <test1|test2|test3|production>"
  echo ""
}

createVersionFile()
{
  echo "---"
  echo "Building version.txt file"
  git rev-parse HEAD > ./NodeWebSite/version.txt
}

removeOldBuildTree()
{
  echo "---"
  echo "Removing any existing build tree"
  rm -rf ./build
}

createBuildDirectory()
{
  echo "---"
  echo "Creating build directory"
  mkdir ./build/
  mkdir ./build/NodeWebSite/
  cp -R ./NodeWebSite/controllers ./build/NodeWebSite/controllers/
  cp -R ./NodeWebSite/logger/ ./build/NodeWebSite/logger/
  cp -R ./NodeWebSite/public/ ./build/NodeWebSite/public/
  cp -R ./NodeWebSite/views/ ./build/NodeWebSite/views/
  cp -R ./NodeWebSite/utilities/ ./build/NodeWebSite/utilities/
  cp ./NodeWebSite/*.js ./build/NodeWebSite/
  cp ./NodeWebSite/package.json ./build/NodeWebSite/
  cp ./NodeWebSite/version.txt ./build/NodeWebSite/
  mkdir ./build/NodeWebSite/logfiles/
  cp ./NodeWebSite/logfiles/readme.txt ./build/NodeWebSite/logfiles/
}

runTimeStamper()
{
  echo "---"
  echo "Build timestamp.txt file"
  date +%Y%m%d%H%M%S > ./build/NodeWebSite/timestamp.txt
}

runStylesCacheBuster()
{
  echo "---"
  echo "Running sed to cache-bust image files in styles"
  timeStamp=$(cat ./build/NodeWebSite/timestamp.txt)

  DIRS="./build/NodeWebSite/public/staticfiles/styles/"

  for d in $DIRS
  do
    for f in $d*.css
    do
      sed -e "s/.png/.png?$timeStamp/g" -e "s/.jpg/.jpg?$timeStamp/g" -e "s/.jpeg/.jpeg?timeStamp/g" -e "s/.gif/.gif?timeStamp/g" <$f >./build/temp.css
      rm $f
      mv ./build/temp.css $f
    done
  done
}

fixPackageJsonForDeployment()
{
  echo "---"
  echo "Updating package.json with deployment subdomain"
  sed -e "s/\"name\": \"slidesharecom\"/\"name\": \"$SUBDOMAIN_NAME\"/g" -e "s/\"subdomain\": \"slidesharedotcom\"/\"subdomain\": \"$SUBDOMAIN_NAME\"/g" -e "s/\"domains\"/\"$DOMAINS_KEY\"/g" <./build/NodeWebSite/package.json >./build/temp.json
  rm ./build/NodeWebSite/package.json
  mv ./build/temp.json ./build/NodeWebSite/package.json
}

renameStaticfiles()
{
  echo "---"
  echo "Renaming staticfiles directory"
  versionString=$(cat ./build/NodeWebSite/version.txt)
  mv ./build/NodeWebSite/public/staticfiles ./build/NodeWebSite/public/$versionString
}

#
# Check for expected arguments
#
if [ $# -lt $EXPECTED_ARGS ]
then
  usage
  exit $E_BADARGS
fi

case "$1" in
  'test1')
    SUBDOMAIN_NAME="hftest1"
    DOMAINS_KEY="domains_ignore"
    ;;
  'test2')
    SUBDOMAIN_NAME="hftest2"
    DOMAINS_KEY="domains_ignore"
    ;;
  'test3')
    SUBDOMAIN_NAME="hftest3"
    DOMAINS_KEY="domains_ignore"
    ;;
  'production')
    SUBDOMAIN_NAME="slidesharedotcom"
    DOMAINS_KEY="domains"
    ;;
  *)
    usage
    exit $E_BADARGS
esac

BUILD_TARGET=$1

echo "SUBDOMAIN_NAME is set to $SUBDOMAIN_NAME"
echo "BUILD_TARGET is set to $BUILD_TARGET"

createVersionFile
removeOldBuildTree
createBuildDirectory
runTimeStamper
runStylesCacheBuster
fixPackageJsonForDeployment
renameStaticfiles
echo "Done!"
exit 0
