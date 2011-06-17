(ns glassfish_elasticsearch.access-log
  (:use [glassfish_elasticsearch.util])
  (:use [glassfish_elasticsearch.md5])
  (:use [clojure.java.io])
  (:use [name.choi.joshua.fnparse])
  (:require [clj-time.core :as time])
  (:use [clj-time.format])
  (:require [clojure.string :as string])
  (:use [clojure.contrib.json])
  (:require [clj-http.client :as client]))

; Apache Common Log Format (CLF) http://httpd.apache.org/docs/2.0/logs.html
(defstruct clf-entry :host :ident :user :date-time :request-line :response-code :response-size)

(defstruct request-line :method :path :protocol)

(def make-clf-entry (partial struct clf-entry))

(def make-request-line (partial struct request-line))

(def space-lit (lit \space))
(def quote-lit (lit \"))
(def open-bracket-lit (lit \[))
(def close-bracket-lit (lit \]))

(def anything-but-space+
  (semantics (rep+ (except anything space-lit)) apply-str))

(def anything-but-quote+
  (semantics (rep+ (except anything quote-lit)) apply-str))

(def date-time-formatter
  (formatter "dd/MMM/yyyy:HH:mm:ss Z"))

(def parse-date-time
  (partial parse date-time-formatter))

(def date-time-parser
  (complex [_ open-bracket-lit
            date-time (rep* (except anything close-bracket-lit))
            _ close-bracket-lit]
    (parse-date-time (apply-str date-time))))

(def request-line-parser
  (complex [_ quote-lit
            method anything-but-space+
            _ space-lit
            path anything-but-space+
            _ space-lit
            protocol anything-but-quote+
            _ quote-lit]
    (make-request-line method path protocol)))

(def number-parser
  (semantics (rep+ (lit-alt-seq "0123456789")) apply-str))

(def clf-parser
  (complex [host anything-but-space+
            _ space-lit
            ident anything-but-space+
            _ space-lit
            user anything-but-space+
            _ space-lit
            date-time date-time-parser
            _ space-lit
            request-line request-line-parser
            _ space-lit
            response-code number-parser
            _ space-lit
            response-size number-parser
            _ (rep* anything)]
    (make-clf-entry host ident user date-time request-line response-code response-size)))

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

(defn- index-log-entry [log-entry base-uri type]
  (let [id (build-id (:date-time log-entry))
        uri (str base-uri "/glassfish-access-log/" type "/" id)
        body (json-str (merge log-entry {:date-time (str (:date-time log-entry))}))]
    (try
      (client/put uri {:body body})
      (catch java.lang.Exception e (println "Exception while sending the JSON to elasticsearch [" uri "]:" (.getMessage e) "body:" body)))))

(defn- index-line [base-uri type line]
  (index-log-entry (rule-match clf-parser prn prn {:remainder line}) base-uri type))

(defn- index-log-file [reader base-uri type]
  "Indexes the contents of a log file. This function requires an open reader of
   the log file and the base URI of a elasticsearch node."
  (let [lines (line-seq reader)]
    (reduce = (map (partial index-line base-uri type) lines))))

(let [base-uri (nth *command-line-args* 0)
      type (nth *command-line-args* 1)
      filename (nth *command-line-args* 2)]
  (do
    (println "Index contents of:" filename)
    (println "as type:" type)
    (println "to:" base-uri)
    (with-open [reader (reader filename)]
      (index-log-file reader base-uri type))))
