#!/bin/bash

dir_flowdroid=/opt/FlowDroid
dir_pvdetector=/opt/PVDetector
jar_pvdetector=PrivacyViolationDetection.jar

if [[ $# -ne 4 ]]; then
	echo "Usage: $0 <owl_file> <mapping_file> <apk> <policy>"
	exit 1
fi

owl=$1
mapping=$2
policy=$4

# convert to absolute path since we're moving around later
if [[ "${3:0:1}" != "/" ]]; then
	apk_path=$(pwd)"/$3"
else
	apk_path="$3"
fi

fd_out="/tmp/$(basename -- "$apk_path").flowdroidout"


# generate FlowDroid Object
printf "Performing dataflow analysis on %s.\n" "$apk_path"
cd "$dir_flowdroid"
java -Xms65536m -Xmx196608m  -cp soot-trunk.jar:soot-infoflow.jar:soot-infoflow-android.jar:slf4j-api-1.7.5.jar:axml-2.0.jar \
	soot.jimple.infoflow.android.TestApps.Test $apk_path \
	android.jar --nostatic --aliasflowins --layoutmode none > $fd_out 
printf "Dataflow analysis complete.\n"

# violation detection
printf "Performing violation detection.\n"
java -jar "$dir_pvdetector/$jar_pvdetector" "$owl" "$mapping" "$policy" "$fd_out"
