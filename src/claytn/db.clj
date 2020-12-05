(ns claytn.db)

(defn get-db
  "Fetches and parses the edn data inside the provided db-file as raw Clojure values."
  [db-file]
  (read-string (slurp db-file)))

(defn update-db
  "Given a database file name and an edn value, updates the db file with the provided edn data"
  [db-file edn]
  (spit db-file (pr-str edn)))