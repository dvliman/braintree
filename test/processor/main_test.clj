(ns processor.main-test
  (:require
   [blancohugo.luhn :as luhn]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [processor.main :refer :all]))

(deftest luhn-algorithm-test
  ;; from the example: Tom, Lisa, Quincy
  ;; https://en.wikipedia.org/wiki/Luhn_algorithm
  (is (luhn/valid? "4111111111111111"))
  (is (luhn/valid? "5454545454545454"))
  (is (not (luhn/valid? "error")))
  (is (not (luhn/valid? "1234567890123456"))))

(deftest read-input-test
  (testing "read from input file"
    (let [args '("resources/sample-input.txt")]
      (is (= (read-input args)
             '("Add Tom 4111111111111111 $1000"
               "Add Lisa 5454545454545454 $3000"
               "Add Quincy 1234567890123456 $2000"
               "Charge Tom $500"
               "Charge Tom $800"
               "Charge Lisa $7"
               "Credit Lisa $100"
               "Credit Quincy $200")))))

  (testing "read from stdin"
    (binding [*in* (java.io.StringReader. "Add Tom 4111111111111111 $1000\nAdd Lisa 5454545454545454 $3000")]
      (is (= (read-input nil)
             '("Add Tom 4111111111111111 $1000" "Add Lisa 5454545454545454 $3000")))))

  (testing "parse line"
    (binding [*in* (java.io.StringReader. "Add Tom 4111111111111111 $1000\nCharge Tom $500\nCredit Lisa $100")]
      (= (->>
          (read-input nil)
          (map #(str/split % #" ")))
         [["Add" "Tom" "4111111111111111" "$1000"]
          ["Charge" "Tom" "$500"]
          ["Credit" "Lisa" "$100"]]))))

(deftest processor-test
  ;; Add
  (testing "adding new card, initial balance is 0"
    (is (as-> (process {} ["Add" "Tom" "4111111111111111" "$1000"]) result
          (get result "Tom")
          (:balance result)
          zero?)))

  (testing "showcase what data representation looks like"
    (is (= (process {} ["Add" "Tom" "4111111111111111" "$1000"])
           {"Tom" {:account-name "Tom" :card-number "4111111111111111" :limit 1000 :balance 0}})))

  (testing "card number should be validated with luhn 10"
    (is (nil? (as-> (process {} ["Add" "Tom" "1234567890123456" "$1000"]) result
                (get result "Tom")))))

  ;; Charge
  (testing "charge should increase balance by the amount specified"
    (is (= (as-> (->>
                  [["Add" "Tom" "4111111111111111" "$1000"] ;; balance 0
                   ["Charge" "Tom" "$500"]] ;; balance 500
                  (reduce process {})) result
             (get result "Tom")
             (:balance result))
           500)))

  (testing "charges over limit ignored as if declined"
    (is (= (as-> (->>
                  [["Add" "Tom" "4111111111111111" "$1000"] ;; balance 0
                   ["Charge" "Tom" "$500"]  ;; balance 500
                   ["Charge" "Tom" "$500"]  ;; balance 1000 (equal limit)
                   ["Charge" "Tom" "$500"]] ;; over limit, charge is ignored
                  (reduce process {})) result
             (get result "Tom")
             (:balance result))
           1000)))

  (testing "charges against invalid luhn 10 are ignored"
    (is (as-> (reduce
               process
                  ;; note: not allowed during add, this shouldn't happen
               {"Tom" {:account-name "Tom"
                       :card-number "4111111111111111"
                       :limit 1000
                       :balance 0}}
               [["Charge" "Tom" "$500"]]) result
          (get result "Tom")
          (:balance result)
          zero?)))

  ;; Credit
  (testing "credit should decrease balance by amount specified"
    (is (= (as-> (->>
                  [["Add" "Tom" "4111111111111111" "$1000"] ;; balance 0
                   ["Charge" "Tom" "$500"]  ;; balance 500
                   ["Credit" "Tom" "$100"]]  ;; balance 400
                  (reduce process {})) result
             (get result "Tom")
             (:balance result))
           400)))

  (testing "credit that would drop balance below 0 will create negative balance"
    (is (= (as-> (->>
                  [["Add" "Tom" "4111111111111111" "$1000"] ;; balance 0
                   ["Credit" "Tom" "$100"]]  ;; balance -100
                  (reduce process {})) result
             (get result "Tom")
             (:balance result))
           -100))))
