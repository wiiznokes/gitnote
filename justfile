set windows-powershell := true

fix:
    ./gradlew lintFix

prettier:
	# install on Debian: sudo snap install node --classic
	# npx is the command to run npm package, node is the runtime
	npx prettier -w .
