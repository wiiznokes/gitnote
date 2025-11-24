# Contributing

First off, thanks for taking the time to contribute!

Contributions are welcome, do not hesitate to open an issue, a pull request, etc...
For features, it's better to create an issue first, in order to get feedback on whether the feature is wanted or not, and to align on the best approach before starting development.

# Translation

There are two places where translations can be done in this project.

- the fastlane [directory](./metadata/)
- the strings used in the app ([directory](./app/src/main/res/))

There is no plan to add Weblate support for now, because it cost to much money. You can use Android Studio if you want to use an editor.

<strong> Please don't translate the README, or other markdown files, this will not be merged. </strong>

# File extensions

You can add supported file extension by adding an extension in [this those files](./app/src/main/rust/extensions/). Make sure to call `just sort-supported-extension` after.
