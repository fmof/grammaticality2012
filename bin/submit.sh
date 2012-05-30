#!/bin/bash

export FRANK_PROJECT_PATH=${HOME}/code/laptsg/

: ${R_CUT=5000}
: ${K_CUT=50000}
: ${TREEBANK="WSJ"}
: ${minmem=12G}
: ${maxmem=32G}
: ${ALL_DEPTH_ONE=0}
: ${LEFT_OVER=""}
: ${CORPUS="/export/common/data/corpora/LDC/LDC99T42/treebank_3/parsed/mrg/wsj/"}
: ${UNIVERSAL_TAG_LANG=""}
: ${UNIVERSAL_TAG_LOC=""}
#: ${UNIVERSAL_TAG_LOC="/home/hltcoe/fferraro/code/universal_pos_tags.1.02/"}
#: ${UNIVERSAL_TAG_LANG="-universalTagEnglish english"}


cmd="java -Xmx${maxmem} -cp ${FRANK_PROJECT_PATH}classes edu.jhu.coe.syntax.SubtreeAnalyzerByRule -corpus ${CORPUS} -treebank ${TREEBANK} -rulecriterion ${R_CUT} -cutoff ${K_CUT} -incrementalprint ${LEFT_OVER} -alldepthone ${ALL_DEPTH_ONE} ${BINARIZE} ${UNIVERSAL_TAG_LANG} ${UNIVERSAL_TAG_LOC}"

echo "$cmd"

$cmd

