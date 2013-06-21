#!/usr/bin/perl

$COUNT_FILE = $ARGV[0] || die("please give input count file");
$COUNT_FILE =~ /\/([^\/]+R(\d+)\D?K(\d+)[^\/]*?)$/;
$infile = $1;
$R = $2;
$K = $3;
$OUTPATH = $ARGV[1] || die("please give output directory");

$dataset = defined($ARGV[2])?$ARGV[2]:'foster-data';

$JOBNAME = defined($ARGV[3])?$ARGV[3]:"extrFeat_${dataset}_R${R}_K${K}";

$HOME = $ENV{'HOME'};
$DATA_LOC = $ENV{'GRAM_DATA_LOC'};


@SPLITSECT = qw(class dev test);
@SOURCE = qw(good bad);

if($dataset eq 'foster-data'){
    @SOURCE = qw(good wong2010parser);
}


foreach $split (@SPLITSECT){
    foreach $source (@SOURCE){
	system("mkdir -p ${OUTPATH}/${split}/${source}");
	$testcorpus = "$DATA_LOC/${dataset}/${split}/${source}/berkeley/parses.full";
	my $tjn = "${JOBNAME}-${split}-${source}";
	$outputpath = "${OUTPATH}/${split}/${source}/${JOBNAME}";
	$qsubcmd = "qsub -N ${tjn} ${HOME}/experiments/extractRuleCountFeatures/ -e ${HOME}/experiments/extractRuleCountFeatures/err/ -l num_proc=1,mem_free=25G,h_rt=16:00:00 -v COUNTS=\"${COUNT_FILE}\",TEST_CORPUS=\"${testcorpus}\",OUTPUT_FEATURES=\"${outputpath}\" -cwd -b y \"java -Xmx20G -cp classes edu.jhu.coe.syntax.SubtreeAnalyzerByRule -extractcounts 0 -texttofragment $COUNT_FILE -testcorpus ${testcorpus} -outputtextfeatures ${outputpath}\" ";
	system($qsubcmd);
	print "$qsubcmd\n";
    }
}
