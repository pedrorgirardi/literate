# Literate

Literate is a graphical user interface extension for your Clojure REPL.
          
![Image of Yaktocat](https://github.com/pedrorgirardi/literate/raw/master/doc/Screen%20Shot%202020-04-04%20at%2012.44.44%20AM.png)

## Up and Running
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

