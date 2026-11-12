if grep -q "FAILURES\!\!\!" testOutput.txt ; then exit 1 ; else exit 0 ; fi
