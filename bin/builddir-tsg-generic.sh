#!/bin/bash

# Parses with a specified grammar in a given directory

. ~/.bashrc
. $CACHEPIPE/bashrc

set -u

dir=$1
grammar=$2
name=$3

MHOME=/home/hltcoe/mpost/

extract_tsg=$MHOME/expts/treekernel12/scripts/extract_tsg_features.pl
map=$grammar.map
grammar=$grammar.mj

if [[ ! -e "$grammar" ]]; then
	echo "can't find grammar $grammar, quitting"
	exit
fi

if [[ ! -e "$map" ]]; then
	echo "can't find map file $map, quitting"
	exit
fi

cd $dir

mkdir $name
cd $name

# UNK the testing data
cachecmd unks "cat ../words | perl /home/hltcoe/mpost/code/dptsg/scripts/corpus2unks.pl -lexicon /home/hltcoe/mpost/expts/lsa11/data/lex.02-21 > corpus.UNK" ../words corpus.UNK

# parse with 100 nodes, 16 GB each
export JOSHUA=/home/hltcoe/mpost/code/joshua
cachecmd parse "cat corpus.UNK | perl $JOSHUA/scripts/training/parallelize/parallelize.pl -j 100 -pmem 16g -- /home/hltcoe/mpost/code/cky/llncky - $grammar > parse.out" corpus.UNK parse.out

# split the output into parses and scores
cachecmd split "cat parse.out | ${MHOME}/bin/splittabs.pl scores parses" parse.out scores parses

# unflatten the rules
#cachecmd restore "cat parses | $DPTSG/scripts/convert_from_johnson.pl -map $map -scrub 0 -delex 0 > parses.full" parses parses.full

# extract features
#cachecmd extract "$extract_tsg -feature rules -arg 1 < parses.full > ../rules.$name" parses.full ../rules.$name
