(ns flexiscram-spa.db)

(def default-db

  {:source nil
   :target nil

   ;; use sets to avoid de-duping on insertion
   :history {:source #{}
             :target #{}}

   :scramble? :undecided

   :user-error nil
   :lookup-error nil})