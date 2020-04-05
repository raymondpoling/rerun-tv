(defproject auth "0.1.0-SNAPSHOT"
  :description "basic authorization services"
  :url "https://github.com/raymondpoling/rerun-tv/"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [compojure "1.6.1"]
                 [ring/ring "1.8.0"]
                 [ring/ring-json "0.5.0"]
                 [ring/ring-defaults "0.3.2"]
                 [org.clojure/java.jdbc "0.7.11"]
                 [org.clojure/tools.logging "0.6.0"]
                 [ch.qos.logback/logback-classic "1.2.3"]
                 [common-lib "0.1.0"]
                 [mysql/mysql-connector-java "8.0.19"]
                 [http-kit "2.1.16"]]
  :plugins [[lein-ring "0.12.5"]
            [io.sarnowski/lein-docker "1.1.0"]
            [lein-resource "17.06.1"]]
  :ring {:handler auth.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.2"]
                        [com.h2database/h2 "1.4.200"]]}}
  :docker {:image-name "infinite-playlist/auth"
           :dockerfile "target/Dockerfile"
           :build-dir  "target"}
  :hook []
  :resource {
             :resource-paths ["src-resources"]
             :extra-values { :date ~(java.util.Date.) }
             }
  :release-tasks [["change" "version"
                   "leiningen.release/bump-version" "release"]
                  ["resource" "clean"]
                  ["clean"]
                  ["uberjar"]
                  ["resource"]
                  ["docker" "build"]
                  ["change" "version" "leiningen.release/bump-version"]]
  :main auth.handler
  :aot [auth.handler])
