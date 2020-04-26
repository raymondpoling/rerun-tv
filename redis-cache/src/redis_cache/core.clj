(ns redis-cache.core
  (:require [clojure.core.cache :as cache]
            [taoensso.carmine :as car :refer [wcar]]
            [clj-http.client :as client]
            [clojure.tools.logging :as logger]
            [clojure.core.cache.wrapped :as c]))

(def ^:dynamic server-conn1 (if-let [redis_url (System/getenv "REDIS_URI")]
                              {:pool {}
                               :spec {:uri redis_url}}
                              nil))

(cache/defcache RedisCache [conn]
  cache/CacheProtocol
  (cache/lookup [self e]
                  (car/wcar conn (car/get e)))
  (cache/lookup [self e not-found]
                (try
                  (let [lookup (car/wcar conn (car/get e))]
                    (logger/debug "lookup is" lookup "value" lookup)
                    (or lookup not-found))
                  (catch Exception e
                    (logger/error (.getMessage e)))))
  (cache/has? [self e]
              (seq? (seq (car/wcar conn (car/get e)))))
  (cache/hit [self e]
             (logger/debug "hit?" e)
             self)
  (cache/miss [self e ret]
              (logger/debug "miss is " ret)
              (when (= (:status ret) "ok")
                (wcar conn (car/set e ret)))
              self)
  (cache/evict [self e]
               (logger/debug "Evicting" e)
               (wcar conn (car/del e))
               self)
  (cache/seed [self seed] (RedisCache. seed))
  Object
  (toString [_] "redis-cache"))

(defn make-cache [& {:keys [conn] :or {conn nil}}]
  (if (or server-conn1 conn)
           (RedisCache. (or server-conn1 conn))
           (cache/lru-cache-factory {})))

(def ^:dynamic internal-cache (atom (make-cache)))

;; (defn make-redis-cache [conn]
;;   (swap! internal-cache (fn [_] (RedisCache. conn))))

(defn get-url [host path]
  (let [url (format "http://%s%s" host path)]
    (logger/debug "Looking up site" url)
    (let [result (:body (client/get url {:as :json}))]
      (logger/debug "for url" url "got" (type result))
      result)))

(defn format-key [host path]
  (format "%s:%s" host path))

(defn redis-cache [host path]
  (let [key (format-key host path)]
    (logger/debug "looking up " key "for host" host)
    (c/lookup-or-miss internal-cache
                      key
                      (fn [_] (get-url host path)))))

(defn evict [host path]
   (c/evict internal-cache (format-key host path)))
