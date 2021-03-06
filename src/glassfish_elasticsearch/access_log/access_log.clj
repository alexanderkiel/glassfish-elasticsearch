(ns glassfish-elasticsearch.access-log.access-log
  (:use [glassfish-elasticsearch.access-log.parser])
  (:use [glassfish-elasticsearch.util])
  (:use [glassfish-elasticsearch.md5])
  (:use [glassfish-elasticsearch.http])
  (:use [clojure.java.io])
  (:use [name.choi.joshua.fnparse])
  (:require [clj-time.core :as time])
  (:use [clj-time.format])
  (:require [clojure.string :as string])
  (:use [clojure.contrib.json])
  (:require [clj-http.client :as client]))

(def time-id-formatter (formatter "yyyy-MM-dd'T'HH-mm-ss"))

; The id-atom holds the current date time and count value for building unique
; ids which start with the date time followed by a counter value.
(def id-atom (atom {:date-time nil :count nil}))

(defn- inc-id [current-id date-time]
  "Increments the value of the id-atom.

   Takes the current value of the atom (current-id) and the new date time value
   (date-time).

   Increments the counter value if the current date-time is equal to the new
   date-time. Otherwise sets the new date time and resets the counter value to
   zero."
  (let [current-date-time (:date-time current-id)
        current-count (:count current-id)]
    (if (= current-date-time date-time)
      {:date-time current-date-time :count (inc current-count)}
      {:date-time date-time :count 0})))

(defn- build-id [date-time]
  "Build the id of the log-entry using the supplyed date time value.

   Ids consits of the date time value formatted using the time-id-formatter and
   and additional counter value to make the ids unique. This function uses the
   id-atom to increment the counter value."
  (let [id (swap! id-atom inc-id date-time)]
    (str (unparse time-id-formatter (:date-time id)) "_" (:count id))))

(defn- put [uri body]
  (try
    (client/put uri {:body body})
    (catch java.lang.Exception e (println "Exception while sending the JSON to elasticsearch [" uri "]:" (.getMessage e) "body:" body))))

(defn- index-log-entry [log-entry base-uri app-name]
  (let [id (build-id (:date-time log-entry))
        uri (str base-uri "/clf/" id)
        body (json-str (merge log-entry {:date-time (str (:date-time log-entry))} {:app-name app-name}))]
    (put uri body)))

(defn- index-line [base-uri app-name line]
  (index-log-entry (rule-match clf-parser prn prn {:remainder line}) base-uri app-name))

(defn- create-index [base-uri]
  (when (returns-404? (str base-uri "/_status"))
    (println "Created the index with default settings.")
    (client/put base-uri)))

(defn- create-mapping [base-uri]
  (let [uri (str base-uri "/clf/_mapping")
        body (json-str {:clf {:properties {
      :app-name {:type "string" :index "not_analyzed"}
      :host {:type "ip"}
      :ident {:type "string" :index "not_analyzed"}
      :user {:type "string" :index "not_analyzed"}
      :date-time {:type "date" :format "date_time"}
      :request-line {:properties {
        :method {:type "string" :index "not_analyzed"}
        :path {:type "string" :index "not_analyzed"}
        :protocol {:type "string" :index "not_analyzed"}
        }}
      :response-code {:type "integer"}
      :response-size {:type "integer"}}}})]
    (put uri body)))

(defn- index-log-file [reader base-uri app-name]
  "Indexes the contents of a log file. This function requires an open reader of
   the log file, the base URI of a elasticsearch node and an application name."
  (let [lines (line-seq reader)]
    (do
      (create-index base-uri)
      (create-mapping base-uri)
      (reduce = (map (partial index-line base-uri app-name) lines)))))

(let [base-uri (nth *command-line-args* 0)
      app-name (nth *command-line-args* 1)
      filename (nth *command-line-args* 2)]
  (when (and base-uri app-name filename)
    (println "Index contents of:" filename)
    (println "with app-name:" app-name)
    (println "to:" base-uri)
    (with-open [reader (reader filename)]
      (index-log-file reader base-uri app-name))))
