(defproject omdb-meta "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [compojure "1.6.1"]
                 [ring/ring-defaults "0.3.2"]
                 [ring/ring-json "0.5.0"]
                 [diehard "0.9.1"]
                 [clj-http "3.10.0"]
                 [org.clojure/tools.logging "0.6.0"]
                 [common-lib "0.1.1"]
                 [ch.qos.logback/logback-classic "1.2.3"]
                 [http-kit "2.1.16"]]
  :plugins [[lein-ring "0.12.5"]]
  :ring {:handler omdb-meta.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.2"]
                        [clj-http-fake "1.0.3"]]}}
  :jvm-opts ["-Dclojure.tools.logging.factory=clojure.tools.logging.impl/slf4j-factory"]
  :main omdb-meta.handler
  :aot [omdb-meta.handler])
