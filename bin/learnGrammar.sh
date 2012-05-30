#!/bin/bash

export FRANK_PROJECT_PATH=${HOME}/code/laptsg/
: ${minmem=12G}
: ${maxmem=32G}
: ${CUTOFF=5000}
: ${RULES=4}
: ${gamma="0.5"}
: ${howtoobtain=""}
: ${ts=$(date +%Y%m%d-%H%M)}
: ${folder="smcd${SMCD}-K${CUTOFF}${howtoobtain}-R${RULES}-gamma${gamma}"}
: ${outdir=/home/hltcoe/fferraro/experiments/laptsg/wsj/${folder}}
: ${path=/export/common/data/corpora/LDC/LDC99T42/treebank_3/parsed/mrg/}
: ${traindev=""}
: ${TREEBANK=WSJ}
: ${UNIVERSAL_TAG_LANG=""}
: ${UNIVERSAL_TAG_LOC=""}
: ${outname="wsjptb"}
: ${CONSTRAINTS=${FRANK_PROJECT_PATH}/data/constraintSet/ptb_R31_K50000_allDepthOne_bin}

export outdir=${outdir}/${ts}

echo "mkdir -p ${outdir}"
mkdir -p ${outdir}

if [ -z "$traindev" ]; then
    traindev="-path ${path} "
fi

echo "timestamp = ${ts}"

cmd="time java -Xmx${maxmem} -cp ${FRANK_PROJECT_PATH}classes edu.jhu.coe.PCFGLA.GrammarTrainer ${traindev}  -out ${outdir}/${outname} -SMcycles ${SMCD} ${UNIVERSAL_TAG_LANG} ${UNIVERSAL_TAG_LOC} -constraints ${CONSTRAINTS} ${howtoobtain} -useConstraintSet -reobtainConstraints -cutoff ${CUTOFF} -rulecriterion ${RULES} -treebank ${TREEBANK}"

echo $cmd

#-agentlib:hprof=heap=all,cpu=samples,depth=15,file="${outdir}/prof.txt"

$cmd

