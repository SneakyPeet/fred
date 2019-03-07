(ns fred.core
  (:require [fred.annotate :as image]
            [clojure.java.io :as io]
            [clojure.string :as string]))

(def document-data (read-string (slurp "images/raw/document-03-03-2019_clicks_42237_1.edn")))
(def text-data (read-string (slurp "images/raw/text-03-03-2019_clicks_42237_1.edn")))


(defn annotation-data
  [{:keys [description boundingPoly] :as annotation}]
  (let [[x1y1 x2y1 x2y2 x1y2] (:vertices boundingPoly)]
    (assoc annotation
           :top-left (:y x1y1)
           :sort-index (:x x1y1)
           :height (- (:x x2y1) (:x x1y1)))))


(defn same-row? [a1 a2]
  (let [tolerence (* (:height a1) 0.1)
        delta (Math/abs (- (:top-left a1) (:top-left a2)))]
    (< delta tolerence)))


(defn extract-rows [annotations]
  (let [annotations (sort-by :top-left annotations)
        current-annotation (first annotations)]
    (loop [rows []
           current-row [current-annotation]
           current-annotation current-annotation
           annotations (rest annotations)]
      (if (empty? annotations)
        (conj rows current-row)
        (let [next-annotation (first annotations)
              same-row? (same-row? current-annotation next-annotation)]
          (if same-row?
            (recur rows
                   (conj current-row next-annotation)
                   next-annotation
                   (rest annotations))
            (recur (conj rows current-row)
                   [next-annotation]
                   next-annotation
                   (rest annotations))))))))

(comment
  (let [file-name "03-03-2019_clicks_42237_1"
        image-path (str "images/raw/" file-name ".JPG")
        data-path (str "images/raw/document-" file-name ".edn")
        image-data (image/annotate-document image-path)
        print-length *print-length*]
    (set! *print-length* Integer/MAX_VALUE)
    (spit data-path (pr-str image-data))
    (set! *print-length* print-length))



  (->> text-data
       :data
       :responses
       first
       :textAnnotations
       rest
       (map annotation-data)
       extract-rows
       (map (fn [row] (sort-by :sort-index row)))
       (map (fn [row] (map :description row)))
       )




  (let [{:keys [description boundingPoly] :as annotation}
        {:description "B/PACK",
         :boundingPoly
         {:vertices
          [{:x 1065, :y 1665}
           {:x 1423, :y 1665}
           {:x 1423, :y 1779}
           {:x 1065, :y 1779}]}}
        [x1y1 x2y1 x2y2 x1y2] (:vertices boundingPoly)]
    (assoc annotation
           :top-left (:y x1y1)
           :height (- (:x x2y1) (:x x1y1)))))
