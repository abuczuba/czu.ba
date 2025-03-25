(ns build
  (:require [babashka.fs :as fs]
            [babashka.process :as pro]
            [hiccup2.core :as h]))

(def header (h/raw (slurp "templates/header.html")))
(def footer (h/raw (slurp "templates/footer.html")))

(defn- out-file-name
  "Generate an output file name from an input file name."
  [file]
  (let [basename (-> file fs/file-name fs/strip-ext)]
    (str "public/" basename ".html")))

(defn- html-content-str
  "Generate html content from input file using pandoc."
  [file]
  (:out
   (pro/shell {:out :string}
              "pandoc" "-i" file
              "--from=org-auto_identifiers"
              "--to=html"
              "--filter=filter.clj")))

(defn- html-file-str
  "Wrap content in header and footer to create a standalone file."
  [content]
  (str
   (h/html
    (h/raw "<!DOCTYPE html>")
    [:html {:lang "en"}
     [:head
      [:meta {:charset "utf-8"}]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
      [:link {:rel "stylesheet" :href "https://fonts.googleapis.com/css2?family=Source+Code+Pro:wght@400;700&display=swap"}]
      [:link {:rel "stylesheet" :href "/css/normalize.css"}]
      [:link {:rel "stylesheet" :href "/css/sakura.css"}]]
     [:body
      [:div#container
       header
       [:main (h/raw content)]
       footer]
      [:script
       "document.addEventListener('DOMContentLoaded', function() {
          const currentPath = window.location.pathname;
          const navLinks = document.querySelectorAll('.nav-link');
          navLinks.forEach(link => {
            const href = link.getAttribute('href');
            if ((currentPath === '/' && href === '/') || 
                (href !== '/' && currentPath.endsWith(href))) {
              link.classList.add('active');
            }
          });
        }"]]])))

(doseq [file (fs/list-dir "content")]
  (when (= (fs/extension file) "org")
    (let [out (out-file-name file)]
      (->> file
           html-content-str
           html-file-str
           (spit out))
      (println "Wrote" out))))

(fs/copy-tree "content/static" "public/static" {:replace-existing true})
(println "Copied over static artifacts")
