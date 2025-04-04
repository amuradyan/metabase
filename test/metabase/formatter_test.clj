(ns metabase.formatter-test
  (:refer-clojure :exclude [format])
  (:require
   [clojure.test :refer :all]
   [metabase.formatter :as formatter]
   [metabase.models.visualization-settings :as mb.viz]))

(defn format [value viz]
  (str ((formatter/number-formatter {:id 1}
                                    {::mb.viz/column-settings
                                     {{::mb.viz/field-id 1} viz}})
        value)))

(deftest number-formatting-test
  (let [value 12345.5432
        fmt   (partial format value)]
    (testing "Regular Number formatting"
      (is (= "12,345.54" (fmt nil)))
      (is (= "12*345^54" (fmt {::mb.viz/number-separators "^*"})))
      (is (= "prefix12,345.54suffix" (fmt {::mb.viz/prefix "prefix"
                                           ::mb.viz/suffix "suffix"})))
      (is (= "12,345.54" (fmt {::mb.viz/decimals 2})))
      (is (= "12,345.5432000" (fmt {::mb.viz/decimals 7})))
      (is (= "12,346" (fmt {::mb.viz/decimals 0})))
      (is (= "2" (format 2 nil))))
    (testing "Currency"
      (testing "defaults to USD and two decimal places and symbol"
        (is (= "$12,345.54" (fmt {::mb.viz/number-style       "currency"
                                  ::mb.viz/currency-in-header false}))))
      (testing "Defaults to currency when there is a currency style"
        (is (= "$12,345.54" (fmt {::mb.viz/currency-style     "symbol"
                                  ::mb.viz/currency-in-header false}))))
      (testing "Defaults to currency when there is a currency"
        (is (= "$12,345.54" (fmt {::mb.viz/currency           "USD"
                                  ::mb.viz/currency-in-header false}))))
      (testing "respects the number of decimal places when specified"
        (is (= "$12,345.54320" (fmt {::mb.viz/currency           "USD"
                                     ::mb.viz/decimals           5
                                     ::mb.viz/currency-in-header false}))))
      (testing "Other currencies"
        (is (= "AED12,345.54" (fmt {::mb.viz/currency           "AED"
                                    ::mb.viz/currency-in-header false})))
        (is (= "12,345.54 Cape Verdean escudos"
               (fmt {::mb.viz/currency           "CVE"
                     ::mb.viz/currency-style     "name"
                     ::mb.viz/currency-in-header false})))
        (testing "which have no 'cents' and thus no decimal places"
          (is (= "Af12,346" (fmt {::mb.viz/currency           "AFN"
                                  ::mb.viz/currency-in-header false})))
          (is (= "₡12,346" (fmt {::mb.viz/currency           "CRC"
                                 ::mb.viz/currency-in-header false})))
          (is (= "ZK12,346" (fmt {::mb.viz/currency           "ZMK"
                                  ::mb.viz/currency-in-header false})))))
      (testing "Understands name, code, and symbol"
        (doseq [[style expected] [["name" "12,345.54 Czech Republic korunas"]
                                  ["symbol" "Kč12,345.54"]
                                  ["code" "CZK 12,345.54"]]]
          (is (= expected (fmt {::mb.viz/currency           "CZK"
                                ::mb.viz/currency-style     style
                                ::mb.viz/currency-in-header false}))
              style))))
    (testing "scientific notation"
      (is (= "1.23E4" (fmt {::mb.viz/number-style "scientific"})))
      (is (= "1.23E4" (fmt {::mb.viz/number-style "scientific"
                            ::mb.viz/decimals     2})))
      (is (= "1.2346E4" (fmt {::mb.viz/number-style "scientific"
                              ::mb.viz/decimals     4})))
      (is (= "1E0" (format 1 {::mb.viz/number-style "scientific"})))
      (is (= "1.2E1" (format 12 {::mb.viz/number-style "scientific"})))
      (is (= "1.23E2" (format 123 {::mb.viz/number-style "scientific"})))
      (is (= "1.23E3" (format 1234 {::mb.viz/number-style "scientific"})))
      (is (= "1.23E4" (format 12345 {::mb.viz/number-style "scientific"})))
      (is (= "-1E0" (format -1 {::mb.viz/number-style "scientific"})))
      (is (= "-1.2E1" (format -12 {::mb.viz/number-style "scientific"})))
      (is (= "-1.23E2" (format -123 {::mb.viz/number-style "scientific"})))
      (is (= "-1.23E3" (format -1234 {::mb.viz/number-style "scientific"})))
      (is (= "-1.23E4" (format -12345 {::mb.viz/number-style "scientific"}))))
    (testing "Percentage"
      (is (= "1,234,554.32%" (fmt {::mb.viz/number-style "percent"})))
      (is (= "1.234.554,3200%"
             (fmt {::mb.viz/number-style      "percent"
                   ::mb.viz/decimals          4
                   ::mb.viz/number-separators ",."})))
      (is (= "10%" (format 0.1 {::mb.viz/number-style "percent"})))
      (is (= "1%" (format 0.01 {::mb.viz/number-style "percent"})))
      (is (= "0%" (format 0.000000 {::mb.viz/number-style "percent"})))
      ;; With default formatting (2 digits) and zero trimming, we get 0%
      (is (= "0%" (format 0.0000001 {::mb.viz/number-style "percent"})))
      ;; Requiring 2 digits adds zeros
      (is (= "0.00%" (format 0.0000001 {::mb.viz/number-style "percent"
                                        ::mb.viz/decimals     2})))
      ;; You need at least 5 digits (not the scale by 100 for percents) to show the low value
      (is (= "0.00001%" (format 0.0000001 {::mb.viz/number-style "percent"
                                           ::mb.viz/decimals     5}))))
    (testing "Match UI 'natural formatting' behavior for decimal values with no column formatting present"
      ;; basically, for numbers greater than 1, round to 2 decimal places,
      ;; and do not display decimals if they end up as zeroes
      ;; for numbers less than 1, round to 2 significant-figures,
      ;; and show as many decimals as necessary to display these 2 sig-figs
      (is (= ["2"    "0"]       [(format 2 nil)       (format 0 nil)]))
      (is (= ["2.1"  "0.1"]     [(format 2.1 nil)     (format 0.1 nil)]))
      (is (= ["0.57" "-0.57"]   [(format 0.57 nil)    (format -0.57 nil)]))
      (is (= ["2.57" "-2.57"]   [(format 2.57 nil)    (format -2.57 nil)]))
      (is (= ["-0.22" "-1.34"]  [(format -0.2222 nil) (format -1.345 nil)]))
      (is (= ["2.01" "0.01"]    [(format 2.01 nil)    (format 0.01 nil)]))
      (is (= ["2"    "0.001"]   [(format 2.001 nil)   (format 0.001 nil)]))
      (is (= ["2.01" "0.006"]   [(format 2.006 nil)   (format 0.006 nil)]))
      (is (= ["2"    "0.0049"]  [(format 2.0049 nil)  (format 0.0049 nil)]))
      (is (= ["2"    "0.005"]   [(format 2.00499 nil) (format 0.00499 nil)]))
      ;; Test small numbers with many decimal places
      (is (= ["2"    "0.0014"]  [(format 2.0013593702702702702M nil)
                                 (format 0.0013593702702702702M nil)]))
      (is (= ["2"    "0.000012"] [(format 2.0000123456789M nil)
                                  (format 0.0000123456789M nil)])))
    (testing "Column Settings"
      (letfn [(fmt-with-type
                ([type value] (fmt-with-type type value nil))
                ([type value decimals]
                 (let [fmt-fn (formatter/number-formatter {:id 1 :effective_type type}
                                                          {::mb.viz/column-settings
                                                           {{::mb.viz/field-id 1}
                                                            (merge
                                                             {:effective_type type}
                                                             (when decimals {::mb.viz/decimals decimals}))}})]
                   (str (fmt-fn value)))))]
        (is (= "3" (fmt-with-type :type/Integer 3)))
        (is (= "3" (fmt-with-type :type/Integer 3.0)))
        (is (= "3.0" (fmt-with-type :type/Integer 3 1)))
        (is (= "3" (fmt-with-type :type/Decimal 3)))
        (is (= "3" (fmt-with-type :type/Decimal 3.0)))
        (is (= "3.1" (fmt-with-type :type/Decimal 3.1)))
        (is (= "3.01" (fmt-with-type :type/Decimal 3.010)))
        (is (= "0.25" (fmt-with-type :type/Decimal 0.254)))))
    (testing "Relation types do not do special formatting"
      (letfn [(fmt-with-type
                ([type value] (fmt-with-type type value nil))
                ([type value decimals]
                 (let [fmt-fn (formatter/number-formatter {:id 1 :semantic_type type}
                                                          {::mb.viz/column-settings
                                                           {{::mb.viz/field-id 1}
                                                            (merge
                                                             {:effective_type type}
                                                             (when decimals {::mb.viz/decimals decimals}))}})]
                   (str (fmt-fn value)))))]
        (is (= "1000" (fmt-with-type :type/PK 1000)))
        (is (= "1000" (fmt-with-type :type/FK 1000)))))
    (testing "Does not throw on nils"
      (is (nil?
           ((formatter/number-formatter {:id 1}
                                        {::mb.viz/column-settings
                                         {{::mb.viz/column-id 1}
                                          {::mb.viz/number-style "percent"}}})
            nil))))
    (testing "Does not throw on non-numeric types"
      (is (= "bob"
             ((formatter/number-formatter {:id 1}
                                          {::mb.viz/column-settings
                                           {{::mb.viz/column-id 1}
                                            {::mb.viz/number-style "percent"}}})
              "bob"))))))

