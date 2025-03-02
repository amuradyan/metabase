(ns metabase-enterprise.sandbox.test-util
  "Shared test utilities for sandbox tests."
  (:require
   [malli.core :as mc]
   [mb.hawk.parallel]
   [metabase.permissions.models.data-permissions :as data-perms]
   [metabase.request.core :as request]
   [metabase.test :as mt]
   [metabase.test.data :as data]
   [metabase.test.data.impl :as data.impl]
   [metabase.test.data.users :as test.users]
   [metabase.test.util :as tu]
   [metabase.util :as u]))

(defn do-with-user-attributes! [test-user-name-or-user-id attributes-map thunk]
  (mb.hawk.parallel/assert-test-is-not-parallel "with-user-attributes!")
  (let [user-id (test.users/test-user-name-or-user-id->user-id test-user-name-or-user-id)]
    (tu/with-temp-vals-in-db :model/User user-id {:login_attributes attributes-map}
      (thunk))))

(defmacro with-user-attributes!
  "Execute `body` with the attributes for a User temporarily set to `attributes-map`. `test-user-name-or-user-id` can be
  either one of the predefined test users e.g. `:rasta` or a User ID.

    (with-user-attributes! :rasta {\"cans\" 2} ...)"
  {:style/indent 2}
  [test-user-name-or-user-id attributes-map & body]
  `(do-with-user-attributes! ~test-user-name-or-user-id ~attributes-map (fn [] ~@body)))

(defn- do-with-gtap-defs!
  [group [[table-kw {:keys [query remappings]} :as gtap-def] & more] f]
  (if-not gtap-def
    (f)
    (let [do-with-card (fn [f]
                         (if query
                           (mt/with-temp [:model/Card {card-id :id} {:dataset_query query}]
                             (f card-id))
                           (f nil)))]
      (do-with-card
       (fn [card-id]
         (mt/with-temp [:model/GroupTableAccessPolicy _gtap {:group_id             (u/the-id group)
                                                             :table_id             (data/id table-kw)
                                                             :card_id              card-id
                                                             :attribute_remappings remappings}]
           (data-perms/set-database-permission! group (data/id) :perms/view-data :unrestricted)
           (data-perms/set-table-permission! group (data/id table-kw) :perms/create-queries :query-builder)
           (do-with-gtap-defs! group more f)))))))

(def ^:private WithGTAPsArgs
  "Schema for valid arguments to `with-gtaps`."
  [:map
   [:gtaps [:map-of :keyword [:maybe
                              [:map
                               [:query      {:optional true} :map]
                               [:remappings {:optional true} :map]]]]]
   [:attributes {:optional true} :map]])

(defn do-with-gtaps-for-user! [args-fn test-user-name-or-user-id f]
  (mb.hawk.parallel/assert-test-is-not-parallel "with-gtaps-for-user!")
  (letfn [(thunk []
            (mt/with-no-data-perms-for-all-users!
              ;; create new perms group
              (test.users/with-group-for-user [group test-user-name-or-user-id]
                (let [{:keys [gtaps attributes]} (mc/assert WithGTAPsArgs (args-fn))]
                  ;; set user login_attributes
                  (with-user-attributes! test-user-name-or-user-id attributes
                    (mt/with-additional-premium-features #{:sandboxes}
                      ;; create Cards/GTAPs from defs
                      (do-with-gtap-defs!
                       group gtaps
                       (fn []
                         ;; bind user as current user, then run f
                         (if (keyword? test-user-name-or-user-id)
                           (test.users/with-test-user test-user-name-or-user-id
                             (f group))
                           (request/with-current-user (u/the-id test-user-name-or-user-id)
                             (f group)))))))))))]
    ;; create a temp copy of the current DB if we haven't already created one. If one is already created, keep using
    ;; that so we can test multiple sandboxed users against the same DB
    (if data.impl/*db-is-temp-copy?*
      (thunk)
      (data/with-temp-copy-of-db
        (thunk)))))

(defmacro with-gtaps-for-user!
  "Like `with-gtaps!`, but for an arbitrary User. `test-user-name-or-user-id` can be a predefined test user e.g. `:rasta`
  or an arbitrary User ID."
  {:style/indent :defn}
  [test-user-name-or-user-id gtaps-and-attributes-map & body]
  `(do-with-gtaps-for-user! (fn [] ~gtaps-and-attributes-map) ~test-user-name-or-user-id (fn [~'&group] ~@body)))

(defmacro with-gtaps!
  "Execute `body` with `gtaps` and optionally user `attributes` in effect, for the :rasta test user. All underlying
  objects and permissions are created automatically.

  `gtaps-and-attributes-map` is a map containing `:gtaps` and optionally `:attributes`; see the `WithGTAPsArgs` schema
  in this namespace.

  *  `:gtaps` is a map of test ID table name -> gtap def. Both `:query` and `:remappings` are optional.

  *  If `:query` is specified, a corresponding Card is created, and the GTAP is saved with that `:card_id`.
     Otherwise Card ID is nil and the GTAP uses the source table directly.

  *  `:remappings`, if specified, is saved as the `:attribute_remappings` property of the GTAP.

    (met/with-gtaps! {:gtaps      {:checkins {:query      {:database (data/id), ...}
                                              :remappings {:user_category [\"variable\" ...]}}}
                     :attributes {\"user_category\" 1}}
      (mt/run-mbql-query checkins {:limit 2}))

  Introduces `&group` anaphor, bound to the PermissionsGroup associated with this GTAP."
  {:style/indent :defn}
  [gtaps-and-attributes-map & body]
  `(do-with-gtaps-for-user! (fn [] ~gtaps-and-attributes-map) :rasta (fn [~'&group] ~@body)))

(defn restricted-column-query
  "An MBQL query against Venues that only returns a subset of the columns."
  [db-id]
  {:database db-id
   :type     :query
   :query    (data/$ids venues
               {:source_table $$venues
                :fields       [$id
                               $name
                               $category_id]})})
