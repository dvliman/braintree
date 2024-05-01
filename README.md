
Assume you have clojure installed i.e

```
❯ clj --version
Clojure CLI version 1.11.1.1200
```

You can run this program in two ways. One by passing the input file name

```
# make run

clj -M -m braintree.main resources/sample-input.txt
```

via stdin pipe

```
# make pipe

cat resources/sample-input.txt | clj -M -m braintree.main
```

