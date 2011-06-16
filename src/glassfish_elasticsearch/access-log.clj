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
    (str (parse-date-time (apply-str date-time)))))

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

(defn- index-line [line]
  (first (clf-parser {:remainder line})))

(defn index-log-entry [log-entry]
  (let [base-uri (first *command-line-args*)
        body (json-str log-entry)
        id (md5-sum body)]
    (if base-uri
      (try
        (client/put (str base-uri "/glassfish-access-log-prod/test/" id) {:body body})
        (catch java.lang.Exception e (println "Exception: " (.getMessage e)))))))

(defn- index-log-file [reader]
  "Indexes the contents of a log file. This function requires an open reader of the log file."
  (let [lines (line-seq reader)]
    (doall (map println (map index-log-entry (map index-line lines))))))

(when *command-line-args*
  (with-open [reader (reader (second *command-line-args*))]
    (index-log-file reader)))