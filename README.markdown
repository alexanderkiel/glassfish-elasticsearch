# glassfish-elasticsearch

An Agent which transfers GlassFish log files such as server.log into
elasticsearch.

This project is really in its beginnings. Please contact me if you have
questions.

## Access Log Indexer

Part of this project is an Access Log Indexer which can index access logs
produced by Glassfish or other compatible servers in elasticsearch.

The currently supported format is the [Apache Common Log Format (CLF)][1].
Glassfish produces access logs in this format by default.

Every log entry (one line of the access log) is indexed as one document in
elasticsearch. The Access Log Indexer creates a custom [mapping][2] for such
documents to get the types of the various document fields right.

### The Access Log Mapping

The mapping looks like this:

    {
      "clf" : {
        "properties" : {
          "response-code" : {
            "type" : "integer"
          },
          "app-name" : {
            "index" : "not_analyzed",
            "type" : "string"
          },
          "host" : {
            "type" : "ip"
          },
          "ident" : {
            "index" : "not_analyzed",
            "type" : "string"
          },
          "date-time" : {
            "format" : "date_time",
            "type" : "date"
          },
          "user" : {
            "index" : "not_analyzed",
            "type" : "string"
          },
          "request-line" : {
            "properties" : {
              "protocol" : {
                "index" : "not_analyzed",
                "type" : "string"
              },
              "path" : {
                "index" : "not_analyzed",
                "type" : "string"
              },
              "method" : {
                "index" : "not_analyzed",
                "type" : "string"
              }
            }
          },
          "response-size" : {
            "type" : "integer"
          }
        }
      }
    }

All string typed fields are set to `non_analyzed` because there is no full text
like field. Without setting the `request-line.path` to `non_analyzed`, a term
facet would report the individual path segments and not the whole path.

### Index Access Logs

At the current state of the project, you can do the following:

* build the project with _lein uberjar_ and
* call _sh index-access-log.sh_ which will tell you which arguments are required.

Note: You will need at least lein 1.5.2.

[1]: <http://httpd.apache.org/docs/2.0/logs.html>
[2]: <http://www.elasticsearch.org/guide/reference/mapping/>