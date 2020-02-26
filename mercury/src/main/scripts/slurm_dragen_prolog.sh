#!/bin/sh
#
# If on dragen partition, then check dragen status and drain if necessary
#
if [ "$SLURM_JOB_PARTITION" = "dragen" ]
then
  echo "Checking Dragen Status via /opt/edico/bin/dragen_reset -c"
  $(/opt/edico/bin/dragen_reset -c)
  retVal=$?
  echo "Status resulted in $retVal"
  if [ "$retVal" -eq 0 ]; then
    echo "No Reset needed. Continuing with job."
    exit 0
  else
    echo "dragen requires a reset: $SLURM_JOB_NODELIST"
    $(echo "dragen requires a reset: $SLURM_JOB_NODELIST" |   mail -s 'dragen failure' jowalsh@broadinstitute.org)
    #$(sudo sosreport --batch --tmp-dir /staging/tmp)
    $(/opt/edico/bin/dragen_reset)
    exit 1
  fi
fi
exit 0