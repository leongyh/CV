#!/usr/bin/bash
# usage: runsuccess initfile goalfile type, where there is a solution
#   type is either "hard" or "medium"
echo "Checking a puzzle in 100 seconds; last script change = April 23, 2010"
ulimit -t 100
testdir=~cs61bl/code/proj3/testing/$3
j=/usr/sww/opt/jdk-1.6.0_26/bin
/bin/rm -f /tmp/out$$
echo $1 " " $2
$j/java -classpath . Solver $testdir/$1 $testdir/$2 > /tmp/out$$
if test "$?" -ne 0
then
    echo "*** Wrong exit status"
fi
$j/java Checker $testdir/$1 $testdir/$2 < /tmp/out$$
if test "$?" -ne 0
then
    echo "*** Incorrect solver output"
fi
/bin/rm -f /tmp/out$$
echo " "
