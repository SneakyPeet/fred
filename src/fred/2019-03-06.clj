;;;https://cloud.google.com/vision/docs/ocr
(ns fred.annotate
  (:require [org.httpkit.client :as http]
            [cheshire.core :as json]
            [clojure.data.codec.base64 :as b64]))


(def ^{:private true} token (slurp "./secrets/api.txt"))


(defn- encode-file [path]
  (let [f (java.io.File. path)
        ary (byte-array (.length f))]
    (with-open [is (java.io.FileInputStream. f)]
      (.read is ary))
    (String. (b64/encode ary) "UTF-8")))


(defn- request [annotate-type encoded-image]
  {:method :post
   :timeout (* 1000 300)
   :url (str "https://vision.googleapis.com/v1/images:annotate?key=" token)
   :body (json/encode
          {:requests
           [{:features [{:type annotate-type}]
             :image {:content encoded-image}}]})})


(defn- annotate [annotate-type path]
  (let [data (encode-file path)
        request (request annotate-type data)
        response @(http/request request)]
    (if (= 200 (:status response))
      {:valid? true :data (-> response
                              :body
                              (json/decode keyword))
       :annotate-type annotate-type}
      {:valid? false :data (dissoc response :opts)
       :annotate-type annotate-type})))


(defn annotate-text [path] (annotate "TEXT_DETECTION" path))


(defn annotate-document [path] (annotate "DOCUMENT_TEXT_DETECTION" path))



*****************************************


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
