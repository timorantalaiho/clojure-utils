(ns oph.common.infra.common-audit-log-test
  (:require [clojure.test :refer [deftest testing is are]]
            [cheshire.core :as json]
            [clj-time.core :as time]
            [clj-time.local :as time-local]
            [oph.common.infra.common-audit-log :refer :all]))

(def ^:private boot-time (time-local/local-now))   ;; (time/local-date-time 1980 9 20 1 2 3 123)
(def ^:private validi-meta {:boot-time        boot-time
                            :hostname         "host"
                            :service-name     "aitu"
                            :application-type "virkailija"})

(deftest auditlogitus-test

  (testing "environment metaa ei ole annettu"
;    (is (thrown? AssertionError (->audit-log-entry {})))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Value does not match schema" (->audit-log-entry {})))
    )

  (testing "environment meta on epävalidi"
    (konfiguroi-common-audit-lokitus {:service-name "aitu"})
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Value does not match schema" (->audit-log-entry {})))
    )

  (testing "meta annettu mutta logisisältö puuttuu"
    (konfiguroi-common-audit-lokitus validi-meta)
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Value does not match schema" (->audit-log-entry {})))
    )

  (testing "kaikki kentät annettu"
    (konfiguroi-common-audit-lokitus validi-meta)
    (let [resp (->audit-log-entry {:operation :paivitys
                                   :user {:oid        "henkiloOid"
;                                          :ip         "127.0.0.1"
;                                          :session    "124uih23u124"
;                                          :user-agent "Apache-HttpClient/4.5.2(Java/1.8.0_121)"
                                          }
                                   :resource "järjestämissopimus"
                                   :resourceOid "sopimusOid"
                                   :id "paa-avain"
                                   :delta [{:op "päivitys"
                                            :path "alkupvm"
                                            :value (time/local-date 2009 8 1)}
                                           {:op "päivitys"
                                            :path "loppupvm"
                                            :value (time/local-date 2009 7 31)}]
                                   :message "Tämä on viesti."})
          ]
      ;; Ohitetaan muuttuvan "timestamp"-arvon tarkastelu. Myös userin ip:tä, sessiota ja user-agenttia on tässä hankala testata, sillä ne otetaan livenä ring-requestista.
      (is (and
            (.contains resp
              "\"operation\":\"päivitys\",\"type\":\"log\",\"hostname\":\"host\",\"applicationType\":\"virkailija\"")
            (.contains resp
              "\"delta\":[{\"op\":\"päivitys\",\"path\":\"alkupvm\",\"value\":\"01.08.2009\"},{\"op\":\"päivitys\",\"path\":\"loppupvm\",\"value\":\"31.07.2009\"}],\"logSeq\":1")
            (.contains resp
              "\"target\":{\"järjestämissopimus\":\"sopimusOid\",\"id\":\"paa-avain\"},\"serviceName\":\"aitu\",\"version\":1")
            (.contains resp
              "\"user\":{\"oid\":\"henkiloOid\"")
            (.contains resp
              "\"message\":\"Tämä on viesti.\"")
            (.contains resp (json/generate-string boot-time))))
      ))
  )
