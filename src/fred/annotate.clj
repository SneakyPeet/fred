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
