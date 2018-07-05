(ns flexiana-spa.db)

(def default-db

  {:source {:term  ""
            :error nil}

   :target {:term  ""
            :error nil}

   ;; we invert the relation for history because
   ;; the app seems to prefer that organization
   :history {:source #{}
             :target #{}}

   :inputs-ready? false
   :scramble? :undecided})

