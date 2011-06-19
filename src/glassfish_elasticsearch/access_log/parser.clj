(ns glassfish-elasticsearch.access-log.parser
  (:use [glassfish-elasticsearch.util])
  (:use [name.choi.joshua.fnparse])
  (:require [clj-time.core :as time])
  (:use [clj-time.format]))

(def space-lit (lit \space))
(def quote-lit (lit \"))
(def hyphen-lit (lit \-))
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

(defstruct request-line :method :path :protocol)

(def make-request-line (partial struct request-line))

(def request-line-parser
  (complex [_ quote-lit
            method anything-but-space+
            _ space-lit
            path anything-but-space+
            _ space-lit
            protocol anything-but-quote+
            _ quote-lit]
    (make-request-line method path protocol)))

(def part-parser
  (alt
    (constant-semantics hyphen-lit nil)
    anything-but-space+))

(def number-parser
  (alt
    (constant-semantics hyphen-lit nil)
    (semantics (rep+ (lit-alt-seq "0123456789")) #(Integer/parseInt (apply-str %)))))

; Apache Common Log Format (CLF) http://httpd.apache.org/docs/2.0/logs.html
(defstruct clf-entry :host :ident :user :date-time :request-line :response-code :response-size)

(def make-clf-entry (partial struct clf-entry))

(def clf-parser
  ^{:doc "A parser for Apache Common Log Format (CLF) log entries.

  It parses a single log entry (one line). An example log entry is:

  127.0.0.1 - frank [10/Oct/2000:13:55:36 -0700] \"GET /apache_pb.gif HTTP/1.0\" 200 2326

  The log entry is parsed into a clf-entry struct."}
  (complex [host part-parser
            _ space-lit
            ident part-parser
            _ space-lit
            user part-parser
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
