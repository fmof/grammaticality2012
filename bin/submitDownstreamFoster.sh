#!/bin/bash

: ${name=ptb_R${R}_K${K}}

cd /home/hltcoe/fferraro/experiments/treekernel12/foster-data/
echo $PWD
cmd="./eval.sh ${name} sentlens"
echo $cmd
$cmd

#cd /home/hltcoe/fferraro/experiments/treekernel12/foster-data; force=1 ./eval.sh $name sentlens $depth1
