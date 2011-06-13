(defproject glassfish-elasticsearch "0.1-SNAPSHOT"
  :description "An Agent which transfers GlassFish log files such as server.log into elasticsearch."
  :dependencies [[org.clojure/clojure "1.2.1"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [fnparse "2.2.7"]
                 [clj-time "0.3.0"]])