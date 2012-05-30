#!/bin/bash


: ${name=ptb_R${R}_K${K}}
: ${isdepth1=0}

if [ $isdepth1 -eq 1 ]; then
    depth1=rules.berkeley.1
fi

echo "cd /home/hltcoe/fferraro/code/lapcfg/experiments/extractRuleCountFeatures/treekernel12/BLLIP"
echo "force=1 ./eval.sh $name sentlens $depth1"

cd /home/hltcoe/fferraro/code/lapcfg/experiments/extractRuleCountFeatures/treekernel12/BLLIP; force=1 ./eval.sh $name sentlens $depth1