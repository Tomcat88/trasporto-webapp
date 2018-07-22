(ns trasporto-webapp.events
  (:require
   [re-frame.core :as rf]
   [trasporto-webapp.db :as db]
   [ajax.core :as ajax]))

(rf/reg-event-db
 ::initialize-db
 (fn [_ _]
   db/default-db))

(rf/reg-event-fx
 ::process-lines-response
 (fn [{:keys [db]} [_ {:keys [JourneyPatterns]}]]
   (println JourneyPatterns)
   {:db (assoc db :lines JourneyPatterns)
    :dispatch [::loading false]}))

(rf/reg-event-fx
 ::failed-lines
 (fn [{:keys [db]} [_ fail]]
   (println "fail" fail)
   {:dispatch [::loading false]}))

(rf/reg-event-fx
 ::process-stops-response
 (fn [{:keys [db]} [_ stops]]
   (println stops)
   {:db (assoc db :line-stops stops)
    :dispatch [::loading false]}))

(rf/reg-event-fx
 ::failed-stops
 (fn [{:keys [db]} [_ fail]]
   (println "fail" fail)
   {:dispatch [::loading false]}))

(rf/reg-event-db
 ::loading
 (fn [db [_ loading]] 
   (assoc db :loading? loading)))

(rf/reg-event-fx
 ::load-lines
 (fn [{:keys [db]} [_ _]]
   {:dispatch [::loading true]
    :http-xhrio {:method :get
                 :uri "http://localhost:3000/lines"
                 :headers {
                           "Access-Control-Allow-Origin" "http://localhost:3000" 
                           }
                 :timeout 5000
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [::process-lines-response]
                 :on-failure [::failed-lines]
                 }}))

(rf/reg-event-db
 ::query-change
 (fn [db [_ new-query]]
   (assoc db :query new-query)))

(rf/reg-event-db
 ::stops-query-change
 (fn [db [_ new-query]]
   (assoc db :stops-query new-query)))

(rf/reg-event-fx
 ::line-click
 (fn [{:keys [db]} [_ line]]
   {:dispatch [::loading true]
    :http-xhrio {:method :get
                 :uri (str "http://localhost:3000/line/" (:Code line) "/stops?direction=" (:Direction line))
                 :timeout 5000
                 :headers {
                           "Access-Control-Allow-Origin" "http://localhost:3000" 
                           }
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [::process-stops-response]
                 :on-failure [::failed-stops]
                 } 
    }))
