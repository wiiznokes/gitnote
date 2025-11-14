set windows-powershell := true

main:
    echo "do nothing"

fix:
    ./gradlew lintFix

fmt-just:
    just --fmt --unstable

prettier:
    # install on Debian: sudo snap install node --classic
    # npx is the command to run npm package, node is the runtime
    npx prettier -w . --ignore-path ./.gitignore --ignore-path ./.prettierignore

sort-supported-extension:
    #!/usr/bin/env bash
    for f in $(ls app/src/main/rust/extensions 2>/dev/null); do
    	sort app/src/main/rust/extensions/$f -o app/src/main/rust/extensions/$f
    done
