#!/bin/bash

JURISDICTION=$1
TOTAL_CASES=$2

function usage() {
  echo "This script updates last state modified date of existing cases for a given jurisdiction."
  echo "Please provide total number of cases in the jurisdiction with optional batch size"
  echo "Usage: lastStateModifiedMigration.sh <JURISDICTION> <TOTAL_CASES> <BATCH_SIZE>"
  echo "Example: lastStateModifiedMigration.sh CMC 10000 2000"
  exit 0
}

if [[ -z ${JURISDICTION} ]] || [[ -z ${TOTAL_CASES} ]]  ; then
  usage
  exit 1
fi

USER_TOKEN='Replace with idam user token'
SERVICE_TOKEN='Replace with S2S token'

BATCH_SIZE=${3:-5000}
HOST="http://ccd-data-store-api-prod.service.core-compute-prod.internal"
#HOST="http://localhost:4452"

NO_OF_ITERATIONS=$(( TOTAL_CASES / BATCH_SIZE ))
counter=0
while [[ counter -lt ${NO_OF_ITERATIONS} ]]
do
  curl -X POST \
  "$HOST/last-state-modified/migrate?dryRun=true&jurisdiction=$JURISDICTION&batchSize=$BATCH_SIZE" \
  -H "content-type: application/json" \
  -H "authorization: $USER_TOKEN" \
  -H "serviceauthorization: $SERVICE_TOKEN" -d {}

  printf "\n Invoking lastStateModified endpoint with jurisdiction:%s, batchSize:%s\n" ${JURISDICTION} ${BATCH_SIZE}

  counter=`expr $counter + 1`
done

printf "Completed migration for jurisdiction:%s" ${JURISDICTION}
