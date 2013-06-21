Frank Ferraro

This accompanies 

@InProceedings{ferraro-grammaticality-2012,
    author = {Ferraro, Francis and Post, Matt and {Van Durme}, Benjamin},
    title = {Judging Grammaticality with Count-Induced Tree Substitution Grammars},
    year = {2012},
    booktitle = {The Proceedings of the 7th Workshop on Innovative Use of NLP for Building Educational Applications},
    url = {http://cs.jhu.edu/~ferraro/papers/ferraro-grammaticality-2012.pdf},
    poster = {http://cs.jhu.edu/~ferraro/papers/ferraro-grammaticality-2012_poster.pdf}
}

This readme explains how to:
1. extract count-based tree fragments (Algorithm 1), and
2. replicate the experiments (Section 4).

Requirements
============

The two tasks have slightly different dependencies, but generally you need Java >= 1.6. 
The code and experiments were run on a Linux set-up. Compile using ANT:

```$ ant
Buildfile: [dir]/build.xml
     [echo] Using Java version 1.7.

prepare:

compile:
    [javac] Compiling 6 source files to [dir]/classes

BUILD SUCCESSFUL
Total time: 1 second```

Running
=======

Extracting Fragments
--------------------

Flags you'll be most interested in:
-corpus %s   	    [REQUIRED: corpus directory]
-rulecriterion %d   [maximal fragment size]
-cutoff %d          [number of fragments to keep around]

For example, to get the five most commonly used CFG rules in sections 2-21 of WSJ,
```$ java -cp classes/ edu.jhu.laptsg.syntax.SubtreeAnalyzerByRule -rulecriterion 1 -cutoff 5 -corpus <WSJ path>
[...]
### FINAL OUTPUT

76617 (PP IN NP)
48723 (, _,_)
41098 (DT _the_)
39020 (. _._)
36165 (ROOT S)
$
```

If you want incremental updates, turn on:
-incrementalprint 

e.g.,
```$ java -cp classes/ edu.jhu.laptsg.syntax.SubtreeAnalyzerByRule -rulecriterion 2 -cutoff 5 -incrementalprint -corpus <WSJ path>
[...]
### OUTPUT FROM AT MOST 1 RULE
76617 (PP IN NP)
48723 (, _,_)
41098 (DT _the_)
39020 (. _._)
36165 (ROOT S)
Expanding at most 2 rules

### OUTPUT FROM AT MOST 2 RULES
76617 (PP IN NP)
48723 (, _,_)
41098 (DT _the_)
39020 (. _._)
36165 (ROOT S)

[...]
```

Many more options are available via the -h flag
$ java -cp classes edu.jhu.laptsg.syntax.SubtreeAnalyzerByRule -h


There's also a wrapper script available:
* bin/extract_fragments.sh <path/to/corpus/dir>

NOTE: This implementation is not resource-conscious and it uses a lot of memory!

Replicating Experiments
-----------------------

The experiments in this paper build on those in Post (2011):
https://github.com/mjpost/post2011judging

Please first set up the data according to the instructions there (most important steps are 4-8).

To extract features from count_file:
```java -cp classes edu.jhu.coe.syntax.SubtreeAnalyzerByRule \
-extractcounts 0 #don't extract counts\
-texttofragment <count_file> \
-testcorpus <parsed_corpus> \
-outputtextfeatures <output_file>```

If you used the incremental print option, you'll need to run the original count file through 
bin/splitOutputFiles.pl (see below). 

The script
`bin/submitRuleCountFeatureJobs.pl <count_file> <out_directory_path> [dataset] [jobname]` is a 
Perl script to submit SGE jobs to extract count-based features for all splits of a given dataset 
(BLLIP or Foster).
This *REQUIRES* the environment variable $GRAM_DATA_LOC to be set to where the data are. 



Other Useful Helper Scripts
===========================

* bin/splitOutputFiles.pl <path/to/count_file> 
  - Split incremental output from bin/extract_fragment.sh

* bin/rankFragments.pl <path/to/count_file> [N=5]
  - List the top N fragments for each symbol sorted by weight
  - Weight-space should be monotonically-isomorphic to probability space

* bin/binarizeWSJ.sh
  - Little built-in wrapper to binarize a treebank
