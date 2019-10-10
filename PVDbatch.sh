#!/bin/bash
# assumes PVDetector.sh is in PATH

realpath() {
  # $1 : relative filename
  echo "$(cd "$(dirname "$1")" && pwd)/$(basename "$1")"
}

if [[ $# -lt 1 ]]; then
	echo "Usage: $0 <apk-policy_dir> [owl_file] [mapping_file]"
	echo "<apk-policy_dir> should containg apks and policies with the same name and .apk and .txt extensions"
	exit 1
fi

if [[ $3 -eq 3 ]]; then
	owl=$2
	mapping=$3
else
	owl=/opt/PVDetector/ontology.owl
	mapping=/opt/PVDetector/mappings.csv
fi

for apk in $(ls "$1"/*.apk); do
	policy="${apk%.*}.txt"
	if [[ ! -f "$policy" ]]; then
		echo "Policy file ($policy) for $apk not found -- Skipping"
		continue
	fi
	echo "Processing `realpath $apk` with `realpath $policy`."
	PVDetector.sh "$owl" "$mapping" "`realpath $apk`" "`realpath $policy`"
	printf "\n"
done
