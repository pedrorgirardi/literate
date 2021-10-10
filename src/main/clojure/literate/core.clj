(ns literate.core
  (:require [literate.server :as server]
            [clojure.tools.cli :as cli]))

(def cli-options
  [["-p" "--port PORT" "Port number"
    :default 8118
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]])

(defn -main [& args]
  (let [opts (cli/parse-opts args cli-options)
        port (get-in opts [:options :port])]

    (println
      (str "Welcome to Literate\n"
           "Starting server..."))

    (server/run-server {:port port})

    (println "Server is up and running on port:" port)
    (println "Happy coding!")))
