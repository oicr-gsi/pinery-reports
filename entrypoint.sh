#!/bin/sh

set -e

if [ ! -f "$JAR_FILE" ]; then
  echo "JAR_FILE ( $JAR_FILE ) does not exist";
  exit 1
fi

if [ -z "$PINERY_URL" ]
then
  echo "PINERY_URL must be provided in `docker run` command: -e PINERY_URL=<pinery-url>"
  exit 1
fi

if [ -z "$REPORT_NAME" ]
then
  echo "REPORT_NAME must be provided in `docker run` command: -e REPORT_NAME=<report-name>"
  exit 1
fi

GUANYIN_OPTION=''
if [ ! -z "$GUANYIN_URL" ]
then
  GUANYIN_OPTION="-g $GUANYIN_URL"
fi

FILE_NAME="${FILE_NAME:-output.csv}"

echo Pinery: $PINERY_URL
echo Report: $REPORT_NAME
echo File: $FILE_NAME
echo Guanyin option: $GUANYIN_OPTION

echo Report-specific parameters: $@
cd /app
exec java -jar $JAR_FILE -s "$PINERY_URL" -r "$REPORT_NAME" "$GUANYIN_OPTION" -o /output/"$FILE_NAME" $@
