(ns glassfish-elasticsearch.core
  (:use [name.choi.joshua.fnparse])
  (:use [clj-time.core])
  (:use [clj-time.format])
  (:use [clojure.string])
  (:use [clojure.contrib.json]))

(defstruct log-entry :date-time :level :product-id :logger-name :name-value-pairs :message)

(def make-log-entry
  (partial struct log-entry))

(def apply-str (partial apply str))

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
            _ (rep+ (lit \newline))]
    log-entry))

(def log-file-parser (rep* log-entry-in-file-parser))

(def log-filename (first *command-line-args*))

(def log-entries
  (first (log-file-parser {:remainder (slurp log-filename)})))

(defn log-entry-to-json [x]
  {:date-time (str (:date-time x))
   :level (subs (upper-case (:level x)) 1)
   :product-id (:product-id x)
   :logger-name (:logger-name x)
   :name-value-pairs (:name-value-pairs x)
   :message (:message x)})

(pprint-json (map log-entry-to-json log-entries))
