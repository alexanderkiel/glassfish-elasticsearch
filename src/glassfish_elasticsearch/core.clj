(ns glassfish-elasticsearch.core
  (:use [glassfish_elasticsearch.util])
  (:use [glassfish_elasticsearch.md5])
  (:use [clojure.java.io])
  (:use [name.choi.joshua.fnparse])
  (:require [clj-time.core :as time])
  (:use [clj-time.format])
  (:require [clojure.string :as string])
  (:use [clojure.contrib.json])
  (:require [clj-http.client :as client]))

(defstruct log-entry :date-time :level :product-id :logger-name :name-value-pairs :message)

(def make-log-entry
  (partial struct log-entry))

(def start-lit (lit-conc-seq "[#|"))
(def end-lit (lit-conc-seq "|#]"))
(def separator (lit \|))

(def severe-lit (constant-semantics (lit-conc-seq "SEVERE") :severe))
(def warning-lit (constant-semantics (lit-conc-seq "WARNING") :warning))
(def info-lit (constant-semantics (lit-conc-seq "INFO") :info))
(def config-lit (constant-semantics (lit-conc-seq "CONFIG") :config))
(def fine-lit (constant-semantics (lit-conc-seq "FINE") :fine))
(def finer-lit (constant-semantics (lit-conc-seq "FINER") :finer))
(def finest-lit (constant-semantics (lit-conc-seq "FINEST") :finest))

(def log-level-lit (alt severe-lit warning-lit info-lit config-lit fine-lit finer-lit finest-lit))

(def anything-except-separator+
  (semantics (rep+ (except anything separator)) apply-str))

(def date-time-formatter
  (formatters :date-time))

(def parse-date-time
  (partial parse date-time-formatter))

(def name-value-pair
  (complex [name (rep+ (except anything (lit-alt-seq "|=")))
            _ (lit \=)
            value (rep+ (except anything (lit \;)))
            _ (lit \;)]
    {:name (apply-str name) :value (apply-str value)}))

(def name-value-pair-array
  (rep+ name-value-pair))

(def log-entry-parser
  (complex [_ start-lit
            date-time anything-except-separator+
            _ separator
            log-level log-level-lit
            _ separator
            product-id anything-except-separator+
            _ separator
            logger-name anything-except-separator+
            _ separator
            name-value-pairs name-value-pair-array
            _ separator
            message anything-except-separator+
            _ end-lit]
    (make-log-entry
      (parse-date-time (apply-str date-time))
      log-level
      product-id
      logger-name
      name-value-pairs
      message)))

(def log-entry-in-file-parser
  (complex [log-entry log-entry-parser
            _ (rep* (lit \space))]
    log-entry))

(def log-file-parser (rep* log-entry-in-file-parser))

(defn log-entry-to-json [x]
  {:date-time (str (:date-time x))
   :level (subs (string/upper-case (:level x)) 1)
   :product-id (:product-id x)
   :logger-name (:logger-name x)
   :name-value-pairs (:name-value-pairs x)
   :message (:message x)})

(def time-id-formatter (formatter "yyyy-MM-dd'T'HH-mm-ss-SSS"))

(defn- thread-id-filter [map]
  (= (:name map) "_ThreadID"))

(defn index-log-entry [log-entry]
  (let [base-uri (first *command-line-args*)
        message (if (:message log-entry) (:message log-entry) "")
        id (str (unparse time-id-formatter (:date-time log-entry)) "_" (md5-sum message))
        body (json-str (log-entry-to-json log-entry))]
    (if base-uri
      (try
        (client/put (str base-uri "/glassfish-log/test/" id) {:body body})
        (catch java.lang.Exception e (println "Exception: " (.getMessage e)))))))

(defn- response-filter [response]
  (not= 201 (:status response)))

(defn index-line [line]
  (let [log-entry (first (log-entry-parser {:remainder line}))]
    (try
      (index-log-entry log-entry)
      (catch java.lang.Exception e (println (.getClass e) (.getMessage e))))))

(defn- join-partitions [partitions partition]
  (if (.startsWith ^String (first partition) "[#|")
    (conj partitions partition)
    (let [partition-to-add (first partitions)
          new-partition (conj partition (first partition-to-add))]
      (conj (rest partitions) new-partition))))

(defn index-log-file [reader]
  "Indexes the contents of a log file. This function requires an open reader of the log file."
  (let [lines (line-seq reader)
        partitions (partition-by #(.startsWith ^String % "[#|") lines)
        non-empty-partitions (filter #(or (> (count %) 1) (not (empty? (first %)))) partitions)
        joined-partitions (reduce join-partitions '() non-empty-partitions)
        final-partitions (map apply-str joined-partitions)
        index-responses (map index-line final-partitions)]
    (doall (map println (filter response-filter index-responses)))))

(when *command-line-args*
  (with-open [reader (reader (second *command-line-args*))]
    (index-log-file reader)))
