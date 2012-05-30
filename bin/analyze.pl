#!/usr/bin/perl

use strict;
use warnings;

my $COUNT_FILE=$ARGV[0];
my %countFILE=();
my %countRank=();

my $TSG_COUNTS=$ARGV[1];
my %tsgFILE=();
my %tsgRank=();

my %intersection = ();
my $M=defined($ARGV[2])?$ARGV[2]:5000;

open(COUNT_FILE, "< $COUNT_FILE");
my $rank=0;
print STDERR "processing the PTB count file...\n";
while(<COUNT_FILE>){
    if(/^\s*$/){
	next;
    }
    chomp;
    print STDERR "." if ($. % 1000 == 0);
    if(/^(\d+)\s+(.+)$/){
	my $freq = $1;
	my $remmedUS = &removeUnderscores($2);
	my @tarr = defined($countFILE{$freq})?@{$countFILE{$freq}}:();
	push(@tarr,$remmedUS);
	$countRank{$remmedUS}=$rank+1;
	$countFILE{$freq}=[@tarr];
	
    }
    $rank++;
}
print STDERR "\n";
close(COUNT_FILE);
my %CF_topM = %{&getTopM(\%countFILE, $M)};

$rank=0;

print STDERR "processing the TSG count file...\n";
open(TSG_COUNTS, "< $TSG_COUNTS");
my $lncount=0;
while(<TSG_COUNTS>){
    if(/^\s*$/){
	next;
    }
    chomp;
    $lncount=$.;
    print STDERR "." if ($lncount % 1000 == 0);
    if(/^(\d+)\s+(.+)$/){
	my $freq = $1;
	my $remmedUS = &removeUnderscores($2);
	#print "thisprint: $remmedUS\n";
	my @tarr = defined($tsgFILE{$freq})?@{$tsgFILE{$freq}}:();
	#push(@tarr,$remmedUS);
	$tsgRank{$remmedUS}=$rank+1;
	#$tsgFILE{$freq}=[@tarr];
	#is it in top M from count file?
	if(defined($CF_topM{$remmedUS})){
	    #print "\t$CF_topM{$remmedUS}, $freq\n";
	    my @intArr = ();
	    push(@intArr,$CF_topM{$remmedUS});
	    push(@intArr,$freq);
	    #print "$remmedUS ; ". $#intArr ."=> \t(";
	    #for my $a (@intArr){
		#print "$a, ";
	    #}
	    #print ")\n";
	    $intersection{$remmedUS}=[@intArr];
	}
	$rank++;
    }
}
print STDERR "\n";

my $allPTB=0; my $allTSG=0;
$allPTB = keys %countRank;
$allTSG = keys %tsgRank;
for my $a (sort {$countRank{$a} <=> $countRank{$b}} (keys %intersection)){
    print("$a ");
    print(&percentile($countRank{$a},$allPTB) . " ");
    print(&percentile($tsgRank{$a}, $allTSG) . "\n");
}

sub percentile(){
    my $num=shift; my $denom=shift;
    return sprintf("%.3f", 100*($denom-$num)/$denom);
}

sub removeUnderscores(){
    my $arg = shift;
    $arg =~ s/\_(\S+)\_/$1/g;
    return $arg;
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


