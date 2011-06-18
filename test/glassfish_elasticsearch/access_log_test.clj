(ns glassfish-elasticsearch.access-log-test
  (:use [glassfish-elasticsearch.access-log] :reload-all)
  (:use [clojure.test]))

(deftest test-number-parser
  (is (nil? (first (number-parser {:remainder "-"}))))
  (is 0 (first (number-parser {:remainder "0"})))
  (is 1 (first (number-parser {:remainder "1"})))
  (is 9 (first (number-parser {:remainder "9"})))
  (is 10 (first (number-parser {:remainder "10"}))))