(ns claytn.busboy
  (:require [clojure.string :as s]
            [clojure.data.json :as json]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.middleware.json-logger :refer [wrap-json-logger]]
            [ring.util.response :refer [response bad-request created]])
  (:use claytn.db))

(def ^:dynamic db-file "db.edn")

(defn debug [& args]
  (println "DEBUG: " args)
  args)

(defn find-by
  "Helper because I was too lazy to find the real clojure
   function that returns a goddamn element based on a
   predicate."
  [pred coll]
  (if (empty? coll)
    nil
    (let [curr (first coll)]
      (if (pred curr)
        curr
        (recur pred (rest coll))))))

(defn update-values-if
  "Given a collection, updates individual values in that collection
   if the existing value passes the provided predicate."
  [pred f coll]
  (mapv
    (fn [x]
      (if (pred x) (f x) x))
    coll))

(defn parse-unsigned-int
  "Parses string as unsigned integer. Returns nil if unsuccessful."
  [x]
  (try
    (Integer/parseUnsignedInt x)
    (catch NumberFormatException e
      nil)))

(defn as-shallow-route
  "Takes a route handler and only allows single level routes and routes with integer identifiers
  like /posts/1 to pass through. Multi-level subroutes are not supported yet (ex. /posts/user/2)"
  [route-handler]
  (fn [root sub-paths body]
    (if (> (count sub-paths) 1)
      (bad-request {})
      (let [[identifier] sub-paths
            parsed-identifier (parse-unsigned-int identifier)]
        (if (and (not (nil? identifier))
                 (not (int? parsed-identifier)))
          (bad-request {})
          (route-handler root parsed-identifier body))))))

(def get-handler
  (as-shallow-route
    (fn [root identifier _]
      (let [db (get-db db-file)]
        (if (nil? identifier)
          (response (get db root))
          (let [data (find-by #(= (:id %) identifier) (get db root))]
            (debug {:data data :identifier-type (type identifier) :identifier identifier})
            (if (nil? data)
              (bad-request {})
              (response data))))))))

(def post-handler
  (as-shallow-route
    (fn [root identifier body]
      (if (not (nil? identifier))
        (bad-request {})
        (let [db (get-db db-file)
              current-data (get db root)
              new-entry (if (map? current-data)
                          body
                          (merge body {:id (inc (count current-data))}))
              updated-db (if (map? current-data)
                           (update db root (constantly new-entry))
                           (update db root #(conj % new-entry)))]
          (update-db db-file updated-db)
          (created (str "/" (name root)) new-entry))))))

(def put-handler
  (as-shallow-route
    (fn [root identifier body]
      (if (nil? identifier)
        (bad-request {})
        (let [db (get-db db-file)
              current-data (get db root)
              updated-data (update-values-if
                             #(= (:id %) identifier)
                             #(merge % body {:id identifier})
                             current-data)
              updated-db (update db root (constantly updated-data))]
          (update-db db-file updated-db)
          (response updated-data))))))

(def delete-handler
  (as-shallow-route
    (fn [root identifier body]
      (if (nil? identifier)
        (bad-request {})
        (let [db (get-db db-file)
              current-data (get db root)
              updated-data (filterv #(not= (:id %) identifier) current-data)
              updated-db (update db root (constantly updated-data))]
          (update-db db-file updated-db)
          (response {}))))))


(def route-handlers {:get    get-handler
                     :post   post-handler
                     :put    put-handler
                     :delete delete-handler})

(defn handler [request]
  (let [uri-components (-> (request :uri)
                           (s/split #"/")
                           (#(filter (comp not empty?) %)))
        root (-> uri-components
                 first
                 keyword)
        sub-paths (rest uri-components)
        body (request :body)]
    (if (nil? (-> db-file get-db root))
      (bad-request {})
      (let [route-handler (route-handlers (request :request-method))
            resp (route-handler root sub-paths body)]
        resp))))

(def app (-> handler
             (wrap-json-body {:keywords? true})
             wrap-json-response
             wrap-json-logger))

(def app-with-reload
  (wrap-reload #'app))

(defn -main [& args]
  (let [[database-file] args]
    (if (nil? database-file)
      (println "Usage: clj -m claytn.busboy <filename>.edn")
      (binding [db-file database-file]
        (run-jetty #'app-with-reload {:port 3000})))))