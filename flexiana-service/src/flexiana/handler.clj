(ns flexiana.handler
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.json :refer [wrap-json-response]]
            [flexiana.scramble :refer [handle-scramble?]]))

(defroutes app-routes
  (GET "/" request
    {:status  400
     :headers {"Content-Type" "text/html"}
     :body    (str "Invalid request, viz.:<p>" request)})

  (GET "/scramblep" req
    (handle-scramble? req))

  (route/not-found "Invalid route toplevel."))

(def app
  (->
    (wrap-defaults app-routes site-defaults)
    wrap-json-response))

;(def app
;  (-> (handler/site app-routes)
;    (wrap-json-body {:keywords? true})
;    wrap-json-response))
