use include_lines::include_lines;

pub fn is_extension_supported(extension: &str) -> bool {
    let text_extensions = include_lines!("./extensions/text.txt");
    let markdown_extensions = include_lines!("./extensions/markdown.txt");

    text_extensions.binary_search(&extension).is_ok()
        || markdown_extensions.binary_search(&extension).is_ok()
}

#[cfg(test)]
mod test {
    use std::{collections::HashMap, fs, path::Path};

    use super::*;

    #[test]
    fn test1() {
        assert!(is_extension_supported("md"));
        assert!(is_extension_supported("txt"));
        assert!(!is_extension_supported("bin"));
    }

    #[test]
    #[ignore = "local repo"]
    fn check_extension() {
        let path = Path::new("../../../../../note-pv");
        let mut counts: HashMap<String, usize> = HashMap::new();

        // Recursively walk through the directory
        fn visit_dir(dir: &Path, counts: &mut HashMap<String, usize>) {
            if let Ok(entries) = fs::read_dir(dir) {
                for entry in entries.flatten() {
                    let path = entry.path();
                    if path.is_dir() {
                        visit_dir(&path, counts);
                    } else if let Some(ext) = path.extension().and_then(|e| e.to_str()) {
                        *counts.entry(ext.to_lowercase()).or_insert(0) += 1;
                    }
                }
            }
        }

        visit_dir(path, &mut counts);

        // Sort by number of files (descending)
        let mut extensions = counts
            .into_iter()
            .map(|(extension, count)| {
                let supported = is_extension_supported(&extension);

                (extension, count, supported)
            })
            .collect::<Vec<_>>();

        extensions.sort_by(|a, b| b.1.cmp(&a.1));
        extensions.sort_by(|a, b| b.2.cmp(&a.2));

        println!("extension     supported     count");
        // Print results
        for (ext, count, supported) in extensions {
            println!("{ext:<10}    {supported:<5}         {count:<6}");
        }
    }
}
