(ns trasporto-webapp.subs
  (:require
   [clojure.string :as s]
   [re-frame.core :as rf]))

(rf/reg-sub
 ::loading?
 (fn [db] (:loading? db)))

(rf/reg-sub
 ::query
 (fn [db] (:query db)))

(rf/reg-sub
 ::lines
 (fn [db [_ query]]
   (if query
     (let [lc-query (s/lower-case query)
           includef #(-> % (get-in [:Line :LineDescription]) s/lower-case (s/includes? lc-query))]
       (->> db :lines (filter includef)))
     (:lines db))))

(rf/reg-sub
 ::line-stops
 (fn [db [_ _]]
   (:line-stops db)))

(rf/reg-sub
 ::stops-query
 (fn [db] (:stops-query db)))

(rf/reg-sub
 ::stops
 (fn [db [_ query]]
   (if query
     (let [lc-query (s/lower-case query)
           stops (get-in db [:line-stops :Stops])
           includef #(-> % :Description s/lower-case (s/includes? lc-query))]
       (filter includef stops))
     (get-in db [:line-stops :Stops]))))

(rf/reg-sub
 ::stop
 (fn [db]
   (:stop db)))

(rf/reg-sub
 ::timetable
 (fn [db]
   (:timetable db)))

(rf/reg-sub
 ::dow
 (fn [db]
   (:dow db)))

(rf/reg-sub
 ::hour
 (fn [db]
   (:hour db)))

(rf/reg-sub
 ::sub-view
 (fn [db]
   (:sub-view db)))

(rf/reg-sub
 ::line-stop
 (fn [{:keys [stop line-stops]} db] 
   (first (filter #(= (:JourneyPatternId %) (:Id line-stops)) (:Lines stop)))))
