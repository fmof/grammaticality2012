#!/usr/bin/perl

$COUNT_FILE = $ARGV[0] || die("please give input count file");
$COUNT_FILE =~ /\/([^\/]+R(\d+)\D?K(\d+)[^\/]*?)$/;
$infile = $1;
#print("$infile\n\n");
$R = $2;
$K = $3;
$OUTPATH = $ARGV[1] || die("please give output directory");
system("mkdir -p ${OUTPATH}/qc");

$JOBNAME = defined($ARGV[2])?$ARGV[2]:"extractFeatures_R${R}_K${K}";

$HOME = $ENV{'HOME'};

$dataset = 'qc/data';

@SOURCE = qw(train_5500.label TREC_10.label);


foreach $source (@SOURCE){
    $testcorpus = "/home/hltcoe/mpost/expts/treekernel12/${dataset}/${source}";
    system("mkdir -p ${OUTPATH}/${source}");
    $outputpath = "${OUTPATH}/${source}/$infile";
    $qsubcmd = "qsub -N ${JOBNAME} -o ${HOME}/code/lapcfg/experiments/extractRuleCountFeatures/ -e ${HOME}/code/lapcfg/experiments/extractRuleCountFeatures/err/ -M ferraro\@cs.jhu.edu -m bea -q mem.q -v COUNTS=\"${COUNT_FILE}\",TEST_CORPUS=\"${testcorpus}\",OUTPUT_FEATURES=\"${outputpath}\" ${HOME}/code/lapcfg/bin/extractFeaturesUsingExistingCounts.sh";
    system($qsubcmd);
    print "$qsubcmd\n";
}

