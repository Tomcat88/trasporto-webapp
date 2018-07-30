(ns trasporto-webapp.views
  (:require
   [re-frame.core :as rf]
   [trasporto-webapp.subs :as subs]
   [trasporto-webapp.events :as events]
   [day8.re-frame.http-fx]
   [clojure.string :as s]
   ))

(defn on-input-change [input]
  (let [lines @(rf/subscribe [::subs/lines])
        query @(rf/subscribe [::subs/query])]
    ;;(println "lines" lines)
    (if (seq lines)
      (rf/dispatch [::events/query-change (-> input .-target .-value)])
      (do (rf/dispatch [::events/query-change (-> input .-target .-value)])
          (rf/dispatch [::events/load-lines])))))

(defn on-stops-query-change [input]
  (let [query @(rf/subscribe [::subs/stops-query])]
    (rf/dispatch [::events/stops-query-change (-> input .-target .-value)])))

(defn on-line-click [line]
  (rf/dispatch [::events/line-click line]))

(defn on-stop-click [stop]
  (rf/dispatch [::events/stop-click stop]))

(defn on-back-click [step]
  (rf/dispatch [::events/back-click step]))

(defn on-timetable-click [line stop direction]
  (rf/dispatch [::events/timetable-click stop line direction]))

(defn on-other-lines-click []
  (rf/dispatch [::events/other-lines-click]))

(defn input []
  (let [value (rf/subscribe [::subs/query])]
    [:input {:type "text"
             :value @value
             :on-change #(on-input-change %)}]))

(defn back-view [step]
  [:a {:style {:margin-left "10px"} :href "#back" :on-click #(on-back-click step)} "[Indietro]"])

(defn refresh-view [stop] ;; CustomerCode seems a mistake in the api response
  [:a {:href "#update" :on-click #(on-stop-click (:CustomerCode stop))} "[Aggiorna]"])

(defn get-timetable-view [line stop]
  (println "LINE" line)
  [:a {:style {:margin-left "10px"} :href "#timetable" :on-click #(on-timetable-click (get-in line [:Line :LineCode]) (:CustomerCode stop) (:Direction line))} "[Orari]"])

(defn get-other-lines-view []
  [:a {:style {:margin-left "10px"} :href "#other-lines" :on-click #(on-other-lines-click)} "[Altre linee]"])

(defn stops-query-input []
  (let [value (rf/subscribe [::subs/stops-query])]
    [:span
     [:input {:type "text"
              :value @value
              :on-change #(on-stops-query-change %)}]
     (back-view :line-stops)
     ]))

(defn loading-view []
  (let [loading? (rf/subscribe [::subs/loading?])]
    ;;(println @loading?)
    (if @loading? [:div "Carico..."] nil)))

(defn line-view [line]
  [:li {:key (:Id line)}
   [:a 
    {:href (str "#" (:Id line)) :on-click #(on-line-click line)}
    (get-in line [:Line :LineDescription])]])

(defn lines-view []
  (let [query (rf/subscribe [::subs/query])
        lines (rf/subscribe [::subs/lines @query])]
    [:ul
     (for [l @lines] (line-view l))]))

(defn stop-view [stop]
  (let [code (:Code stop)]
    [:li {:key code}
     [:a
      {:href (str "#" code) :on-click #(on-stop-click code)}
      (:Description stop)]]))

(defn stops-view []
  (let [query @(rf/subscribe [::subs/stops-query])
        stops @(rf/subscribe [::subs/stops query])]
    [:ul
     (for [s stops] (stop-view s))]))

(defn get-wait-message [wait-message]
  (case wait-message
    "in arrivo" "In arrivo!"
    "ricalcolo" "Mmm... 'spe che ricalcolo"
    "no serv."  "Ci vediamo domani mattina..."
    nil         "Prima o poi..." ;; should not happen
    (let [[min _] (s/split wait-message #" ")
          msg (str "Tra " min " min.")]
      (if (>= (js/parseInt min) 10)
        (str msg " Sei fottuto.")
        msg))
    ))

(defn get-other-lines-messages [other-lines]
  (let [lines-with-message (filter #(-> % :WaitMessage some?) other-lines)]
    (if (not-empty lines-with-message)
      [:div
       [:span "Altre linee:"]
       [:ul (for [l lines-with-message]
              [:li {:key (get-in l [:Line :LineCode])}
               [:span (str (get-in l [:Line :LineCode]) " - " (get-in l [:Line :LineDescription]) ": ")]
               [:b (get-wait-message (:WaitMessage l))]
               ])]]
      nil)))

(defn timetable-view []
  (let [timetable    @(rf/subscribe [::subs/timetable])
        dow          @(rf/subscribe [::subs/dow])
        current-hour @(rf/subscribe [::subs/hour])
        dow-timetable (first (filter #(true? (get (:DayType %) dow)) (:TimeSchedules timetable)))]
    (println "dow" dow "dt" dow-timetable)
    (if dow-timetable
      [:div
       [:span (get-in dow-timetable [:DayType :DayTypeDescription])]
       [:ul
        (for [s (:Schedule dow-timetable)]
          (let [hour (:Hour s)
                detail (:ScheduleDetail s)
                night-detail (:NightDetail s)
                highlight (= hour current-hour)
                desc (str hour (when (not= "" detail) (str " - " detail)) (when (not= "" night-detail) (str " - " night-detail)) "")]
            [:li (if highlight [:b desc] desc)]))]]
      nil)
    ))

(defn actions-view []
  (let [stop @(rf/subscribe [::subs/stop])
        line-stop @(rf/subscribe [::subs/line-stop])
        sub-view @(rf/subscribe [::subs/sub-view])]
    [:div
     (refresh-view stop)
     (case sub-view
       :other-lines (get-timetable-view line-stop stop)
       :timetable   (get-other-lines-view))
     (back-view :stop)]))

(defn wait-message-view []
  (let [stop @(rf/subscribe [::subs/stop])
        {:keys [Id]} @(rf/subscribe [::subs/line-stops])
        line-stop @(rf/subscribe [::subs/line-stop])
        loading? @(rf/subscribe [::subs/loading?])
        sub-view @(rf/subscribe [::subs/sub-view])]
    ;;(println "lines @ stop" (:JourneyPatternId (second (:Lines stop))) "line" Id)
    [:span
     [:h2
      (if line-stop
        (if loading? "Mmm..." (get-wait-message (:WaitMessage line-stop)))
        "Mmm... non trovo la linea")
      ]
     (if-not loading?
       (case sub-view
         :other-lines (get-other-lines-messages (filter #(not= (:JourneyPatternId %) Id) (:Lines stop)))
         :timetable   (timetable-view)))
     (actions-view)
     ]))

(defn get-article [line]
  (case (:TrasportMode line)
    0 "la"
    1 "il"
    3 "la"
    "la"))

(defn main-panel []
  (let [{:keys [Line] :as line-stops} @(rf/subscribe [::subs/line-stops])
        stop @(rf/subscribe [::subs/stop])]
    [:div
     [:h1 (if (empty? line-stops)
            "Quando cazzo arriva?"
            (if-not stop
              (str "Quando cazzo arriva " (get-article Line) " " (:LineCode Line) "?")
              (str "Quando cazzo arriva " (get-article Line) " " (:LineCode Line) " a " (:Description stop) "?")))]
     (if stop
       [:div (wait-message-view)]
       [:div
        (if (empty? line-stops)
          (input)
          (stops-query-input))
        (loading-view)
        (if (empty? line-stops)
          (lines-view)
          (stops-view))])
     ]))
