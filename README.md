# Literate

Literate is a Clojure & ClojureScript application which you can use to visualize data.

![Literate](https://github.com/pedrorgirardi/literate/raw/master/doc/screenshot.png)

## Installation

```clojure
{literate {:git/url "https://github.com/pedrorgirardi/literate.git"
           :sha "3d2ea4c1f26937f6c7e2ddb81a8c19b1c8cd5dc0"}}
```

## Usage

```clojure
(require '[literate.server :as server])

(def stop-server (server/run-server {:port 8080}))

(require '[literate.core :as literate])
(require '[literate.widget :as widget])

(literate/view
  (widget/vega-lite {"$schema" "https://vega.github.io/schema/vega-lite/v4.json"
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
                                    :type "quantitative"}}}))

(literate/view
  (widget/hiccup [:span "Hiccup Snippet"]))
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

