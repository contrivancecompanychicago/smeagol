(ns ^{:doc "Render all the main pages of a very simple Wiki engine."
      :author "Simon Brooke"}
  smeagol.routes.admin
  (:require [clojure.walk :refer :all]
            [noir.session :as session]
            [taoensso.timbre :as timbre]
            [smeagol.authenticate :as auth]
            [smeagol.layout :as layout]
            [smeagol.util :as util]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;
;;;; Smeagol: a very simple Wiki engine.
;;;;
;;;; This program is free software; you can redistribute it and/or
;;;; modify it under the terms of the GNU General Public License
;;;; as published by the Free Software Foundation; either version 2
;;;; of the License, or (at your option) any later version.
;;;;
;;;; This program is distributed in the hope that it will be useful,
;;;; but WITHOUT ANY WARRANTY; without even the implied warranty of
;;;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
;;;; GNU General Public License for more details.
;;;;
;;;; You should have received a copy of the GNU General Public License
;;;; along with this program; if not, write to the Free Software
;;;; Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
;;;; USA.
;;;;
;;;; Copyright (C) 2016 Simon Brooke
;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn edit-users
  "Render a page showing a list of users for editing."
  [request]
  (let [params (keywordize-keys (:params request))
        user (session/get :user)]
    (layout/render "edit-users.html"
                   (merge (util/standard-params request)
                          {:title (:edit-users-title (util/get-messages request))
                           :users (auth/list-users)}))))

(defn delete-user
  "Render a form allowing a user to be deleted; and
  process that form.."
  [request]
  (let [params (keywordize-keys (:params request))
        target (:target params)
        deleted (auth/delete-user target)
        message (if deleted (str (:del-user-success (util/get-messages request)) " " target "."))
        error (if (not deleted) (str (:del-user-fail (util/get-messages request)) " " target "."))]
    (layout/render "edit-users.html"
                   (merge (util/standard-params request)
                          {:title (:edit-users-title (util/get-messages request))
                           :message message
                           :error error
                           :users (auth/list-users)}))))


(defn edit-user
  "Render a form showing an individual user's details for editing; and
  process that form."
  [request]
  (let [params (keywordize-keys (:params request))]
    (try
      (let [target (or (:target params) "")
            pass1 (:pass1 params)
            pass2 (:pass2 params)
            check-pass (auth/evaluate-password pass1 pass2)
            password (if (and pass1 (true? check-pass)) pass1)
            stored (if
                     (and
                      (:email params)
                      (or
                       (nil? pass1)
                       (zero? (count pass1))
                       (true? check-pass)))
                     (auth/add-user target password (:email params) (:admin params)))
            message (if stored (str (:save-user-success (util/get-messages request)) " " target "."))
            error (if
                    (and (:email params) (not stored))
                    (str
                      (:save-user-fail (util/get-messages request))
                      " " target ". "
                      (if (keyword? check-pass) (check-pass (util/get-messages request)))))
            page (if stored "edit-users.html" "edit-user.html")
            details (auth/fetch-user-details target)]
        (if message
          (timbre/info message))
        (if error
          (timbre/warn error))
        (layout/render page
                       (merge (util/standard-params request)
                              {:title (str (:edit-title-prefix (util/get-messages request)) " " target)
                               :message message
                               :error error
                               :target target
                               :details details
                               :users (auth/list-users)})))
      (catch Exception any
        (timbre/error any)
        (layout/render "edit-user.html"
                       (merge (util/standard-params request)
                              {:title (str (:edit-title-prefix (util/get-messages request)) " " (:target params))
                               :error (.getMessage any)
                               :target (:target params)
                               :details {:email (:email params) :admin (:admin params)}}))))))