(deftest coords-formatting-test
  (testing "Test the correctness of formatting longitude and latitude values"
    (is (= "12.34560000° E"
           (formatter/format-geographic-coordinates :type/Longitude 12.3456)))
    (is (= "12.34560000° W"
           (formatter/format-geographic-coordinates :type/Longitude -12.3456)))
    (is (= "12.34560000° N"
           (formatter/format-geographic-coordinates :type/Latitude 12.3456)))
    (is (= "12.34560000° S"
           (formatter/format-geographic-coordinates :type/Latitude -12.3456)))
    (testing "0 corresponds to the non-negative direction"
      (is (= "0.00000000° E"
             (formatter/format-geographic-coordinates :type/Longitude 0)))
      (is (= "0.00000000° N"
             (formatter/format-geographic-coordinates :type/Latitude 0))))
    (testing "A non-coordinate type just stringifies the value"
      (is (= "0.0"
             (formatter/format-geographic-coordinates :type/Froobitude 0))))
    (testing "We handle missing values"
      (is (= ""
             (formatter/format-geographic-coordinates :type/Longitude nil))))))

(deftest ambiguous-column-types-test
  (testing "Ambiguous column types (eg. `:type/SnowflakeVariant` pass through the formatter without error (#46981)"
    (let [format (fn [value viz]
                   (str ((formatter/number-formatter {:id 1
                                                      :base_type :type/SnowflakeVariant}
                                                     {::mb.viz/column-settings
                                                      {{::mb.viz/field-id 1} viz}})
                         value)))]
      (is (= "variant works"
             (format "variant works" {}))))))
