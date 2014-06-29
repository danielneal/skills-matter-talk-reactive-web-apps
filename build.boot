#!/usr/bin/env boot

#tailrecursion.boot.core/version "2.5.0"

(set-env!
  :project      'skills-matter-talk
  :version      "0.1.0-SNAPSHOT"
  :dependencies '[[tailrecursion/boot.task   "2.2.1"]
                  [tailrecursion/hoplon      "5.10.6"]
                  [org.clojure/clojurescript "0.0-2234"]]
  :out-path     "resources/public"
  :src-paths    #{"src"})

;; Static resources (css, images, etc.):
(add-sync! (get-env :out-path) #{"assets"})

(require '[tailrecursion.hoplon.boot :refer :all])

(deftask development
  "Build skills-matter-talk for development."
  []
  (comp (watch) (hoplon {:prerender false :pretty-print true :source-map true})))

(deftask production
  "Build skills-matter-talk for production."
  []
  (hoplon {:optimizations :advanced}))
