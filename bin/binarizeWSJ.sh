#!/bin/bash

wsjpath=$(readlink -f "$1")

function usage {
    cat <<EOF
$0 <path to corpus directory> <path to output directory>
EOF
exit 1
}

: ${PROJECT_CP="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"}

if [[ -z "$wsjpath" ]]; then
    echo "cannot find path to corpus directory."
    usage
fi

if [[ -z "$2" ]]; then
    echo "please provide an output directory"
    usage
elif [[ ! -d "$2" ]]; then
    echo "making new output directory."
    mkdir -p "$2" || exit 1
fi
outpath=$(readlink -f "$2")

export wsjpath

java -Xmx8g -cp ${PROJECT_CP} edu.jhu.coe.util.BinarizeCorpus -path ${wsjpath} -b HEAD -out ${outpath}
