.PHONY: run
run:
	clj -M -m braintree.main resources/sample-input.txt

.PHONY: pipe
pipe:
	cat resources/sample-input.txt | clj -M -m braintree.main
