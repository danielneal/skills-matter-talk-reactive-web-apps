#!/usr/bin/env boot

#tailrecursion.boot.core/version "2.5.0"

(set-env!
  :project      'demo
  :version      "0.1.0-SNAPSHOT"
  :dependencies '[[tailrecursion/boot.task   "2.2.1"]
                  [tailrecursion/hoplon      "5.10.7"]
                  [org.clojure/clojurescript "0.0-2234"]
                  [tailrecursion/boot.ring   "0.2.1"]
                  [com.datomic/datomic-free "0.9.4766"]
                  [datascript "0.1.5"]
                  [org.clojure/algo.generic "0.1.0"]
                  [lein-light-nrepl "0.0.18"]
                  [org.clojure/tools.nrepl "0.2.3"]]
 :out-path     "resources/public"
  :src-paths    #{"src/hl" "src/cljs" "src/clj"})

;; Static resources (css, images, etc.):
(add-sync! (get-env :out-path) #{"assets"})

(require '[tailrecursion.hoplon.boot :refer :all]
         '[tailrecursion.castra.task :as c]
         '[lighttable.nrepl.handler :as light]
         '[clojure.tools.nrepl.server :as nrepl])

(deftask repl-light
  "Launch nrepl in the project."
  []
  (let [nrepl-server (atom nil)]
    (fn [continue]
      (fn [event]
        (swap! nrepl-server
               #(or % (nrepl/start-server
                       :port 0
                       :handler (nrepl/default-handler #'light/lighttable-ops))))
        (println "nrepl running on " (:port @nrepl-server))
        (continue event)
        @(promise)))))

(deftask development
  "Build demo for development."
  []
  (comp (repl-light) (watch) (hoplon {:prerender false}) (c/castra-dev-server 'demo.api)))

(deftask production
  "Build demo for production."
  []
  (hoplon {:optimizations :advanced}))
