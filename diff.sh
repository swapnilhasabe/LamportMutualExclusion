#!/bin/bash

rm out.txt
for ((i = 0; i < $1; i++))
do
	for ((j = 0; j < $1; j++))
	do
		diff $2$i.out $2$j.out > out.txt
		if [ -s out.txt ] 
		then
			echo new$i.out new$j.out
			`cat out.txt`
			exit
		else
		 	continue 
		fi
	done
done


echo success
