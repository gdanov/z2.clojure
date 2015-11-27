(ns bootstrap-comp
  (:require [clojure.test])
  (:import [java.util.concurrent Callable]
           [java.util.logging Logger]))

(def -logger (Logger/getLogger "bootstrap-comp"))

(def classLoader (.. Thread currentThread getContextClassLoader))
(def my-name (.. classLoader getJavaComponentHandle getResourceInfo getName))

(.info -logger (str "## bootstrapping the component " my-name " ##"))

(defn lifecycle-fn? [phase fn-var]
  (and (clojure.test/function? (deref fn-var)) (= phase (:z2-lifecycle (meta fn-var)))))

(defn scan-lifecycle-hooks [state]
  (->> (all-ns)
    (map (fn [nn]
           (->> (ns-publics nn)
             (map (fn [[_ v]]
                    (if-let [m (= state (:z2-lifecycle (meta v)))]
                      @v)))
             (filter (comp not nil?)))))
    (filter seq)
    (apply conj)))

(defn initialize [ns-name]
  (when ns-name
    (.info -logger (str "asynchronously initializing component using ns [" ns-name "]"))

    ;; async on purpose - avoid the case where server sync fails due to slow init (like cider). RTs anyway cannot refer each other
    (future    
      (let [thens (symbol ns-name)
            the-lib (require thens)
            ; _ (println "########" the-lib)
            pubs (ns-publics thens)]
      
        (doall
          (map
            (fn [[k v]]
              (when (lifecycle-fn? :start v)
                (.info -logger (str "invoking init fn [" k "]"))
                (v)))
            pubs))
        (.info -logger (str "init done for [" ns-name "]"))))
    ))

(defn shutdown []
  (.info -logger (str "shutting down component"))

  (doall
    (map
      (fn [f]
        (.info -logger (str "invoking stop fn [" f "]"))
        (f))
      (scan-lifecycle-hooks :stop)))
  
  (.info -logger "shutting down agents")
  (shutdown-agents))

(defn getCommand []
			(proxy [Callable] []
						 (call []
									 "tadaa")))


