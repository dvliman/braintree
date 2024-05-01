(ns braintree.main
  (:require
   [blancohugo.luhn :as luhn]
   [clojure.java.io :as io]
   [clojure.string :as str]))

(defn read-input [args]
  (cond
    (and (= 1 (count args)) (.canRead (io/file (first args))))
    (line-seq (io/reader (first args)))

    :else
    (line-seq (io/reader *in*))))

(defn money [x]
  (some-> x (str/split #"\$") second Integer/parseInt))

(defn add-account [db account-name attributes]
  (assoc db account-name attributes))

(defn lookup-account [db account-name]
  (get db account-name))

(defn update-account [db account-name changeset]
  (update db account-name (fn [attributes] (into attributes changeset))))

(defn log [message context]
  (str/join
   ", "
   (for [[k v] (into {:message message} context)]
     (str (name k) "=" v))))

(defn process [db parts]
  (let [[command account-name amount-or-card-number maybe-limit] parts
        [amount card-number] [(money amount-or-card-number) amount-or-card-number]]
    (condp = command
      "Add"
      (add-account db account-name
                   {:account-name account-name
                    :card-number (if (luhn/valid? card-number) card-number "error")
                    :limit (money maybe-limit)
                    :balance 0})

      "Charge"
      (when-let [{:keys [balance limit card-number]} (lookup-account db account-name)]
        (if-not (luhn/valid? card-number)
          db
          (if (> (+ balance amount) limit)
            (do
              (log "charging over limit, ignoring"
                   {:account-name account-name
                    :balance balance
                    :amount amount
                    :limit limit
                    :limit-after (+ balance amount)})
              db)
            (update-account db account-name {:balance (+ balance amount)}))))

      "Credit"
      (when-let [{:keys [balance card-number]} (lookup-account db account-name)]
        (if-not (luhn/valid? card-number)
          db
          (update-account db account-name {:balance (- balance amount)})))

      :else
      (do
        (log "invalid parts" {:parts parts :reason "don't know how to process"})
        db))))

(defn summary [db]
  (map
   (fn [[k {:keys [card-number balance]}]]
     (if (= card-number "error")
       (str k ": " card-number)
       (str k ": " "$" balance)))
   (into (sorted-map) db)))

(defn display-summary [summary]
  (doseq [s summary]
    (println s)))

(defn -main
  [& args]
  (->>
   args
   read-input
   (map #(str/split % #" "))
   (reduce process {})
   summary
   display-summary))
