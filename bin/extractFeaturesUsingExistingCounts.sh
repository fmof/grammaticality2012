#!/bin/bash

export FRANK_PROJECT_PATH=${HOME}/code/.lapcfg/

cmd="java -Xmx20G -cp ${FRANK_PROJECT_PATH}classes edu.jhu.coe.ling.SubtreeAnalyzerByRule -extractcounts 0 -texttofragment ${COUNTS} -testcorpus ${TEST_CORPUS} -outputtextfeatures ${OUTPUT_FEATURES}"

echo $cmd
$cmd

#experiments/subtreeCount/ruleCriterion/o6185383/ptb_R31_K50000
#smallcorpora/foster-data/class/good/berkeley/rand3
