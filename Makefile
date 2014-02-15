PIDFILE=/tmp/messtin.pid

all: server

server: server.go
	go build server.go

run: server
	@if [ -f $(PIDFILE) ]; then \
		echo "killing existing server..."; \
		kill `cat $(PIDFILE)`; \
	fi
	./server & echo $$! > $(PIDFILE)