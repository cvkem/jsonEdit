(defproject jsonViewer "1.0.0-SNAPSHOT"
  :description ""
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]                 
                 [postgresql/postgresql "9.0-801.jdbc4"]
		 [org.clojars.brenton/google-diff-match-patch "0.1"]
		 [difform "1.1.1"]]
  :dev-dependencies [[org.clojars.gjahad/debug-repl "0.3.1"]]
  ;;  (jar is copied to lib)
  ;; :dev-resources-path	"/home/cees/.m2/repository/org/clojars/gjahad/debug-repl/0.3.1/debug-repl-0.3.1.jar"
  )
