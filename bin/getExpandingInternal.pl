while(<>){ 
    chomp;
    if(/^\d+\s+\((\S+)\s(.*)\)\s*$/){	
	$root=$1; 
	$innerfull=$2; 
	if($innerfull =~ /\(|\)/){
	    next;
	}
	#$innerfull =~ s/\((\S|[^\(]\)//g; 
	$innerfull =~ s/\)//g;
	
	push(@lines,"$root $innerfull");
	#print "for line $_, \n\tpush $root $innerfull\n";
	$rootcount{$root}++; 
	@innerarr = split(/\s/,$innerfull); 
	foreach $a (@innerarr){
	    #print "\t\t$a\n";
	    $innercount{$a}++;
	}
    }
} 
#see if the LHS ever occurred on the RHS as well,
#i.e., if it could be coupled
if(0){
foreach $a (@lines){
    if($a =~ /^(\S+)\s(.+)$/){
	$lhs =$1; $rhs=$2; $rhsorig=$2;
	#print "$a\n";
	#for every observed LHS
	$rhs =~ s/\_[^\_]+\_//g;
	for $tmp (keys %rootcount){
	    #is it in this rhs?
	    
	    if($rhs =~ /(\b|[^_])\Q${tmp}\E\b/){
		if($lhs eq '-LRB-'){
		    print "$lhs => $rhs ::: $tmp\n";
		}
		if(0 && $tmp eq '$'){
		    #print "matched with $b **AND** $rhs **AND** $lhs\n";
		    next;#print "$lhs => $rhsorig :: $b\n";
		}
		#if so, then RHS has a node that could
		#be internal
		$actrootcount{$lhs}++;
		last;
	    }
	}
    }
}

#frequency of "root" nodes that could have some internal node...
for $a (keys %actrootcount){
    print "$a $actrootcount{$a}"; 
    if(defined($innercount{$a})){
	print " $innercount{$a}\n";
    } else{
	print " 0\n";
    }
} 
}

if(1){
    for $a (keys %rootcount){
	print "$a $rootcount{$a}"; 
	if(defined($innercount{$a})){
	    print " $innercount{$a}\n";
	} else{
	    print " 0\n";
	}
    }
}


if(0){
    for $a (keys %innercount){
	if(!defined($rootcount{$a})){
	    print "$a :: 0 :: $innercount{$a}\n";
	}
    }
}
