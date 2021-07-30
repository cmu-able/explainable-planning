#!/bin/bash
# Parse command line arguments - origin code from https://stackoverflow.com/a/14203146 - please look there for details
# Usage: Usage demo-space-separated.sh -e conf -s /etc -l /usr/lib /etc/hosts

RUNNER=""
PROPS="--props data/docker-globals.properties"

if [ "$1" = "dart" ]; then
  RUNNER="examples.dart.demo.DartXPlanner"
  PROPS="${PROPS} --props data/dart/docker-problem-spec.properties"
  DIR="data/dart/missions/"
elif [ "$1" = "mobilerobot" ]; then
  RUNNER="examples.mobilerobot.demo.MobileRobotXPlanner"
  PROPS="${PROPS} --props data/mobilerobot/docker-problem-spec.properties"
  DIR="data/mobilerobot/missions/"
  
elif [ "$1" = "clinic" ]; then
  RUNNER="examples.clinicscheduling.demo.ClinicSchedulingXPlanner"
  PROPS="${PROPS} --props data/clinicscheduling/docker-problem-spec.properties"
  DIR="data/clinicscheduling/missions/"
  
else
  echo "Must specify [dart | mobilerobot | clinic] as the first argument"
  exit 1
fi

MISSION="$2"

if [ "" = "${MISSION}" ]; then
  echo "Must specify a mission file as the first argument"
  echo "One of:"
  for file in ${DIR}/*; do
    echo "  "`basename $file`
  done
  exit 1
fi

LD_LIBRARY_PATH=/xplanning/lib/:/gurobi9.1.0_linux64/gurobi910/linux64/lib/ java -cp target/classes:lib/* ${RUNNER} ${PROPS} ${MISSION}