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

(defn input []
  (let [value (rf/subscribe [::subs/query])]
    [:input {:type "text"
             :value @value
             :on-change #(on-input-change %)}]))

(defn back-view [step]
  [:a {:style {:margin-left "10px"} :href "#back" :on-click #(on-back-click step)} "[Indietro]"])

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
  [:div
   [:span "Altre linee:"]
   [:ul (for [l (filter #(-> % :WaitMessage some?) other-lines)]
          [:li {:key (get-in l [:Line :LineCode])}
           [:span (str (get-in l [:Line :LineCode]) " - " (get-in l [:Line :LineDescription]) ": ")]
           [:b (get-wait-message (:WaitMessage l))]
           ])]])

(defn wait-message-view []
  (let [stop @(rf/subscribe [::subs/stop])
        {:keys [Id]} @(rf/subscribe [::subs/line-stops])
        line-stop (first (filter #(= (:JourneyPatternId %) Id) (:Lines stop)))
        loading? @(rf/subscribe [::subs/loading?])]
    ;;(println "lines @ stop" (:JourneyPatternId (second (:Lines stop))) "line" Id)
    [:span
     [:h2
      (if line-stop
        (if loading? "Mmm..." (get-wait-message (:WaitMessage line-stop)))
        "Figa non ci sto capendo un cazzo...")
      ]
     (if-not loading? (get-other-lines-messages (filter #(not= (:JourneyPatternId %) Id) (:Lines stop))))
     [:a {:href "#update" :on-click #(on-stop-click (:CustomerCode stop))} "[Aggiorna]"]
     (back-view :stop)])) ;; CustomerCode seems a mistake in the api response

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
