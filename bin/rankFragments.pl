#!/usr/bin/perl

my $INFILE = defined($ARGV[0])?$ARGV[0]:die("Please provide an input fragment file where each line is of the form,\n<probability:\%f> <PTB-style tree>\n");
my $TOPNUM = defined($ARGV[1])?$ARGV[1]:5;

my %COARSETOFRAG=();
my %COARSETOPROB=();

open(INFILE,"< $INFILE");
while(<INFILE>){
    chomp;
    my $line=$_;
    if(/^(\S+) (\((\S+) .*\))$/){
	my $prob = $1; my $frag=$2; my $startsym=$3;
	$startsym=~/^(.+)_\d+$/;
	my $coarsesym=$1;
	my %frags = defined($COARSETOFRAG{$coarsesym})?%{$COARSETOFRAG{$coarsesym}}:();
	if($frag=~/\(.*\(/){
	    $frags{$frag}=$prob;
	}
	$COARSETOFRAG{$coarsesym}=\%frags;
    }
}
close(INFILE);

foreach my $sym (sort (keys %COARSETOFRAG)){
    my %frags = %{$COARSETOFRAG{$sym}};
    print "$sym:\n";
    my $count=0;
    foreach my $frag (sort {$frags{$b} <=> $frags{$a}} (keys %frags)){
	if($count<$TOPNUM){
	    print "\t$frags{$frag} $frag\n";
	} else{
	    last;
	}
	$count++;
    }
}
