#!/bin/bash

# Command line build all tool.

EXPECTED_ARGS=2
E_BADARGS=65

usage()
{
    echo "hfbuildall - command line tool to invoke hfbuild.sh for all release builds"
    echo "./hfbuildall.sh <buildstring> <password>"
    echo ""
}

buildRelease()
{
    ./hfbuild.sh release release $1 $2
#   cp ./build/bin_ant/proguard/mapping.txt ./skus/release/release_bin/
    cp ./build/bin_ant/stori-release.apk ./skus/release/release_bin/stori-release.apk
}

buildTrial()
{
    ./hfbuild.sh trial release $1 $2
    cp ./build/bin_ant/proguard/mapping.txt ./skus/trial/release_bin/
    cp ./build/bin_ant/stori-release.apk ./skus/trial/release_bin/stori-trial.apk
}

buildAmazon()
{
    ./hfbuild.sh amazon release $1a $2
    cp ./build/bin_ant/proguard/mapping.txt ./skus/amazon/release_bin/
    cp ./build/bin_ant/stori-release.apk ./skus/amazon/release_bin/stori-release.apk
}

buildAmazonTrial()
{
    ./hfbuild.sh amazon_trial release $1a $2
    cp ./build/bin_ant/proguard/mapping.txt ./skus/amazon_trial/release_bin/
    cp ./build/bin_ant/stori-release.apk ./skus/amazon_trial/release_bin/stori-trial.apk
}

#
# Check for expected arguments
#
if [ $# -lt $EXPECTED_ARGS ]
then
    usage
    exit $E_BADARGS
fi

buildRelease $1 $2
#buildTrial $1 $2
#buildAmazon $1 $2
#buildAmazonTrial $1 $2

