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

```clojure
:dev-dependencies [[lein-git-deps "0.0.2"]]
:git-dependencies [["https://github.com/tobyhede/monger.git"]]
:extra-classpath-dirs [".lein-git-deps/monger/src/"]
```

You will also need to manually add the checked-out project's
dependencies as your own (the plugin simply checks out the code, it
doesn't recursively resolve dependencies).

Leiningen 2
-----------

If you're using Leiningen 2, the setup is slightly different. You must
use the `:plugins` key rather than `:dev-dependencies`, and the
`:extra-classpath-dirs` setting is no longer supported. As a
workaround, you can add your library's source directory with
`:source-paths`:

```clojure
:plugins [[lein-git-deps "0.0.2"]]
:git-dependencies [["https://github.com/tobyhede/monger.git"]]
```

Emacs / clojure-jack-in
-----------------------

For Emacs users, using `M-x clojure-jack-in` does not currently
download the Git dependencies as it does normal dependencies. You must
manually run `lein git-deps` from the command line in your project
directory to get the Git dependencies installed.

Deploying your own fork
-----------------------

Using Git-based dependencies is rather fragile and only really
suitable for development. The code you're pulling from GitHub can
change at any time, without warning. If you're deploying code to
production or releasing it publicly, it's better to base your code on
a stable known-target release. Getting the upstream maintainer of the
original library to release a new official version is best, but this
is not always possible if the original author is not around or if the
project has a slow release cycle.

In the case where pushing an updated version of the mainline release
is not possible, fork the project and package up your own
release. Then, you can either deploy it on your local system with
`lein install`, create a private Maven repository and publish it
there, or publish it to Clojars with a different group-id.

Detailed instructions on how to do these things can be found at
[https://github.com/technomancy/leiningen/blob/stable/doc/DEPLOY.md](https://github.com/technomancy/leiningen/blob/stable/doc/DEPLOY.md).

## License

Copyright (C) 2012 Toby Hede

Copyright (C) 2012 Brenton Ashworth and Relevance, Inc

Distributed under the Eclipse Public License, the same as Clojure uses. See the file LICENSE.

