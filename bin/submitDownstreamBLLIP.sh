#!/bin/bash

: ${name=ptb_R${R}_K${K}}

#cd /home/hltcoe/fferraro/experiments/treekernel12/BLLIP; force=1 ./eval.sh ptb_R31_K50000_fixedUnderscores ptb_R1_K200000 sentlens
cd /home/hltcoe/fferraro/experiments/treekernel12/BLLIP 
echo $PWD
cmd="./eval.sh ${name} sentlens"
echo $cmd
$cmd

