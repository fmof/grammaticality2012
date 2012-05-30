#!/usr/bin/perl

use strict;
use warnings;

my $COUNT_FILE=$ARGV[0];
my %countFILE=();
my %countRank=();
my @countHistogram=();

my $TSG_COUNTS=$ARGV[1];
my %tsgFILE=();
my %tsgRank=();
my @tsgHistogram=();

my %intersection = ();
my $M=defined($ARGV[2])?$ARGV[2]:5000;

sub getRuleLength(){
    my $line = shift;
    my $curr_depth = shift;
    my $max_depth = shift;
    if($line=~ /^\((.*)\)$/){
	return getRuleLength($1, $curr_depth+1, &max($max_depth,$curr_depth+1));
    } elsif($line =~ /^[^\(]+\((.*)$/){
	return getRuleLength($1, $curr_depth+1, &max($max_depth,$curr_depth+1));
    } elsif($line =~ /^[^\(]+\)\s*(.*)$/){
 	return getRuleLength($1, $curr_depth-1, $max_depth);
    } else{
	return $max_depth;
    }
}


open(COUNT_FILE, "< $COUNT_FILE");
my $rank=0;
print STDERR "processing the PTB count file...\n";
my $linesincount=0;
while(<COUNT_FILE>){
    if(/^\s*$/){
	next;
    }
    chomp;
    print STDERR "." if ($. % 1000 == 0);
    if(/^(\d+)\s+(.+)$/){
	my $freq = $1;
	my $remmedUS = &removeUnderscores($2);
	my $length = &getRuleLength($remmedUS, 0, 0);
	#print("$remmedUS :: $length\n");
	$countHistogram[$length]++;
	
	$linesincount++;
    }
    $rank++;
}


print STDERR "\n";
close(COUNT_FILE);

print STDERR "processing the TSG count file...\n";
open(TSG_COUNTS, "< $TSG_COUNTS");
my $lncount=0;
my $tsgcountfile=0;
while(<TSG_COUNTS>){
    if(/^\s*$/){
	next;
    }
    chomp;
    $lncount=$.;
    if(0 && $tsgcountfile>$linesincount){
	last;
    }
    print STDERR "." if ($lncount % 1000 == 0);
    if(/^(\d+)\s+(.+)$/){
	my $freq = $1;
	my $remmedUS = &removeUnderscores($2);
	my $length = &getRuleLength($remmedUS, 0, 0);
	#print("$remmedUS :: $length\n");
	$tsgHistogram[$length]++;
	$tsgcountfile++;
    }
}
print STDERR "\n";

my $countsum=0; my $tsgsum=0;
for(my $i = 1; $i<= max($#tsgHistogram, $#countHistogram); $i++){
    
    print "$i";
    if($i<=$#countHistogram && exists($countHistogram[$i])){
	print ",$countHistogram[$i],". $countHistogram[$i]/$linesincount;
    } else{
	print ",0,0";
    }
    if($i<=$#tsgHistogram && exists($tsgHistogram[$i])){
	print ",$tsgHistogram[$i],". $tsgHistogram[$i]/$tsgcountfile ."\n";
    } else {
	print ",0,0\n";
    }
}
#print "\t${countsum}\t${tsgsum}\n";

sub percentile(){
    my $num=shift; my $denom=shift;
    return sprintf("%.3f", 100*($denom-$num)/$denom);
}

sub removeUnderscores(){
    my $arg = shift;
    $arg =~ s/\_(\S+)\_/$1/g;
    return $arg;
}

sub max(){
    my $a=shift; 
    my $b=shift;
    return $a>=$b?$a:$b;
}


sub getTopM(){
    my $infile1=shift;
    my %infile = %{$infile1};
    my $M=shift;
    #sort $countFILE by key;
    my %cf=();
    my @srt_cF_keys = sort {$b <=> $a} keys %infile;
    my @stmp = @srt_cF_keys;#[0,$M];
    my $index = 0;
    for my $freq1 (@stmp){
	if($index > $M){
	    last;
	}
	
	my @tarr = @{$infile{$freq1}};
	for my $rule1 (@tarr){
	    $cf{$rule1}=$freq1;
	    #print "$freq1 => $rule1\n";
	    $index++;
	}
    }
    return \%cf;
}


