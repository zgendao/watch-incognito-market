(ns app.hello
  (:require
    [reagent.core :as r]
    [app.scrapers :refer [PRV-USD PRV-STOCK USDT-STOCK]]
    [app.api :as api]
    ["lightweight-charts" :as charts]
    ["moment" :as moment]
    ["materialize-css" :as materialize]
    [cljs.pprint :as pprint]
    [alandipert.storage-atom :refer [local-storage]]
    [cognitect.transit :as t]
    ))


;(def ws (new js/WebSocket (str "ws://localhost:8080/btc-usd")))

;(set!
;  (.-onmessage ws)
;  (fn [event]
;    (let [data (t/read (t/reader :json) (.-data event))]
;      
;      (js/console.log (str data))
;      
;      )))


(defn exp [x n]
     (if (zero? n) 1
         (* x (exp x (dec n)))))

(def memory (local-storage (atom {}) :prefs))

(def storage (r/atom {}))

(defn common-refreshData []
  (api/blockchainInfo storage)
  (when (get-in @storage [:blockchain :Beacon :Height]) (api/dexInfo storage))
  (js/setTimeout common-refreshData (* 5 1000)))

(defn refreshData []
  (api/validatorInfo storage)
  (js/setTimeout refreshData (* 60 1000)))

(api/tokenList storage)

(defonce common-refresher (common-refreshData))
(defonce refresher (refreshData))

