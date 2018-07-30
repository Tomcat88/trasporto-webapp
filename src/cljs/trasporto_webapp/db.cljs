(ns trasporto-webapp.db)

(defn get-current-date-dow-key []
  (case (.getDay (js/Date.))
    0    :Sun
    1    :Mon
    2    :Tue
    3    :Wed
    4    :Thu
    5    :Fri
    6    :Sat ))

(defn get-current-hour []
  (.getHours (js/Date.)))

(def default-db
  {:query       nil
   :stops-query nil
   :lines       []
   :line-stops  {}
   :stop        nil
   :timetable   nil
   :sub-view    :other-lines
   :loading?    false
   :dow         (get-current-date-dow-key)
   :hour        (get-current-hour)})
