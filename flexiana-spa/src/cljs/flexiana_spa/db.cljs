(ns flexiana-spa.db)

(def default-db

  {:source nil

   :target nil

   :history {:source #{}
             :target #{}}

   :scramble? :undecided

   :user-error nil

   :lookup-error nil})

