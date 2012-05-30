#!/bin/bash

export wsjpath=/export/common/data/corpora/LDC/LDC99T42/treebank_3/parsed/mrg/wsj/
java -Xmx8g -cp /home/hltcoe/fferraro/code/laptsg/classes edu.jhu.coe.util.BinarizeCorpus -path ${wsjpath} -out /home/hltcoe/fferraro/data/wsj/binarizeHead -b HEAD
