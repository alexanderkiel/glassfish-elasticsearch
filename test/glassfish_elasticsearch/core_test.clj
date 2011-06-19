(ns glassfish-elasticsearch.core-test
  (:use [glassfish-elasticsearch.core] :reload-all)
  (:use [name.choi.joshua.fnparse])
  (:use [clj-time.core :only (date-time)])
  (:use [clojure.test]))

(def example "[#|2009-08-06T13:41:09.192-0700|INFO|glassfish|javax.enterprise.system.core.security.com.sun.enterprise.security|_ThreadID=20;_ThreadName=Thread-1;|SEC1002: Security Manager is OFF.|#]")

(deftest parse-log-entry
  (let [log-entry (first (log-entry-parser {:remainder example}))]
    (is (= (date-time 2009 8 6 20 41 9 192) (:date-time log-entry)))
    (is (= :info (:level log-entry)))
    (is (= "glassfish" (:product-id log-entry)))
    (is (= "javax.enterprise.system.core.security.com.sun.enterprise.security" (:logger-name log-entry)))
    (is (= {:name "_ThreadID" :value "20"} (first (:name-value-pairs log-entry))))
    (is (= {:name "_ThreadName" :value "Thread-1"} (second (:name-value-pairs log-entry))))
    (is (= "SEC1002: Security Manager is OFF." (:message log-entry)))))
