#!/bin/bash

: ${PROJECT_CP="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"}
export PROJECT_CP

: ${R_CUT=5000}
: ${K_CUT=50000}
: ${TREEBANK="WSJ"}
: ${maxmem=32G}
: ${ALL_DEPTH_ONE=0}
: ${LEFT_OVER=""}
: ${UNIVERSAL_TAG_LANG=""}
: ${UNIVERSAL_TAG_LOC=""}

CORPUS=$(readlink -f "$1")
if [[ -z "$CORPUS" ]]; then
    echo "cannot find path to corpus directory."
    echo "$0 <corpus path>"
    exit 1
fi


cmd="java -Xmx${maxmem} -cp ${PROJECT_CP}classes edu.jhu.coe.syntax.SubtreeAnalyzerByRule \
-corpus ${CORPUS} -treebank ${TREEBANK} -rulecriterion ${R_CUT} -cutoff ${K_CUT} \
-incrementalprint ${LEFT_OVER} -alldepthone ${ALL_DEPTH_ONE} ${BINARIZE} \
${UNIVERSAL_TAG_LANG} ${UNIVERSAL_TAG_LOC}"


echo "$cmd"

$cmd

