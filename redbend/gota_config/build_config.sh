#!/bin/bash

ZIP=zip
JAVA=java

SIGN_TOOL=signapk.jar
SCRIPT_DIR=`dirname $(readlink -f $0)`

ZIP_DIR=`mktemp -d`
ZIP_FILE=$ZIP_DIR/rb_ota.zip

usage()
{
	# cd is needed to list the possible models
	cd $SCRIPT_DIR
	echo $0 "<model> <public_key> <private_key> <output_file>"
	echo possible models are: `find * -maxdepth 0 -type d`
	exit 1
}

if [[ -z $1 || -z $2 || -z $3  || -z $4 ]] ; then
	usage
fi

MODEL=$1
PUBLIC_KEY=`readlink -f $2`
PRIVATE_KEY=`readlink -f $3`
SIGNED_ZIP=`readlink -f $4`

if [[ ! -f $PUBLIC_KEY || ! -f $PRIVATE_KEY ]]; then
	echo "Security keys are not found"
	usage
fi

set -e

if ( ! which $ZIP > /dev/null ); then
	echo Zip tool '$ZIP' is not in the path
	exit 1
fi

if ( ! which $JAVA > /dev/null ); then
	echo Java tool '$JAVA' is not in the path
	exit 1
fi

cd $SCRIPT_DIR

if [[ ! -d $MODEL ]]; then
	echo "Invalid device model: $MODEL"
	usage
fi

cd $MODEL
$ZIP -r $ZIP_FILE *
$JAVA -Xmx512M -jar $SCRIPT_DIR/$SIGN_TOOL -w $PUBLIC_KEY $PRIVATE_KEY $ZIP_FILE $SIGNED_ZIP
rm -r $ZIP_DIR
echo Successfully created Google OTA config in $SIGNED_ZIP

