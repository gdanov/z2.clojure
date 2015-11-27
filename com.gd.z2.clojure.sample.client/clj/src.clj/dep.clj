(ns dep
		(:require [clojure.pprint :as pp])
		(:import [java.util.concurrent Callable]))

(println "me tooooooooooo")

(defn test [] (println "test"))

(defn do-something []
			"this is the result")

(defprotocol IProto
						 (do-one [this]))

(defrecord Recort [a-field]
					 IProto
					 (do-one [_] "just did it")
					 Callable
					 (call [_] "called"))

(defn xchange-objects []
			(println "## xchange ##")
			(println (.. Thread (currentThread) (getContextClassLoader) (toString)))
			(proxy [Callable] []
						 (call []
									 ;(fn [] true)
									 (->Recort "something"))))