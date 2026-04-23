#!/bin/bash -l
#$ -P cs440
#$ -l h_rt=72:00:00
#$ -l mem_per_core=4G
#$ -pe omp 6
#$ -j y
#$ -m bea
#$ -o /projectnb/cs440/students/jason01/train_log.log

module load java/21

cd /projectnb/cs440/students/jason01

mkdir -p params

javac -cp "./lib/*:." @risk.srcs

java -cp "./lib/*:./src:." edu.bu.pas.risk.SequentialTrain pas.risk.agent.RiskQAgent pas.risk.agent.RiskQAgent random pas.risk.agent.RiskQAgent edu.bu.pas.risk.agent.ff.TurtleAgent edu.bu.pas.risk.agent.ff.AggroAgent -x 300 -t 5 -v 15 -u 50 -n 1.0E-4 -d adam -g 0.99 -i /projectnb/cs440/students/jason01/params/qFunction278.model --outOffset 279 -o /projectnb/cs440/students/jason01/params/qFunction 2>/dev/null | tee /projectnb/cs440/students/jason01/my_logfile.log