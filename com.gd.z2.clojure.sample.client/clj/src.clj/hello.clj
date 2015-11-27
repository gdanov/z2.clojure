(ns hello
	(:require [dep])
		(:import [javax.servlet Servlet]))

(println "HELLO from $$ another $$ clojure module"
	(.. java.lang.Thread (currentThread) (getContextClassLoader)))

(dep/test)

(def wu (proxy [java.lang.Runnable] []
							 (run [] (println "hello clojure workers"))))

(com.zfabrik.work.WorkUnit/work wu)

(require 'hello-2)
(require 'hello-22)