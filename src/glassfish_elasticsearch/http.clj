(ns glassfish-elasticsearch.http
  (:require [clj-http.client :as client]))

(defn returns-404?
  "Test if a resource exists by checking for a 404 response.

   Returns true for 404 responses and false otherwise."
  [url & [req]]
  (let [resp (client/get url (merge req {:throw-exceptions false}))
        status (:status resp)]
    (do
      (if (and (>= status 400) (not= status 404))
        (throw (Exception. (str status)))
        (= status 404)))))