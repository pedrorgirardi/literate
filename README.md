# Literate

Literate is a graphical user interface extension for your Clojure REPL.
          
![Image of Yaktocat](https://github.com/pedrorgirardi/literate/raw/master/doc/screen-shot.png)

## Installation

```clojure
{literate {:git/url "https://github.com/pedrorgirardi/literate.git"
           :sha "35d3304f8333ed16c11a8b52f96c5f45d7d75241"}}
```

## Usage

```clojure
(require '[literate.server :as server])

(def stop-server (server/run-server {:port 8080}))

(require '[literate.core :as l])

(l/vega-lite {"$schema" "https://vega.github.io/schema/vega-lite/v4.json"
                :description "A simple bar chart with embedded data."
                :data {:values
                       [{:a "A" :b 28}
                        {:a "B" :b 55}
                        {:a "C" :b 43}
                        {:a "D" :b 91}
                        {:a "E" :b 81}
                        {:a "F" :b 53}
                        {:a "G" :b 19}
                        {:a "H" :b 87}
                        {:a "I" :b 52}]}
                :mark "bar"
                :encoding {:x {:field "a"
                               :type "ordinal"}
                           :y {:field "b"
                               :type "quantitative"}}})

(l/hiccup [:span "Hiccup Snippet"])
```

## Development
### Client
```
npm install
npm run watch
```

### Server
```clojure
(require '[literate.server :as server])
(server/run-server {:port 8090})
```

