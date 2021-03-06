(ns trasporto-webapp.events
  (:require
   [re-frame.core :as rf]
   [trasporto-webapp.db :as db]
   [ajax.core :as ajax]))

(goog-define api-url "http://localhost:3000")

(def cors-header {"Access-Control-Allow-Origin" api-url})

(defn lines-url      []                    (str api-url "/lines"))
(defn lines-stop-url [line direction]      (str api-url "/line/" line "/stops?direction=" direction))
(defn stop-url       [stop]                (str api-url "/stop/" stop))
(defn timetable-url  [line stop direction] (str api-url
                                                "/line/" line
                                                "/stop/" stop
                                                "/timetable?direction=" direction))

(defn get-http-call-map [uri on-success on-failure]
  {:method :get
   :uri uri
   :timeout 5000
   :headers cors-header
   :response-format (ajax/json-response-format {:keywords? true})
   :on-success [on-success]
   :on-failure [on-failure] })

(rf/reg-event-db
 ::initialize-db
 (fn [_ _] 
   db/default-db))

(rf/reg-event-fx
 ::process-lines-response
 (fn [{:keys [db]} [_ {:keys [JourneyPatterns]}]]
   ;; (println JourneyPatterns)
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
   ;; (println stops)
   {:db (assoc db :line-stops stops)
    :dispatch [::loading false]}))

(rf/reg-event-fx
 ::failed-stops
 (fn [{:keys [db]} [_ fail]]
   (println "fail" fail)
   {:dispatch [::loading false]}))

(rf/reg-event-fx
 ::process-stop-response
 (fn [{:keys [db]} [_ stop]]
   ;; (println stop)
   {:db (assoc db :stop stop)
    :dispatch [::loading false]}))

(rf/reg-event-fx
 ::failed-stop
 (fn [{:keys [db]} [_ fail]]
   (println "fail" fail)
   {:dispatch [::loading false]}))

(rf/reg-event-fx
 ::process-timetable-response
 (fn [{:keys [db]} [_ timetable]]
   ;; (println "tt" timetable)
   {:dispatch [::loading false]
    :db (assoc db :timetable timetable)}))

(rf/reg-event-fx
 ::failed-timetable
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
    :http-xhrio (get-http-call-map
                 (lines-url)
                 ::process-lines-response
                 ::failed-lines
                 )}))

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
    :http-xhrio (get-http-call-map
                 (lines-stop-url (:Code line) (:Direction line))
                 ::process-stops-response
                 ::failed-stops) 
    }))

(rf/reg-event-fx
 ::stop-click
 (fn [{:keys [db]} [_ stop]]
   {:dispatch [::loading true]
    :http-xhrio (get-http-call-map
                 (stop-url stop)
                 ::process-stop-response
                 ::failed-stop) 
    }))

(rf/reg-event-db
 ::back-click
 (fn [db [_ step]]
   (dissoc db step)))

(rf/reg-event-db
 ::sub-view
 (fn [db [_ sub-view]]
   (assoc db :sub-view sub-view)))

(rf/reg-event-fx
 ::timetable-click
 (fn [{:keys [db]} [_ stop line direction]]
   (println "stop" stop "line" line)
   {:dispatch-n [[::sub-view :timetable] [::loading true]]
    :http-xhrio (get-http-call-map
                 (timetable-url line stop direction)
                 ::process-timetable-response
                 ::failed-timetable)}))

(rf/reg-event-db
 ::other-lines-click
 (fn [db] 
   (assoc db :sub-view :other-lines)))
