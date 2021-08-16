#!/bin/bash
# Parse command line arguments - origin code from https://stackoverflow.com/a/14203146 - please look there for details
# Usage: Usage run-demo [-l|--line-length <line length>] (dart | mobilerobot | clinic) mission_file
DEBUG=""
POSITIONAL=()
while [[ $# -gt 0 ]]; do
  key="$1"
  
  case $key in
    -l|--line-length)
    	LINELENGTH="$2"
    	shift;
    	shift;
    	;;
    -d|--debug)
    	DEBUG="-agentlib:jdwp=transport=dt_socket,server=y,address=$2"
    	shift;
    	shift;
    	;;
    *)
    	POSITIONAL+=("$1")
    	shift
    	;;
  esac
done

set -- "${POSITIONAL[@]}"

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
elif [ "$1" != "" ]; then
  $1
  exit 0
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

ARGS=""

if [ "$LINELENGTH" != "" ]; then
  ARGS="--linelength ${LINELENGTH}"
fi

LD_LIBRARY_PATH=/xplanning/lib/:/gurobi9.1.0_linux64/gurobi910/linux64/lib/ java ${DEBUG} -Dfile.encoding=UTF-8 -cp target/classes:lib/* ${RUNNER} ${ARGS} ${PROPS} ${MISSION}