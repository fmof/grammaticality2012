#!/usr/bin/perl

$HOME = $ENV{'HOME'};

@K = (25000, 50000);

foreach $k (@K){
    for($i=3;$i<=10;$i++){
	$JOBNAME = "countSubTrees_R${i}_K${k}";
	$qsubcmd = "qsub -N ${JOBNAME} -o ${HOME}/scratch/ -e ${HOME}/scratch/ -M ferraro\@cs.jhu.edu -m bea -q himem.q -v R_CUT=$i,K_CUT=1$k ${HOME}/code/laptsg/bin/submitbinarize.sh";
	system($qsubcmd);
	print "$qsubcmd\n";
    }
}
