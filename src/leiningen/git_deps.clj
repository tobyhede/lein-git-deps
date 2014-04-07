(ns leiningen.git-deps
  "How this works: It clones projects into .lein-git-deps/<whatever>.
  If the directory already exists, it does a git pull and git checkout."
  (:require [clojure.java.shell :as sh]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [robert.hooke :as hooke]
            [leiningen.deps :as deps]
            [leiningen.core.project :as lein-project]))

;; Why, you might ask, are we using str here instead of simply def'ing
;; the var to a string directly? The answer is that we are working
;; around a bug in marginalia where it can't tell the difference
;; between the string that's the value for a def and a docstring. It
;; will hopefully be fixed RSN, because this makes us feel dirty.
(def ^{:private true
       :doc "The directory into which dependencies will be cloned."}
  git-deps-dir (str ".lein-git-deps"))

(defn- directory-exists?
  "Return true if the specified directory exists."
  [dir]
  (.isDirectory (io/file dir)))

(defn- default-clone-dir
  "Given a git URL, return the directory it would clone into by default."
  [uri]
  (string/join "." (-> uri
                       (string/split #"/")
                       (last)
                       (string/split #"\.")
                       butlast)))

(defn- exec
  "Run a command, throwing an exception if it fails, returning the
  result as with clojure.java.shell/sh."
  [& args]
  (let [{:keys [exit out err] :as result} (apply sh/sh args)]
    (if (zero? exit)
      result
      (throw
       (Exception.
        (format "Command %s failed with exit code %s\n%s\n%s"
                (apply str (interpose " " args))
                exit
                out
                err))))))

(defn- git-clone
  "Clone the git repository at url into dir-name while working in
  directory working-dir."
  [url dir-name working-dir]
  (apply exec (remove nil? ["git" "clone" url (str dir-name) :dir working-dir])))

(defn- git-checkout
  "Check out the specified commit in dir."
  [commit dir]
  (println "Running git checkout " commit " in " (str dir))
  (exec "git" "checkout" commit :dir dir))

(defn- git-submodule-init
  "Initalize submodules in the given dir"
  [dir]
  (println "Running git submodule init in " (str dir))
  (exec "git" "submodule" "init" :dir dir))

(defn- git-submodule-update
  "Update submodules in the given dir"
  [dir]
  (println "Running git submodule update in " (str dir))
  (exec "git" "submodule" "update" :dir dir))

(defn- detached-head?
  "Return true if the git repository in dir has HEAD detached."
  [dir]
  (let [{out :out} (exec "git" "branch" "--no-color" :dir dir)
        lines (string/split-lines out)
        current-branch (first (filter #(.startsWith % "*") lines))]
    (when-not current-branch
      (throw (Exception. "Unable to determine current branch")))
    (= current-branch "* (no branch)")))

(defn- git-pull
  "Run 'git-pull' in directory dir, but only if we're on a branch. If
  HEAD is detached, we only do a fetch, not a full pull."
  [dir]
  (println "Running git pull on " (str dir))
  (if (detached-head? dir)
    (do
      (println "Not on a branch, so fetching instead of pulling.")
      (exec "git" "fetch" :dir dir))
    (exec "git" "pull" :dir dir)))

(defn- git-dependencies
  "Return a map of the project's git dependencies."
  [project]
  (map (fn [dep]
      (let [[dep-url commit {clone-dir-name :dir src :src}] dep
            commit (or commit "master")
            clone-dir-name (or clone-dir-name (default-clone-dir dep-url))
            clone-dir (io/file git-deps-dir clone-dir-name)
            src (or src "src")]
        {:dep dep
         :dep-url dep-url
         :commit commit
         :clone-dir-name clone-dir-name
         :clone-dir clone-dir
         :src src}))
    (:git-dependencies project)))

(defn git-deps
  "A leiningen task that will pull dependencies in via git.

  Dependencies should be listed in project.clj under the
  :git-dependencies key in one of these three forms:

    :git-dependencies [;; First form: just a URL.
                       [\"https://github.com/foo/bar.git\"]

                       ;; Second form: A URL and a ref, which can be anything
                       ;; you can specify for 'git checkout', like a commit id
                       ;; or a branch name.
                       [\"https://github.com/foo/baz.git\"
                        \"329708b\"]

                       ;; Third form: A URL, a commit, and a map
                       ;; all keys in the map are optional
                       [\"https://github.com/foo/quux.git\"
                        \"some-branch\"
                        {:dir \"alternate-directory-to-clone-to\"
                         :src \"alternate-src-directory-within-repo\"}]]
"
  [project]
  (when-not (directory-exists? git-deps-dir)
    (.mkdir (io/file git-deps-dir)))
  (doseq [dep (git-dependencies project)]
    (let [{:keys [dep dep-url commit clone-dir-name clone-dir]} dep]
      (println "Setting up dependency for " dep)
      (if (directory-exists? clone-dir)
        (git-pull clone-dir)
        (git-clone dep-url clone-dir-name git-deps-dir))
      (git-checkout commit clone-dir)
      (git-submodule-init clone-dir)
      (git-submodule-update clone-dir))))

(defn hooks
  []
  (hooke/add-hook #'deps/deps (fn [task & args]
                                (apply task args)
                                (git-deps (first args)))))

(defn- add-source-paths
  [project dep]
  (let [dep-src (-> dep :clone-dir .getAbsolutePath (str "/" (:src dep)))]
    (update-in project [:source-paths] conj dep-src)))

(defn- add-dependencies
  [project dep]
  (let [dep-proj-path (-> dep :clone-dir .getAbsolutePath (str "/project.clj"))
        dep-proj (lein-project/read dep-proj-path)
        dep-deps (:dependencies dep-proj)]
    (update-in project [:dependencies] #(apply conj % dep-deps))))

(defn middleware
  [project]
  (let [deps (git-dependencies project)]
    (reduce add-source-paths (reduce add-dependencies project deps) deps)))
