(ns vinzi.jsonEdit
  (:require [clojure [pprint :as pprint]])
  (:require [clojure.data [json :as json]])
  (:use [clojure.java [io :only [reader]]])
  (:require (clojure [zip :as zip]))
  (:use [vinzi.jsonZip :only [jsonZipper nodeChildrenHtml nodeContentsHtml jsonPathStr isZipper? isJson? jsonStatus jsonKey]])
  (:import (java.awt Color Dimension)
	   (java.awt.event ActionListener)
	   (javax.swing JFrame JButton JPanel JLabel JOptionPane BorderFactory)
	   (java.awt GridBagLayout Insets)))


(defn inspect-ns [prefix]
  (filter #(.startsWith (.toString (second %)) (str "class " prefix)) (ns-imports *ns*)))

;;;;;;;;;;;;;;;
;; Macro's by Stuart Sierra

(defmacro on-action [component event & body]
  `(. ~component addActionListener
      (proxy [java.awt.event.ActionListener] []
        (actionPerformed [~event] ~@body))))

(defmacro set-grid! [constraints field value]
  `(set! (. ~constraints ~(symbol (name field)))
         ~(if (keyword? value)
            `(. java.awt.GridBagConstraints
                ~(symbol (name value)))
            value)))

(defmacro grid-bag-layout [container & body]
  (let [c (gensym "c")
        cntr (gensym "cntr")]
    `(let [~c (new java.awt.GridBagConstraints)
           ~cntr ~container]
       ~@(loop [result '() body body]
           (if (empty? body)
             (reverse result)
             (let [expr (first body)]
               (if (keyword? expr)
                 (recur (cons `(set-grid! ~c ~expr
                                          ~(second body))
                              result)
                        (next (next body)))
                 (recur (cons `(.add ~cntr ~expr ~c)
                              result)
                        (next body)))))))))
;;;;;;   end macro's by Stuart Sierra


(def jsonWidth 400)


(defn showDialog [message]
  (JOptionPane/showMessageDialog
    nil   message   "Info"
    JOptionPane/INFORMATION_MESSAGE))

(defn traverseStat
  "Traverse the 'zipper' zip until you find an element with a status. Using 'step' to go to the next element"
  [zipZap step]
  (if-let [init (step zipZap)]
    (loop [zipper init]
      (println "current node has key " (jsonKey zipper) " and status: " (jsonStatus zipper))
      (if-let [stat (jsonStatus zipper)]
	zipper  ;; node has a status-flag, so return it
	(if-let [nxt (step zipper)]
	  (if (zip/end? nxt)
	    zipper
	    (recur nxt))
	  zipper)))
    zipZap))

(defn nextStat
  "Find next element with a status-flag (skipping current element)"
  [zip]
  (traverseStat zip zip/next))

(defn prevStat
  "Find previous element with a status-flag (skipping current element)"
  [zip]
  (traverseStat zip zip/prev))

(defn jsonZipViewer
  "The argument should be a in-memory Json-object. Internally it will be transformed to a zipper."
  [jsonZip]
  {:pre [(isZipper? jsonZip) (not (nil? jsonZip))]}
  (let [loc    (atom (nextStat jsonZip))
	down   (JButton. "Down")
	root   (JButton. "root")
	up     (JButton. "Up")
	left     (JButton. "Left")
	right     (JButton. "Right")
	path   (doto (JLabel. (jsonPathStr @loc) )
		 (.setBorder (. BorderFactory createLineBorder (Color/black)))
		   (.setPreferredSize (Dimension. jsonWidth 20)))
	contents (doto (JLabel. (nodeContentsHtml @loc))
		   (.setBorder (. BorderFactory createLineBorder (Color/black)))
		   (.setPreferredSize (Dimension. jsonWidth 200)))
	children (doto (JLabel. (nodeChildrenHtml @loc))
		   (.setBorder (. BorderFactory createLineBorder (Color/black)))
		   (.setPreferredSize (Dimension. jsonWidth 150)))
	layout (doto (JPanel. (GridBagLayout.))
		 (grid-bag-layout
		  :fill :NONE, :insets (Insets. 5 5 5 5), :weightx 0.5, :weighty 0.5,
		  :gridx 0, :gridy 0, :gridwidth 5,  path
		            :gridy 1,                contents
		            :gridy 2,

			    children
		  :gridx 2, :gridy 3, :gridwidth 1,  up
		  :gridx 1, :gridy 4,                left
		  :gridx 2,                          root
		  :gridx 3,                          right
		  :gridx 2, :gridy 5,                down
		  ))
	updateFrame (fn []
;		      (println "updating frame for location ")
;		      (clojure.pprint/pprint (zip/node @loc))
;		      (flush)
		      (.setText path (jsonPathStr @loc))
		      (.setText contents (nodeContentsHtml @loc))
		      (.setText children (nodeChildrenHtml @loc))
		      )
	moveFunc  (fn [stepFunc]
		    (fn []  (when-let [nextLoc (stepFunc @loc)]
			      (when (zip/node nextLoc)  ;; empty childList might produce nil
				(swap! loc stepFunc)
				(updateFrame)))))
	moveLeft (moveFunc zip/left)
;;	moveRight (fn [] (showDialog "<html>Moving to the <br/> <font color=blue>right</font></html>"))
	moveRight (moveFunc zip/right)
	moveUp    (moveFunc zip/up)
	moveDown    (moveFunc zip/down)
	moveRoot (fn []  (while (zip/up @loc)
			   (swap! loc zip/up))
			   (updateFrame))
	frame  (doto (JFrame. "JSON viewer")
		   (.setContentPane layout)
		   (.pack)
		   (.setVisible true))
	]
    (println "JSON-viewer launched")
    ;; attach the five action-buttons
    (on-action left evnt  ;;  not-used
		  (moveLeft))
    (on-action right evnt  ;;  not-used
		  (moveRight))
    (on-action up evnt  ;;  not-used
     		  (moveUp))
    (on-action down evnt  ;;  not-used
     		  (moveDown))
    (on-action root evnt  ;;  not-used
     		  (moveRoot))
    {:layout layout
     :frame  frame
     :down   down
     :up     up
     :path   path
     :contents contents
     ; :left   left
     ; :right  right
     }
    ))


(defn jsonViewer
  "The argument should be a in-memory Json-object. Internally it will be transformed to a zipper."
  [json]
  {:pre [(not (nil? json))  (not (isJson? json))]}  ;; not in string representation  
    (jsonZipViewer (jsonZipper json)))

(defn jsonFileViewer
  "Opens the file 'name' and shows the json-contents in the viewer."
  [name]
  (with-open [r  (reader name)]
    (let [json (json/read-json r)]
      (jsonViewer json))))


(comment
;;  temporary for testing purposes

(def testTree {:level_0	{:level_0_1  "de titel"
			 :level_0_2   []
			 :level_0_3  ["text"]}
	       :level_1  {:level_1_1 3
		       :level_1_2 5}
	       :level_2  "de file"})


(defn testgb []
  (let [gbpanel (doto (JPanel. (GridBagLayout.))
		  (grid-bag-layout
		   :fill :BOTH, :insets (Insets. 5 5 5 5)
		   :gridx 0, :gridy 0
		   (JButton. "One")
		   :gridy 1
		   (JButton. "Two")
		   :gridx 1, :gridy 0, :gridheight 2
		   (JButton. "Three")))
	gbframe  (doto (JFrame. "GridBagLayout Test")
		   (.setContentPane gbpanel)
		   (.pack)
		   (.setVisible true))]
    (println "test of gridbag launched")))


; (import 'javax.swing.JOptionPane)

(defn say-hello []
  (JOptionPane/showMessageDialog
    nil "Hello, World!" "Greeting"
    JOptionPane/INFORMATION_MESSAGE))

;(import 'java.awt.event.ActionListener)  
(def act (proxy [ActionListener] []
           (actionPerformed [event] (say-hello))))

(defn testit []
     (let [frame (doto (JFrame. "Hello Frame")
		   (.setSize 200 200)
		   (.setVisible true))
	   panel (JPanel.)
	   _ (.setContentPane frame panel)
	   button (JButton. "Click Me!!!")
	   panel  (.add panel button)]
       (.revalidate button)
       ;; replaced by Macro
       ;; (.addActionListener button act)
       (on-action button evnt  ;;  not-used
		  (say-hello))
       (println "created button")))

)