(defn statistics []
  (r/create-class
    {
     :component-did-mount 
     (fn []
(let [
      currentCurrency "PRV-pUSDT"
      currentInterval 21600
      URL (.-location js/document)
      params (.-searchParams URL)

      options
  #js
   {:localization
      #js
       {:timeFormatter
          (fn [businessDayOrTimestamp]
            (.format (moment (* businessDayOrTimestamp 1000)) "MMM Do hh a"))}}


   watermarkDefault
  #js
   {:color "rgba(11, 94, 29, 0.4)",
    :visible true,
    :fontSize 20,
    :horzAlign "left",
    :vertAlign "top"}

 candles (.getElementById js/document "candles")

candlesChart
  (charts/createChart
    candles
    (js/Object.assign
      #js {}
      options
      #js {:width (.-clientWidth candles)}
      #js {:height 400}))

candlestickSeries (.addCandlestickSeries candlesChart)

_
(.applyOptions
  candlestickSeries
  #js {:priceFormat #js {:precision 6, :minMove 0.000001}})

volumes1 (.getElementById js/document "volumes1")

volumes1Chart
  (charts/createChart
    volumes1
    (js/Object.assign
      #js {}
      options
      #js {:width (.-clientWidth volumes1)}
      #js {:height 140}))

volumes1Series (.addLineSeries volumes1Chart #js {:color "#b2bcc7"})

_
(.applyOptions
  volumes1Series
  #js {:priceFormat #js {:precision 0, :minMove 1}})

volumes2 (.getElementById js/document "volumes2")

volumes2Chart
  (charts/createChart
    volumes2
    (js/Object.assign
      #js {}
      options
      #js {:width (.-clientWidth volumes2)}
      #js {:height 140}))

volumes2Series (.addLineSeries volumes2Chart #js {:color "#b2bcc7"})

_
(.applyOptions
  volumes2Series
  #js {:priceFormat #js {:precision 0, :minMove 1}})

      ]
     
(let [data
      (clj->js
        PRV-USD)
      ]
      (.setData candlestickSeries data)
      (.applyOptions
        candlesChart
        (js->clj
         {
		:layout {
			:backgroundColor "#2B2B43"
			:lineColor "#2B2B43"
			:textColor "#D9D9D9"
		}
		:crosshair {
			:color "#758696"
		}
		:grid {
			:vertLines {
				:color "#2B2B43"
			}
			:horzLines {
				:color "#363C4E"
			}
		}
          :watermark
            (js/Object.assign
              (clj->js {})
              watermarkDefault
              #js {:text currentCurrency})})))
(let [data (clj->js PRV-STOCK)]
      (.setData volumes1Series data)
      (.applyOptions
        volumes1Chart
        #js
         {:watermark
            (js/Object.assign
              #js {}
              watermarkDefault
              #js {:color "#b2bcc7"}
              #js {:text (str "PRV" " total")})}))
(let [data (clj->js USDT-STOCK)]
      (.setData volumes2Series data)
      (.applyOptions
        volumes2Chart
        #js
         {:watermark
            (js/Object.assign
              #js {}
              watermarkDefault
              #js {:color "#b2bcc7"}
              #js {:text (str "pUSDT" " total")})}))

)
   )
     
     :reagent-render (fn []
 
   [:div.container.z-depth-4
    {:style {:border-radius "10px" :padding "10px" :background "white"}}
    [:div [:div#candles]]
    [:div [:div#volumes1]]
    [:div [:div#volumes2]]]
                       
                       )}
    ) )


(defn dex []
  [:div
  [:div.container
   [:h4 "DEX Waiting Contributions"]
   [:ul.collection
    (map
      (fn [{:keys [amount symbol name]}]
         [:li.collection-item.avatar
        [:img.circle {:src ""}]
        ;[:span "Contributor: "(:ContributorAddressStr contribution)]
        ;[:span (str tokenInfo)]
        [:span amount " "symbol " ("name")"]
        ]
        )
    (reverse
      (sort-by (juxt :amount)
             (keep 
     (fn [[id contribution]]
       (let [
             tokenID (:TokenIDStr contribution)
             tokenInfo (get (:tokens @storage) tokenID)
             amount 
             (if tokenInfo
               (pprint/cl-format nil "~,6f"
                 (/ (:Amount contribution)
                (float (exp
                 10
                 (get tokenInfo "Decimals")))))
               (:Amount contribution))
             ] 
        (when tokenInfo 
       {:amount amount
        :symbol (get tokenInfo "Symbol")
        :name (get tokenInfo "Name")
        })
       ))
     (:WaitingPDEContributions (:dex @storage))))))
    ]]
  [:div.container
   [:h4 "DEX Pool"]
   [:ul.collection
    (keep 
     (fn [[id pair]]
         (when (or 
                 (not= (:Token1PoolValue pair) 0)
                 (not= (:Token2PoolValue pair) 0)
                   )
           (let [
             token1ID (:Token1IDStr pair)
             token1Info (get (:tokens @storage) token1ID)
             amount1 
             (if token1Info
               (pprint/cl-format nil "~,3f"
                 (/ (:Token1PoolValue pair)
                (float (exp
                 10
                 (get token1Info "Decimals")))))
               (:Token1PoolValue pair))
             
             token2ID (:Token2IDStr pair)
             token2Info (get (:tokens @storage) token2ID)
             amount2 
             (if token2Info
               (pprint/cl-format nil "~,3f"
                 (/ (:Token2PoolValue pair)
                (float (exp
                 10
                 (get token2Info "Decimals")))))
               (:Token2PoolValue pair))
                 
                 ]
           [:li.collection-item.avatar
        [:img.circle {:src ""}]
        ;[:p "Token ID: "token1ID]
        [:p amount1 " "(get token1Info "Symbol") " ("(get token1Info "Name")")"]
        ;[:p "Token ID: "token2ID]
        [:p amount2 " "(get token2Info "Symbol") " ("(get token2Info "Name")")"]
        ])
       ))
     (sort-by first (:PDEPoolPairs (:dex @storage))))
    ]]
  ]
  )


(defn validators [memory storage]
[:div.container
  [:div.container
   (str (map (fn [[k c]]
               [(name k) (count c)]) @storage))]
  
 ; [:div.container
 ;  (str (map (fn [[k c]]
 ;              [(name k) (when (or (map? c) (vector? c)) (count c))]) (:validator @storage)))]
  
  
  ]
  )



(defn landing []
  [:div
   [:nav
    [:div.nav-wrapper
    [:a.center.brand-logo "Incognito Market"]
     [:ul.right.hide-on-med-and-down
    ;[:li
    ; [:a
    ;  {:href "sass.html"}
    ;  [:i.material-icons.left "search"]
    ;  "Link with Left Icon"]]
    ;[:li
    ; [:a
    ;  {:href "badges.html"}
    ;  [:i.material-icons.right "view_module"]
    ;  "Link with Right Icon"]]
    ]
    ]]
   ]
  )

(defn footer []
  
    [:footer.page-footer
  [:div.container
   [:div.row
    [:div.col.l6.s12
     [:h5.white-text "Incognito Market"]
     [:p.white-text
      "Advanced tools for Incognito investors."
      ]
     [:p.grey-text.text-lighten-4
      "You could support my work sending PRV:"]
     [:input {:value "12RqosnamJWWjW25po5w7wQEzbnM7EjYF6bPFnTkZmC34wx7FuCZrD9RYtBQXzDd6Tpw6bkJFFiDXJQitpxA9yP7zeQMAvepLy5Qc2W"}]]
    [:div.col.l4.offset-l2.s12
     [:h5.white-text "Links"]
     [:ul
      [:li [:a.grey-text.text-lighten-3 {:target "_blank" :href "https://pdex.incognito.best/"} "inccry's charts"]]
      [:li [:a.grey-text.text-lighten-3 {:target "_blank" :href "https://incognito.mesquka.com/"} "mesquka's ecosystem tools"]]
      ]]
    ]]
  [:div.footer-copyright
   [:div.container
    "\n            © 2020 All privacy reserved.\n            "
    [:a.grey-text.text-lighten-4.right {:target "_blank" :href "https://incognito.org/u/raz/summary"} "Made by raz"]]]]
  )

(defn hello []
  (let [
        active? (when (:nodes @memory) (not (empty? (:nodes @memory))))
        noden (count (:RewardReceiver (:validator @storage)))
        
        shown-validators
           (keep
            (fn [[public-id info]]
              (when
                (if active? 
                  ((set (keys (:nodes @memory))) public-id)
                  true)
                (let [
                      validator (when active? (get-in @memory [:nodes public-id]))
                      ]
                [public-id info 
                 validator
                 (when (and validator (not (boolean? validator))) (:name validator))
                 ])
                ))
            (get-in @storage [:validators]))
        
           shown-validators
           (if active?
             (sort-by last shown-validators)
             shown-validators
             )
           earning
           (when active?
           (keep (fn [[_ info _ n]]
                   (when
                     (:committee? info) n)
                   ) shown-validators))
           pending
           (when active?
           (keep (fn [[_ info _ n]]
                   (when
                     (:pending? info) n)
                   ) shown-validators))
           ]
  [:<>
   
    ;[:div 
     ;[statistics]
    ; [:center "(Charts are not udpated, this is only a demo in progress.)"]]
    

[landing]    
  [:div.container
  [:h5 "Active shards: " (get-in @storage [:blockchain :ActiveShards])]
  [:h5 "Reward Receiver Nodes: " (when-not (= noden 0) noden)]
  [:h5 "Current blockchain height: " (get-in @storage [:blockchain :Beacon :Height])]
  ;[:h5 "Current transaction number: " (get-in @storage [:blockchain :TotalTxs])]
  [:h5 "Remaining block epoch: " (get-in @storage [:blockchain :Beacon :RemainingBlockEpoch])]
  
   (if active?
     (when (and (:nodes @memory) (not= 0 noden)) 
       [:h5
      "You have " [:b (count (:nodes @memory))" node"(when (< 1 (count (:nodes @memory))) "s")]
      " worth "[:b (* (count (:nodes @memory)) 1750) " PRV"]
      " or "[:b (pprint/cl-format nil "~,1f" (* (count (:nodes @memory)) 1750 0.547)) " USDT"]
      " and "
      [:b (pprint/cl-format nil "~,3f" (* (/ (count (:nodes @memory)) noden) 100))
      "%"]" network share."
      ])
     )
  (when active?
    [:h5
     "Currently "
     (if (empty? earning) "no one" [:b (map #(str % " ") earning)])
     " "
     (if (< 1 (count earning)) "are" "is")
     " earning"
     (if (empty? pending)
       "."
     [:span
      ", "(if (empty? earning) "but" "and")" "
     [:b (map #(str % " ") pending)]
     " "
     (if (< 1 (count pending)) "are" "is")
     " pending."])
     ])
   ]
    
  [:div.container {:style {:padding-top "30px"
                 }}
   ;[:h4 "Add / edit node"]
   [:br]
   [:div.input-field
    [:input.validate {:type "text" :id "add"}]
    [:label.active {:for "add"} "Paste your public key here:"]]
   ;[:div.input-field
   ; [:input.validate {:type "text" :id "readonly"}]
   ; [:label.active {:for "readonly"} "Paste your read-only key here:"]]
   [:div.input-field
    [:input.validate {:type "text" :id "nodename"}]
    [:label.active {:for "nodename"} "Name your node:"]]
   [:button.waves-effect.waves-light.btn
    {:on-click #(let 
                  [
                   v (.-value (js/document.getElementById "add"))
                   n (.-value (js/document.getElementById "nodename"))
                   ;r (.-value (js/document.getElementById "readonly"))
                   ]
                  (if (get-in @storage [:validators v])
                    (do
                      (swap! memory assoc-in [:nodes v] 
                             {
                              :name n
                              ;:read-only r
                              })
                      (set! (.-value (js/document.getElementById "add")) "")
                      (set! (.-value (js/document.getElementById "nodename")) "")
                      ;(set! (.-value (js/document.getElementById "readonly")) "")
                      (materialize/toast (clj->js {:html "Wait a sec.."})))
                    (materialize/toast (clj->js {:html "Node cannot be found."}))
                    ))}
    "Watch my node"]


          [:ul.collection (when-not active? {:style {:height "600px" :overflow "auto"}})
            
           (map
             (fn [[public-id info validator validator-name]]
               [:li
                {:class (str "collection-item "
                             (cond
                               (:committee? info) "green accent-2"
                               (:pending? info) "lime accent-2"
                               ))}
               [:p
                 (when (and validator (not (boolean? validator)))
                   [:h3 {:style {:margin-top 0}} validator-name])]
               [:h6 {:style {:font-weight "800"}}
                (when (:waiting? info) [:span "Waiting to be selected. "])
                (when (:shard info) [:span "In shard number "(:shard info)". "])
                (when (:committee? info) [:span "Currently in committee. "])
                (when (:pending? info) [:span "Pending.."])
                ]
               [:br]
               [:div.input-field
                [:input.validate {:type "text" :id public-id :value public-id}]
                [:label.active {:for public-id} "Public key:"]]
               (when (:read-only validator)
               [:div.input-field
                [:input.validate {:type "text" :id (:read-only validator) :value (:read-only validator)}]
                [:label.active {:for (:read-only validator)} "Read-only key:"]])
               (when (get (:nodes @memory) public-id)
                 [:button {:on-click #(do
                                        (swap! memory update :nodes dissoc public-id)
                                        (materialize/toast (clj->js {:html "Unfollowing.."}))
                                        )} "Unfollow"])
               ]
               )
             shown-validators
             )
           ]
          
          ]
   [footer] 
   ; [validators memory storage]

  ;  [dex]
    
  ;[:div.container
  ; [:h4 "DEX Shares"]
  ; [:ul.collection
  ;  (keep 
  ;   (fn [[id volume]]
  ;     (when-not (= 0 volume)
  ;       [:li.collection-item.avatar
  ;      [:img.circle {:src ""}]
  ;      [:span (name id)]
  ;      [:p "Volume: "volume]]
  ;     ))
  ;   (:PDEShares (:dex @storage)))
  ;  ]]
    
]))


