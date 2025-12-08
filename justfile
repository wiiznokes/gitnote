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
    extension_dir=app/src/main/rust/supported_extensions
    for f in $(ls $extension_dir 2>/dev/null); do
    	sort $extension_dir/$f -o $extension_dir/$f
        echo sorted $f
    done
