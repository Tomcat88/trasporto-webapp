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
