#!/bin/bash

export FRANK_PROJECT_PATH=${HOME}/code/laptsg/

java -cp ${FRANK_PROJECT_PATH}classes edu.jhu.coe.syntax.SubtreeAnalyzerByRule -rulecriterion ${R_CUT} -cutoff ${K_CUT} 
