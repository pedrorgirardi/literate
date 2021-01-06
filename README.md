# Literate

Literate is a Clojure & ClojureScript application which you can use to visualize data.

![Literate](https://github.com/pedrorgirardi/literate/raw/master/doc/screenshot.png)

Literate is essentially a collection of Widgets, and a Widget is a visual component, or renderer, for a particular '
type' of data.

It's a tricky thing to define 'type' of data, but I will show you some examples, and hopefully, it will make things more
straightforward.

Our example is a collection of maps; it's easy to think about this data in a table, let's say, like this one:

| A | B |
|:--|:--|
| X | 1 |
| Y | 3 |

In Literate, what you could do is view this same data but in a Vega-Lite Bar chart, for instance. And this is
accomplished by Widgets, a Vega-Lite Widget in particular:

```clojure
(literate/vega-lite
  {"$schema" "https://vega.github.io/schema/vega-lite/v4.json"
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
```

![Vega-Lite Widget](https://github.com/pedrorgirardi/literate/raw/master/doc/vega_lite_widget.png)

We are not too bad at making sense of tabular data. Still, if you happen to work with spatial data, it isn't easy to
make sense without a graphical representation. It's similar to talking about geometry without drawing the shapes on a
piece of paper. Very hard.

For spatial data, you could view it in a Vega-Lite Widget too (Vega is capable of many great things), but there's also
the Leaflet Widget:

```clojure
(literate/leaflet
  {:style {:height "600px"}
   :center center
   :zoom 10
   :geojson geojson})
```

![Leaflet Widget](https://github.com/pedrorgirardi/literate/raw/master/doc/leaflet_widget.png)

You see, it's tricky to define 'type' of data because it can take many different shapes and forms. But the idea is the
same: Widgets take data in and present it visually.

## How it works

The Widget API is embarrassingly dummy; whenever you call a Widget function, you are merely creating an entity:

```clojure
(literate/vega-lite
  {"$schema" "https://vega.github.io/schema/vega-lite/v4.json"
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

;; ==>

#:widget{:uuid "7385f5da-f835-49b5-8d8c-005f92b77d11",
         :type :widget.type/vega-lite,
         :vega-lite-spec {"$schema" "https://vega.github.io/schema/vega-lite/v4.json",
                          :description "A simple bar chart with embedded data.",
                          :data {:values [{:a "A", :b 28}
                                          {:a "B", :b 55}
                                          {:a "C", :b 43}
                                          {:a "D", :b 91}
                                          {:a "E", :b 81}
                                          {:a "F", :b 53}
                                          {:a "G", :b 19}
                                          {:a "H", :b 87}
                                          {:a "I", :b 52}]},
                          :mark "bar",
                          :encoding {:x {:field "a", :type "ordinal"}, :y {:field "b", :type "quantitative"}}}}
```

Literate ClojureScript app is a (DataScript) database of Widgets. Everything you see in the app is stored in DataScript, and it's a Widget entity. No special case. 

Every Widget entity has an attribute to describe its type (I'm sorry for the overloaded use of this word.), and for each known type, there is a Widget renderer.

## Installation

```clojure
{pedrorgirardi/literate {:git/url "https://github.com/pedrorgirardi/literate.git"
                         :sha "34ace4589966cbd69c497acddc4d2fb94252bcc4"}}
```

## Usage

```clojure
(require '[literate.server :as server])

(def stop-server (server/run-server {:port 8080}))

(require '[literate.core :as literate])

(literate/view
  (literate/vega-lite {"$schema" "https://vega.github.io/schema/vega-lite/v4.json"
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
  (literate/hiccup [:span "Hiccup Snippet"]))
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

