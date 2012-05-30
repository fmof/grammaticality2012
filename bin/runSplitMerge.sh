#!/bin/bash

export FRANK_PROJECT_PATH=${HOME}/code/laptsg/
: ${minmem=12G}
: ${maxmem=32G}
: ${SM=4}
: ${ts=$(date +%Y%m%d-%H%M)}
: ${folder="sm${SM}-${ts}"}
: ${outdir=/home/hltcoe/fferraro/experiments/laptsg/splitMerge/wsj/${folder}}
#: ${outdir=/home/hltcoe/fferraro/experiments/laptsg/smallwsj/${folder}}
: ${path=/export/common/data/corpora/LDC/LDC99T42/treebank_3/parsed/mrg/}


#echo $*
echo "mkdir -p ${outdir}"
mkdir -p ${outdir}

traindev="-path ${path} "
#traindev="-train ${FRANK_PROJECT_PATH}smallptb/wsj_03xx.mrg -validation ${FRANK_PROJECT_PATH}smallptb/wsj_0401.mrg "

echo "timestamp = ${ts}"

cmd="time java -Xmx${maxmem} -cp ${FRANK_PROJECT_PATH}classes edu.jhu.coe.PCFGLA.GrammarTrainer ${traindev}  -out ${outdir}/wsjptb-sm${SM} -SMcycles ${SM} -nocouple"


echo "$cmd"
#-agentlib:hprof=heap=all,cpu=samples,depth=15,file="${outdir}/prof.txt"

$cmd
