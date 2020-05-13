(ns clojars.routes.token
  (:require
   [clojars.auth :as auth]
   [clojars.db :as db]
   [clojars.web.token :as view]
   [compojure.core :as compojure :refer [GET POST DELETE]]
   [ring.util.response :refer [redirect]]))

(defn- get-tokens [db flash-msg]
  (auth/with-account
    #(view/show-tokens % (db/find-user-tokens-by-username db %)
                       {:message flash-msg})))

(defn- create-token [db token-name]
  (auth/with-account
    (fn [account]
      (let [token (db/add-deploy-token db account token-name)]
        (view/show-tokens account (db/find-user-tokens-by-username db account)
                          {:new-token token})))))

(defn- find-token [db token-id]
  (when-let [id-int (try
                      (Integer/parseInt token-id)
                      (catch Exception _))]
    (db/find-token db id-int)))

(defn- disable-token [db token-id]
  (auth/with-account
   (fn [account]
     (let [token (find-token db token-id)
           user (db/find-user db account)
           found? (and token
                       (= (:user_id token)
                          (:id user)))]
       (db/disable-deploy-token db (:id token))
       (assoc (redirect "/tokens")
              :flash (if found?
                       (format "Token '%s' disabled." (:name token))
                       "Token not found."))))))

(defn routes [db]
  (compojure/routes
   (GET ["/tokens"] {:keys [flash]}
        (get-tokens db flash))
   (POST ["/tokens"] [name]
         (create-token db name))
   (DELETE ["/tokens/:id", :id #"[0-9]+"] [id]
           (disable-token db id))))