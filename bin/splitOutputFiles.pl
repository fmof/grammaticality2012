#!/usr/bin/perl

$R = 0;
$infile = $ARGV[0];
$infile =~ /^(.+)\/([^\/]+)R(\d+)_?K(\d+K?)[^\/]*\.(o\d+)$/i;
print "$infile\n";
$path = $1;
$NAME=$2;
$FINAL_R = $3;
$K = $4;
$pid = $5;

$NAME=~s/_$//;
$alo = defined($ARGV[1])?$ARGV[1]:0;

print "mkdir -p ${path}/${pid}\n";
print "r=$FINAL_R, k=$K\n";
system("mkdir -p ${path}/${pid}");
open(FILE, "< $infile");
if($alo){
    open(ALLALOOUT, "> ${path}/${pid}/all_cutout");
}
while(<FILE>){
    if(/OUTPUT FROM AT MOST (\d+) RULE/){
	$R=$1;
	open(OUT, "> ${path}/${pid}/${NAME}_R${R}_K${K}");
	if($alo){
	    open(ALOOUT, "> ${path}/${pid}/${NAME}_R${R}_K${K}_cutout");
	}
    } else{
	if($R > 0){
	    if(/^\{(.*)\}$/ && $alo){
		chomp;
		$dist = $1;
		@splitdist = split(/\, /,$dist);
		$c = 0;
		%map=();
		foreach $pair (@splitdist){
		    $c++;
		    $pair =~ /^(\d+)\=(\d+)$/;
		    print ALOOUT "$1,$2\n";
		    $map{$1}=$2;
		}
		for($c=1;$c<=$FINAL_R; $c++){		 
		    $valtoprint = defined($map{$c})?$map{$c}:0;
		    if($c==$FINAL_R){
			print ALLALOOUT "$valtoprint\n";
		    } else{
			print ALLALOOUT "$valtoprint,";
		    }
		}
	    } else{
		print OUT $_;
	    }
	}
    }
}
