# glassfish-elasticsearch

An Agent which transfers GlassFish log files such as server.log into elasticsearch.

This project is really in its beginnings. Please contact me if you have questions.

## Access Log Indexer

Part of this project is an Access Log Indexer which can index access logs
produced by Glassfish or other compatible servers in elasticsearch.

The currently supported format is the [Apache Common Log Format (CLF)][1].
Glassfish produces access log in this format by default.

[1]: <http://httpd.apache.org/docs/2.0/logs.html>