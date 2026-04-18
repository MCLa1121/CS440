#!/bin/bash -l
#$ -P cs440
#$ -l h_rt=24:00:00
#$ -l mem_per_core=4G
#$ -pe omp 4
#$ -j y
#$ -m bea
#$ -o /projectnb/cs440/students/faker/train_log.log

module load java/21

cd /projectnb/cs440/students/faker

mkdir -p params

javac -cp "./lib/*:." @risk.srcs

java -cp "./lib/*:./src:." edu.bu.pas.risk.SequentialTrain pas.risk.agent.RiskQAgent pas.risk.agent.RiskQAgent random -x 500 -t 5 -v 10 -u 50 -n 1.0E-4 -d adam -g 0.99 -o /projectnb/cs440/students/faker/params/qFunction 2>/dev/null | tee /projectnb/cs440/students/faker/my_logfile.log
