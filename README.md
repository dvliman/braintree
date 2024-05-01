
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

Run the tests 

```
# make test 

clj -M:test

Running tests in #{"test"}

Testing braintree.main-test

Ran 5 tests containing 16 assertions.
0 failures, 0 errors.
```

**Code Walkthrough**

Starts with the main entrypoint `braintree/-main`. We read input from either filepath or piped stdin. Then we process input line by line, one at a time. For each line/command, store and updates `{<name> {card: <card>, balance: <balance>, ...}, ...}`. Finally, print out the summary.

The test cases `braintree/main-test` exercises the flows with reasonable input assumptions documented.

Happy to walkthrough the code! 
