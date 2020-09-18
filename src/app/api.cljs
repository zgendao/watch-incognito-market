(ns app.api
  (:require
    ["axios" :as axios]
    ["uuid" :as uuid]
    ))

(defn deep-merge [v & vs]
  (letfn [(rec-merge [v1 v2]
            (if (and (map? v1) (map? v2))
              (merge-with deep-merge v1 v2)
              v2))]
    (when (some identity vs)
      (reduce #(rec-merge %1 %2) v vs))))

(def INCOGNITO_NODE "https://fullnode.incognito.org")

(def INCOGNITO_API "https://api.incognito.org")

(def INCOGNITO_ANALYTIC_API "https://analytic-api.incognito.org")

(def PROXY_PREFIX "https://cors-proxy-mesquka.herokuapp.com/")

(defn tokenList
  [storage]
  (.then
    ((.-get axios) (str "" INCOGNITO_API "/ptoken/list"))
    (fn [result]
      (when-not (-> result .-data .-Error)
        (let [tokens
              (js->clj (-> result .-data .-Result))]
        (swap! storage assoc :tokens
               (assoc
                 (into (hash-map)
                     (map
                       (fn [token]
                         [(get token "TokenID") (dissoc token "TokenID")])
                       tokens))
                 "0000000000000000000000000000000000000000000000000000000000000004"
                 {
                  "Symbol" "PRV"
                  "Name" "Privacy"
                  "Decimals" 9
                  }
                 )
               ))
        ))))

(defn blockchainInfo
  [storage]
  (.then
    ((.-post axios)
     INCOGNITO_NODE
     #js {:jsonrpc "2.0", :method "getblockchaininfo", :id (uuid/v4)})
     
    (fn [result]
      (when-not (-> result .-data .-Error)
        (let [blockchainInfo (get-in (js->clj result :keywordize-keys true) [:data :Result])
              info blockchainInfo 
              ]
               (when blockchainInfo 
            (swap! storage assoc :blockchain
                   
            (assoc
            info
            :TotalTxs (get-in blockchainInfo [:BestBlocks :-1 :TotalTxs])
            :Beacon (get-in blockchainInfo [:BestBlocks :-1])
            )
                   )
            )
               )
              ))))

(defn validatorInfo
  [storage]
  (.then
    ((.-post axios)
     INCOGNITO_NODE
     #js {:jsonrpc "1.0", :method "getbeaconbeststatedetail", :id (uuid/v4)})
     
    (fn [result]
      (when-not (-> result .-data .-Error)
        (let [blockchainInfo (get-in (js->clj result :keywordize-keys true) [:data :Result])
              info blockchainInfo 
              waiting-list

                                 (into (hash-map)
                                 (mapv
                                   (fn [node]
                                     [(:IncPubKey node) {:waiting? true}])
                                   (:CandidateShardWaitingForNextRandom info)
                                   ))
              ]
          (when blockchainInfo
            (swap! storage assoc
                   :validator info
                   :validators (dissoc
                                 (deep-merge
                                 (apply merge
                                 (mapv
                                   (fn [[shard nodes]]
                                     (into (hash-map) (mapv 
                                       (fn [node]
                                         [(:IncPubKey node) {:pending? true :shard shard}])
                                       nodes
                                       )))
                                   (:ShardPendingValidator info)
                                   ))
                                 (apply merge
                                 (mapv
                                   (fn [[shard nodes]]
                                     (into (hash-map) (mapv 
                                       (fn [node]
                                         [(:IncPubKey node) {:committee? true :shard shard}])
                                       nodes
                                       )))
                                   (:ShardCommittee info)
                                   ))
                                 waiting-list)
                                 nil)
                   )
            ))
              ))))

(defn beaconbestInfo
  [storage]
  (.then
    ((.-post axios)
     INCOGNITO_NODE
     #js {:jsonrpc "1.0", :method "getbeaconbeststatedetail", :id (uuid/v4)})
     
    (fn [result]
      (when-not (-> result .-data .-Error)
        (let [blockchainInfo (-> result .-data .-Result)
              info (js->clj blockchainInfo :keywordize-keys true)
              ]
            (swap! storage assoc :beacon
                   info)
            )
              ))))

(defn rewardInfo
  [storage]
  (.then
    ((.-post axios)
     INCOGNITO_NODE
     #js {:jsonrpc "1.0", :method "listrewardamount", :id (uuid/v4)})
     
    (fn [result]
      (when-not (-> result .-data .-Error)
        (let [blockchainInfo (-> result .-data .-Result)
              info (js->clj blockchainInfo :keywordize-keys true)
              ]
            (swap! storage assoc :reward
                   info)
            )
              ))))


(defn dexInfo
  [storage]
  (.then
    ((.-post axios)
     INCOGNITO_NODE
     #js
     {:jsonrpc "2.0",
      :method "getpdestate",
      :params #js [#js {:BeaconHeight (get-in @storage [:blockchain :Beacon :Height])}],
      :id (uuid/v4)})
    (fn [result]
      (when-not (-> result .-data .-Error)
        (swap! storage assoc :dex (js->clj (-> result .-data .-Result) :keywordize-keys true))
        ))))












(defn getPublicKeyFromPaymentAddress
  [address]
  (js/Promise
    (fn [resolve reject]
      (->
        ((.-post axios)
          INCOGNITO_NODE
          #js
           {:jsonrpc "2.0",
            :method "getpublickeyfrompaymentaddress",
            :params #js [address],
            :id (uuid/v4)})
        (.then
          (fn [result]
            (if (= (.. result -data -Error) nil)
              (resolve (.. result -data -Result))
              (reject))))
        (.catch reject)))))

(defn listRewardAmount
  []
  (js/Promise
    (fn [resolve reject]
      (->
        ((.-post axios)
          INCOGNITO_NODE
          #js {:jsonrpc "2.0", :method "listrewardamount", :id (uuid/v4)})
        (.then
          (fn [result]
            (if (= (.. result -data -Error) nil)
              (resolve (.. result -data -Result))
              (reject))))
        (.catch reject)))))

(defn getPublicKeyMining
  [node direct]
  (js/Promise
    (fn [resolve reject]
      (let [endpoint
              (if direct (str "" node "") (str "" PROXY_PREFIX "" node ""))]
        (->
          ((.-post axios)
            endpoint
            #js {:jsonrpc "2.0", :method "getpublickeymining", :id (uuid/v4)})
          (.then
            (fn [result]
              (if
                (and
                  (= (.. result -data -Error) nil)
                  (not= (.. result -data -Result) nil))
                (resolve (.. result -data -Result))
                (reject))))
          (.catch reject))))))

(defn getMinerRewardFromMiningKey
  [key]
  (js/Promise
    (fn [resolve reject]
      (->
        ((.-post axios)
          INCOGNITO_NODE
          #js
           {:jsonrpc "2.0",
            :method "getminerrewardfromminingkey",
            :params #js [key],
            :id (uuid/v4)})
        (.then
          (fn [result]
            (if (= (.. result -data -Error) nil)
              (resolve (.. result -data -Result))
              (reject))))
        (.catch reject)))))

(defn getMiningInfo
  [node direct]
  (js/Promise
    (fn [resolve reject]
      (let [endpoint
              (if direct (str "" node "") (str "" PROXY_PREFIX "" node ""))]
        (->
          ((.-post axios)
            endpoint
            #js {:jsonrpc "2.0", :method "getmininginfo", :id (uuid/v4)})
          (.then
            (fn [result]
              (if (= (.. result -data -Error) nil)
                (resolve (.. result -data -Result))
                (reject))))
          (.catch reject))))))


(defn getMempoolInfo
  []
  (js/Promise
    (fn [resolve reject]
      (->
        ((.-post axios)
          INCOGNITO_NODE
          #js {:jsonrpc "2.0", :method "getmempoolinfo", :id (uuid/v4)})
        (.then
          (fn [result]
            (if (= (.. result -data -Error) nil)
              (resolve (.. result -data -Result))
              (reject))))
        (.catch reject)))))


