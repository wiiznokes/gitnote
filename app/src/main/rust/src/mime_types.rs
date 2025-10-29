use mime_guess::mime;

const ADDITIONAL_SUPPORTED_EXTENSIONS: &[&str] = &["sh", "fish", "ps1", "bat"];

pub fn is_extension_supported(extension: &str) -> bool {
    if ADDITIONAL_SUPPORTED_EXTENSIONS.contains(&extension) {
        return true;
    }

    let mimes = mime_guess::from_ext(extension);
    mimes.iter().any(|mime| mime.type_() == mime::TEXT)
}

#[cfg(test)]
mod test {
    use std::{collections::HashMap, fs, path::Path};

    use super::*;

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
                let supported = {
                    let mimes = mime_guess::from_ext(&extension);
                    mimes.iter().any(|mime| mime.type_() == mime::TEXT)
                };

                let supported_manually =
                    ADDITIONAL_SUPPORTED_EXTENSIONS.contains(&extension.as_str());

                (extension, count, supported, supported_manually)
            })
            .collect::<Vec<_>>();

        extensions.sort_by(|a, b| b.1.cmp(&a.1));
        extensions.sort_by(|a, b| b.2.cmp(&a.2));

        println!("extension     supported     supported_manually    count");
        // Print results
        for (ext, count, supported, supported_manually) in extensions {
            println!(
                "{ext:<10}    {supported:<5}         {supported_manually:<5}                 {count:<6}"
            );
        }
    }
}
