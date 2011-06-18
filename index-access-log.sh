#!/bin/sh

BASE_URI=$1
APP_NAME=$2
LOG_FILE=$3

if [ -z $BASE_URI ]; then
    echo "Base URI expected."
    echo "Please specify the base URI of your elasticsearch server including the"
    echo "index name. The index will be created with default settings if it"
    echo "doesn't exist."
    echo "Example: http://localhost:9200/my-index"
    exit 1
fi
if [ -z $APP_NAME ]; then
    echo "App name expected."
    echo "Please specify the name of your site or application. It will be recorded"
    echo "in every log entry."
    exit 1
fi
if [ -z $LOG_FILE ]; then
    echo "Log file expected."
    echo "Please specify the access log file to index."
    exit 1
fi

java -cp glassfish-elasticsearch-0.1-SNAPSHOT-standalone.jar clojure.main @glassfish_elasticsearch/access_log.clj $BASE_URI $APP_NAME $LOG_FILE
