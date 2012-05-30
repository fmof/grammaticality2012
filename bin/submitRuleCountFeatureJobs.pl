#!/usr/bin/perl

$COUNT_FILE = $ARGV[0] || die("please give input count file");
$COUNT_FILE =~ /\/([^\/]+R(\d+)\D?K(\d+)[^\/]*?)$/;
$infile = $1;
#print("$infile\n\n");
$R = $2;
$K = $3;
$OUTPATH = $ARGV[1] || die("please give output directory");

$dataset = defined($ARGV[2])?$ARGV[2]:'foster-data';

$JOBNAME = defined($ARGV[3])?$ARGV[3]:"extractFeatures_${dataset}_R${R}_K${K}";

$HOME = $ENV{'HOME'};


@SPLITSECT = qw(class dev test);
@SOURCE = qw(good wong2010parser);

foreach $split (@SPLITSECT){
    foreach $source (@SOURCE){
	system("mkdir -p ${OUTPATH}/${split}/${source}");
	$testcorpus = "/home/hltcoe/mpost/expts/treekernel12/${dataset}/${split}/${source}/berkeley/parses.full";
	my $tjn = "${JOBNAME}-${split}-${source}";
	$outputpath = "${OUTPATH}/${split}/${source}/${tjn}";
	$qsubcmd = "qsub -N ${tjn} -o ${HOME}/experiments/extractRuleCountFeatures/ -e ${HOME}/experiments/extractRuleCountFeatures/err/ -M ferraro\@cs.jhu.edu -m bea -l num_proc=1,mem_free=25G,h_vmem=35G,h_rt=16:00:00 -v COUNTS=\"${COUNT_FILE}\",TEST_CORPUS=\"${testcorpus}\",OUTPUT_FEATURES=\"${outputpath}\" ${HOME}/code/laptsg/bin/extractFeaturesUsingExistingCounts.sh";
	#$qsubcmd = "qsub -N ${JOBNAME} -o ${HOME}/code/lapcfg/experiments/extractRuleCountFeatures/ -e ${HOME}/code/lapcfg/experiments/extractRuleCountFeatures/err/ -M ferraro\@cs.jhu.edu -m bea -q mem.q -v COUNTS=\"${COUNT_FILE}\",TEST_CORPUS=\"${testcorpus}\",OUTPUT_FEATURES=\"${outputpath}\" ${HOME}/code/lapcfg/bin/extractFeaturesUsingExistingCounts.sh";
	system($qsubcmd);
	print "$qsubcmd\n";
    }
}
