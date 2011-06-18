#!/bin/sh

BASE_URI=$1
APP_NAME=$2
LOG_FILE=$3

if [ -z $BASE_URI ]; then
    echo "Base URI expected."
    exit 1
fi
if [ -z $APP_NAME ]; then
    echo "App name expected."
    exit 1
fi
if [ -z $LOG_FILE ]; then
    echo "Log file expected."
    exit 1
fi

java -cp glassfish-elasticsearch-0.1-SNAPSHOT-standalone.jar clojure.main @glassfish_elasticsearch/access-log.clj $BASE_URI $APP_NAME $LOG_FILE
