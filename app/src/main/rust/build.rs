use std::{
    collections::{HashMap, HashSet},
    fmt::Display,
    fs, io,
    path::Path,
};

struct FileExtension {
    extension: HashSet<String>,
    path: String,
}

fn format_file(path: &Path) -> io::Result<FileExtension> {
    let content = fs::read_to_string(path)?;

    let lines_set = content
        .lines()
        .map(str::trim)
        .filter(|l| !l.is_empty())
        .map(str::to_ascii_lowercase)
        .collect::<HashSet<_>>();

    let mut lines = lines_set.clone().into_iter().collect::<Vec<_>>();

    lines.sort_unstable();

    let sorted = lines.join("\n") + "\n";

    // Only rewrite if the contents actually changed.
    if content != sorted {
        fs::write(path, sorted)?;
    }

    Ok(FileExtension {
        extension: lines_set,
        path: path.to_string_lossy().to_string(),
    })
}

struct DuplicateResult {
    path1: String,
    path2: String,
    extension: String,
}

impl Display for DuplicateResult {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(
            f,
            "Duplicate extension '{}':\n  - {}\n  - {}",
            self.extension, self.path1, self.path2,
        )
    }
}

fn find_duplicates(files: &[FileExtension]) -> Vec<DuplicateResult> {
    let mut seen: HashMap<&str, &str> = HashMap::new();
    let mut duplicates = Vec::new();

    for file in files {
        for extension in &file.extension {
            if let Some(path1) = seen.get(extension.as_str()) {
                duplicates.push(DuplicateResult {
                    path1: path1.to_string(),
                    path2: file.path.clone(),
                    extension: extension.clone(),
                });
            } else {
                seen.insert(extension, &file.path);
            }
        }
    }

    duplicates
}

fn main() -> io::Result<()> {
    let dir = Path::new("supported_extensions");

    // Rerun if the directory itself changes.
    println!("cargo:rerun-if-changed={}", dir.display());

    let mut supported_extensions = Vec::new();

    for entry in fs::read_dir(dir)? {
        let entry = entry?;
        let path = entry.path();

        if !path.is_file() {
            continue;
        }

        // Rerun if this specific file changes.
        println!("cargo:rerun-if-changed={}", path.display());

        let set = format_file(&path)?;

        supported_extensions.push(set);
    }

    let duplicate_result = find_duplicates(&supported_extensions);

    if !duplicate_result.is_empty() {
        let errors = duplicate_result
            .iter()
            .map(ToString::to_string)
            .collect::<Vec<_>>()
            .join("\n");

        panic!("Duplicate extensions detected:\n{errors}");
    }

    Ok(())
}
