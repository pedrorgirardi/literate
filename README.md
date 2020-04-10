# Literate

Literate is a graphical user interface extension for your Clojure REPL.
          
![Image of Yaktocat](https://github.com/pedrorgirardi/literate/raw/master/doc/Literate.png)

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

