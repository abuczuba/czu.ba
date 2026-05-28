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
      [:title "Matthew Czuba | Personal Website"]
      [:meta {:name "description" :content "Personal website of Matthew N. Czuba (楚孟修), software developer and NLP engineer."}]
      [:meta {:name "keywords" :content "Czuba, Matthew, Matthew Czuba, 楚孟修, Matthew N. Czuba, abuczuba, computational linguistics, melange, melange technologies, nlp, natural language processing"}]
      [:meta {:name "author" :content "Matthew N. Czuba"}]
      [:link {:rel "stylesheet" :href "https://fonts.googleapis.com/css2?family=Source+Code+Pro:wght@400;700&display=swap"}]
      [:link {:rel "stylesheet" :href "/css/normalize.css"}]
      [:link {:rel "stylesheet" :href "/css/sakura.css"}]]
     [:body
      [:div#container
       header
       [:main (h/raw content)]
       footer]]])))

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

;; Copy CV and resume to root for direct access
(fs/copy "content/static/czuba-resume-q2-2026.pdf" "public/czuba-resume-q2-2026.pdf")
(fs/copy "content/static/czuba-resume.pdf" "public/czuba-resume.pdf")
(println "Copied CV and resume to public root")

;; Create redirect pages for /cv and /resume
(spit "public/cv.html"
      "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><meta http-equiv=\"refresh\" content=\"0;url=/czuba-resume-q2-2026.pdf\"><title>Redirecting to CV</title></head><body><p>Redirecting to <a href=\"/czuba-resume-q2-2026.pdf\">CV</a>...</p></body></html>")
(spit "public/resume.html"
      "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><meta http-equiv=\"refresh\" content=\"0;url=/czuba-resume-q2-2026.pdf\"><title>Redirecting to Resume</title></head><body><p>Redirecting to <a href=\"/czuba-resume-q2-2026.pdf\">resume</a>...</p></body></html>")
(println "Created redirect pages for /cv and /resume")
