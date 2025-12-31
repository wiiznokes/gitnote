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

build:
    #!/usr/bin/env bash
    JAVA_HOME=$(grep '^java.home=' .gradle/config.properties | cut -d'=' -f2)
    export JAVA_HOME
    ./gradlew :app:assembleDebug

fix-wrapper:
    #curl -L -o gradle/wrapper/gradle-wrapper.jar https://github.com/gradle/gradle/raw/v8.13.0/gradle/wrapper/gradle-wrapper.jar
    git lfs pull
