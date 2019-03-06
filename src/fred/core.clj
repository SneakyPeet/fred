(ns fred.core
  (:require [fred.annotate :as image]
            [clojure.java.io :as io]
            [clojure.string :as string]))

(def raw-image-folder-path "images/raw")
(def processed-image-folder-path "images/processed")
(def image-data-folder-path "images/data")


(defn process-image-file [file]
  (let [file-name (.getName file)
        input-path (.getPath file)
        output-path (str processed-image-folder-path "/" file-name)
        {:keys [annotate-type valid?] :as result} (image/annotate-text input-path)
        data-path (str image-data-folder-path "/" annotate-type "_" file-name ".edn")]
    (spit data-path (pr-str result))
    (io/copy file (io/file output-path))
    (io/delete-file file)
    (prn (str "Completed " file-name " | Success? " valid?))
    valid?))


(defn prep-folders []
  (->> [raw-image-folder-path processed-image-folder-path image-data-folder-path]
       (map #(.mkdir (io/file %)))))


(defn get-raw-images-files []
  (->> (file-seq (io/file raw-image-folder-path))
       (filter #(string/ends-with? (.getName %) ".JPG"))))


(defn process-image-files []
  (let [p-length *print-length*]
    (prep-folders)
    (set! *print-length* Integer/MAX_VALUE)
    (time
     (->> (get-raw-images-files)
          (map process-image-file)
          doall))
    (set! *print-length* p-length)))

#_(process-image-files)
