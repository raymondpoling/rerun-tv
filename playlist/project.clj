(defproject playlist "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [compojure "1.6.1"]
                 [ring/ring "1.8.0"]
                 [ring/ring-json "0.5.0"]
                 [ring/ring-defaults "0.3.2"]
                 [org.clojure/java.jdbc "0.7.11"]
                 [mysql/mysql-connector-java "8.0.19"]
                 [common-lib "0.1.2"]
                 [org.clojure/tools.logging "0.6.0"]
                 [ch.qos.logback/logback-classic "1.2.3"]
                 [http-kit "2.1.16"]]
  :plugins [[lein-ring "0.12.5"]]
  :ring {:handler playlist.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.4.0"]
                        [proto-repl "0.3.1"]
                        [com.h2database/h2 "1.4.200"]]}}
  :main playlist.handler
  :aot [playlist.handler])
