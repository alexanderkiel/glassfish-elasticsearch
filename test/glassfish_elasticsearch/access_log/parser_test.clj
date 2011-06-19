(ns glassfish-elasticsearch.access-log.parser-test
  (:use [glassfish-elasticsearch.access-log.parser] :reload-all)
  (:use [clojure.test]))

(deftest test-request-line-parser
  (is (= {:method "GET" :path "/apache_pb.gif" :protocol "HTTP/1.0"} (first (request-line-parser {:remainder "\"GET /apache_pb.gif HTTP/1.0\""})))))

(deftest test-part-parser
  (is (nil? (first (part-parser {:remainder "-"}))))
  (is (= "0" (first (part-parser {:remainder "0"}))))
  (is (= "127.0.0.1" (first (part-parser {:remainder "127.0.0.1"}))))
  (is (= ["127.0.0.1" {:remainder '(\space \-)}] (part-parser {:remainder "127.0.0.1 -"}))))

(deftest test-number-parser
  (is (nil? (first (number-parser {:remainder "-"}))))
  (is (= 0 (first (number-parser {:remainder "0"}))))
  (is (= 1 (first (number-parser {:remainder "1"}))))
  (is (= 9 (first (number-parser {:remainder "9"}))))
  (is (= 10 (first (number-parser {:remainder "10"})))))