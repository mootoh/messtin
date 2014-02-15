package main

import (
	"fmt"
	"html"
	"log"
	"net/http"
	"strings"
)

func topHandler(w http.ResponseWriter, r *http.Request) {
	fmt.Fprintf(w, "Messtin")
}

func bookListHandler(w http.ResponseWriter, r *http.Request) {
	fmt.Fprintf(w, "book list")
}

func bookHandler(w http.ResponseWriter, r *http.Request) {
	var path = html.EscapeString(r.URL.Path)
	var comps = strings.Split(path, "/")
	var book = comps[len(comps)-2]
	var id = comps[len(comps)-1]

	log.Println("request to book: " + book + ", " + id)
	if id == "manifest" {
		http.ServeFile(w, r, "contents/"+book+".json")
		return
	}
	http.ServeFile(w, r, "contents/"+book+"/"+id+".jpg")
}

func main() {
	// route
	mux := http.NewServeMux()
	mux.Handle("/", http.HandlerFunc(topHandler))
	mux.Handle("/books", http.HandlerFunc(bookListHandler))
	mux.Handle("/book/", http.HandlerFunc(bookHandler))

	log.Fatal(http.ListenAndServe(":8080", mux))
}
