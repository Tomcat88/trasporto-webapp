(ns trasporto-webapp.views
  (:require
   [re-frame.core :as rf]
   [trasporto-webapp.subs :as subs]
   [trasporto-webapp.events :as events]
   [day8.re-frame.http-fx]
   ))

(defn on-input-change [input]
  (let [lines @(rf/subscribe [::subs/lines])
        query @(rf/subscribe [::subs/query])]
    (println "lines" lines)
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

(defn input []
  (let [value (rf/subscribe [::subs/query])]
    [:input {:type "text"
             :value @value
             :on-change #(on-input-change %)}]))

(defn stops-query-input []
  (let [value (rf/subscribe [::subs/stops-query])]
    [:input {:type "text"
             :value @value
             :on-change #(on-stops-query-change %)}]))

(defn loading-view []
  (let [loading? (rf/subscribe [::subs/loading?])]
    (println @loading?)
    (if @loading? [:div "Carico..."] nil)))

(defn line-view [line]
  [:li {:key (:Id line)}
   [:a 
    {:href "#" :on-click #(on-line-click line)}
    (get-in line [:Line :LineDescription])]])

(defn lines-view []
  (let [query (rf/subscribe [::subs/query])
        lines (rf/subscribe [::subs/lines @query])]
    [:ul
     (for [l @lines] (line-view l))]))

(defn stop-view [stop]
  [:li {:key (:Code stop)}
   [:a
    {:href "#" :on-click #(on-stop-click stop)}
    (:Description stop)]])

(defn stops-view []
  (let [query @(rf/subscribe [::subs/stops-query])
        stops @(rf/subscribe [::subs/stops query])]
    [:ul
     (for [s stops] (stop-view s))]))

(defn get-article [line]
  (case (:TrasportMode line)
    0 "la"
    1 "il"
    3 "la"
    "la"))

(defn main-panel []
  (let [{:keys [Line] :as line-stops} @(rf/subscribe [::subs/line-stops])]
    [:div
     [:h1 (if (empty? line-stops)
            "Quando cazzo arriva?"
            (str "Quando cazzo arriva " (get-article Line) " " (:LineCode Line) "?"))]
     [:div
      (if (empty? line-stops)
        (input)
        (stops-query-input))
      (loading-view)
      (if (empty? line-stops)
        (lines-view)
        (stops-view))
      ]]))
