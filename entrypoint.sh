#!/bin/sh

set -eu

if [ ! -f "$JAR_FILE" ]; then
  echo "JAR_FILE ( $JAR_FILE ) does not exist";
  exit 1
fi

echo Pinery: $PINERY_URL
echo Report: $REPORT_NAME
echo File: $FILE_NAME

echo Report-specific parameters: $@
cd /app
exec java -jar $JAR_FILE -s "$PINERY_URL" -r "$REPORT_NAME" -o /output/"$FILE_NAME" $@
