use std::{fs, io, path::Path};

fn main() -> io::Result<()> {
    let dir = Path::new("supported_extensions");

    // Rerun if the directory itself changes.
    println!("cargo:rerun-if-changed={}", dir.display());

    for entry in fs::read_dir(dir)? {
        let entry = entry?;
        let path = entry.path();

        if !path.is_file() {
            continue;
        }

        // Rerun if this specific file changes.
        println!("cargo:rerun-if-changed={}", path.display());

        let content = fs::read_to_string(&path)?;

        let mut lines: Vec<_> = content
            .lines()
            .map(str::trim)
            .filter(|l| !l.is_empty())
            .collect();

        lines.sort_unstable();

        let sorted = lines.join("\n") + "\n";

        // Only rewrite if the contents actually changed.
        if content != sorted {
            fs::write(&path, sorted)?;
        }
    }

    Ok(())
}
