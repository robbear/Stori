#!/bin/bash

# Command line build tool.
# Use like ant:
# -- hfbuild <sku> <debug or release> <proguard or dev> <build string> <password> [install to device]
# -- hfbuild trial|release|amazon|amazon_trial debug|release proguard|dev 201112060900 password install

EXPECTED_ARGS=5
E_BADARGS=65

usage()
{
    echo "hfbuild - command line tool to build Stori indicating sku as first parameter"
    echo "usage: hfbuild <trial|release|amazon|amazon_trial> <debug|release> <proguard|dev> <buildstring> <password> [install]"
    echo ""
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
    mkdir build
    mkdir build/assets
    mkdir build/gen
    mkdir build/keystore
    mkdir build/libs
    mkdir build/proguard
    mkdir build/res
    mkdir build/src
}

copyTrunkTreeToBuildTree()
{
    echo "---"
    echo "Copying trunk tree to build tree"
    cp -R Stori/keystore/ build/keystore/
    cp -R Stori/libs/ build/libs/
    cp -R proguard/ build/proguard/
    cp -R Stori/src/main/res/ build/res/
    cp -R Stori/src/main/java/ build/src/
    cp -R ../google-play-services_lib build
    cp ./.classpath build
    cp ./.project build
    cp ./Stori/src/main/AndroidManifest.xml build
    cp ./project.properties build
    cp ./build.xml build
    cp ./ant.properties build
    cp ./local.properties build
    cp ./proguard-project.txt build
}

copySkuExceptionsToBuildTree()
{
    echo "---"
    echo "Copying exceptions to build tree"
    if [ "$1" != "release" ]
    then
        cp -R skus/$1/exceptions/ build/
    fi
}

populateStringReplace()
{
    echo "---"
    echo "Populating string resources from skus/$1/stringreplace files"

    if [ "$1" != "release" ]
    then
        dirs=("values" "values-fr" "values-ja")
        for f in "${dirs[@]}"
        do
            stringExtensionFile=skus/$1/stringreplace/$f/extensions.xml
            cp $stringExtensionFile build/res/$f/extensions.xml
        done
    fi
}

setBuildStringForRelease()
{
    echo "---"
    echo "Setting build string for release sku"
    sed -i -e 's/g000000000000/'$1'/g' build/src/com/stori/stori/Config.java
}

setBuildStringForTrial()
{
    echo "---"
    echo "Setting build string for trial sku"
    sed -i -e 's/g000000000000/'$1'/g' build/src/com/stori/stori/Config.java
}

fixAntPropertiesForPassword()
{
    echo "---"
    echo "Fix build/ant.properties to hold password"
    sed -i -e 's/foo.store.password=foo/key.store.password='$1'/g' build/ant.properties
    sed -i -e 's/foo.alias.password=foo/key.alias.password='$1'/g' build/ant.properties
}

fixProjectPropertiesToBuildForDev()
{
    echo "---"
    echo "Fix build/project.properties to build for dev"
    sed -i -e 's/proguard.config/#proguard.config/g' build/project.properties
}

moveToBuildDirectory()
{
    echo "---"
    echo "Moving to build directory"
    cd build
}

invokeAnt()
{
    echo "---"
    echo "Invoking ant to build the project"
    ant clean
    ant $1
}

returnToMainDirectory()
{
    echo "---"
    echo "Returning to invoked directory"
    cd ..
}

#
# Check for expected arguments
#
if [ $# -lt $EXPECTED_ARGS ]
then
	usage
	exit $E_BADARGS
fi

removeOldBuildTree
createBuildDirectory
copyTrunkTreeToBuildTree

#case "$1" in
#    'trial')
#    adjustTrialSkuPackage
#    ;;
#    'amazon_trial')
#    adjustTrialSkuPackage
#    ;;
#esac

#copySkuExceptionsToBuildTree $1
#populateStringReplace $1
fixAntPropertiesForPassword $5

if [ "$3" != "proguard" ]
then
    fixProjectPropertiesToBuildForDev
fi

case "$1" in
    'release')
	setBuildStringForRelease $4
	;;
    'amazon')
	setBuildStringForRelease $4
	;;
    'trial')
	setBuildStringForTrial $4
	;;
    'amazon_trial')
        setBuildStringForTrial $4
	;;
esac

moveToBuildDirectory
invokeAnt $2
returnToMainDirectory

echo "Done!"

exit 0
