lein-git-deps
============

A Leiningen task that will pull dependencies in via git.

Original code extracted from the excellent ClojureScript One Project:

https://github.com/brentonashworth/one
https://github.com/brentonashworth/one/blob/master/src/leiningen/git_deps.clj

Usage
====

Dependencies should be listed in project.clj under the ":git-dependencies" key

Dependencies can be provided as:

 * url
 * url & any param applicable for 'git checkout', like a commit id or a branch name.
 * url, commit, and a map of options


To get the system to play nice with leiningen, you will need to add
lein-git-deps as a dev-dependency, specify your git-dependencies, and
then add the Git-sourced code to to the classpath:
    :dev-dependencies [[lein-git-deps "0.0.1-SNAPSHOT"]]
    :git-dependencies [["https://github.com/tobyhede/monger.git"]]
    :extra-classpath-dirs [".lein-git-deps/monger/src/"]

You will also need to manually add the checked-out project's
dependencies as your own (the plugin simply checks out the code, it
doesn't recursively resolve dependencies).



## License

Copyright (C) 2012 Toby Hede

Copyright (C) 2012 Brenton Ashworth and Relevance, Inc

Distributed under the Eclipse Public License, the same as Clojure uses. See the file LICENSE.

