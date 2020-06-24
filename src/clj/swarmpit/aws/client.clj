(ns swarmpit.aws.client
  (:require [cognitect.aws.client.api :as aws]
            [cognitect.aws.credentials :as credentials]
            [taoensso.timbre :refer [info error]]))

(defn- log-error [op service result]
  (info "|>" "Execute" op)
  (info "|>" "Service" service)
  (error "|<" "Request Execution failed:" (:message result))
  (error "|<" result))

(defn- client [{:keys [service region accessKeyId accessKey]}]
  (aws/client {:api                  service
               :region               (keyword region)
               :credentials-provider (credentials/basic-credentials-provider
                                       {:access-key-id     accessKeyId
                                        :secret-access-key accessKey})}))

(defn- execute [op service account]
  (let [acc (select-keys account [:region :accessKeyId :accessKey])
        result (aws/invoke
                 (client (merge acc {:service service}))
                 {:op op})]
    (if (some? (:__type result))
      (let [error (or (:message result) "Unknown error")]
        (log-error op service result)
        (throw
          (ex-info
            (str "AWS client error: " (:message result))
            {:status 401
             :type   :aws-client
             :body   {:error (str "AWS client error: " error)}})))
      result)))

(defn ecr-token [ecr]
  (-> (execute :GetAuthorizationToken :ecr ecr)
      :authorizationData
      (first)))