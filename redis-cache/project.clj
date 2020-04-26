(defproject redis-cache "0.0.1-SNAPSHOT"
  :description "Standard caching for remote calls"
  :url "https://github.com/raymondpoling/rerun-tv"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[clj-http "3.10.0"]
                 [com.taoensso/carmine "2.19.1"]
                 [org.clojure/core.cache "1.0.207"]
                 [org.clojure/tools.logging "0.6.0"]
                 [ch.qos.logback/logback-classic "1.2.3"]
                 [cheshire "5.10.0"]]
  :plugins [[lein-cloverage "1.0.13"]
            [lein-shell "0.5.0"]
            [lein-ancient "0.6.15"]
            [lein-changelog "0.3.2"]]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.10.0"]
                                  [clj-http-fake "1.0.3"]]}}
  :test-paths ["test" "itest"]
  :deploy-repositories [["releases" :clojars]]
  :aliases {"update-readme-version"
            ["shell" "sed" "-i" "s/\\\\[redis-cache \"[0-9.]*\"\\\\]/[redis-cache \"${:version}\"]/" "README.md"]
            "itest" ["test" ":integration"]
            "utest" ["test" ":default"]}
  :test-selectors {:default (complement :integration)
                   :integration :integration}
  :release-tasks [["shell" "git" "diff" "--exit-code"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["changelog" "release"]
                  ["update-readme-version"]
                  ["vcs" "commit"]
                  ["vcs" "tag"]
                  ["deploy"]
                  ["vcs" "push"]])
