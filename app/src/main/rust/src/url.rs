use anyhow::bail;

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum UrlKind {
    Ssh,
    Http,
    Https,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct UrlInfo {
    pub kind: UrlKind,
    pub path: String,
}

pub fn parse_url(url: &str) -> anyhow::Result<UrlInfo> {
    let str = bstr::BStr::new(url);

    let url = gix_url::parse(str)?;

    dbg!(&url);

    let kind = match &url.scheme {
        gix_url::Scheme::Ssh => UrlKind::Ssh,
        gix_url::Scheme::Http => UrlKind::Http,
        gix_url::Scheme::Https => UrlKind::Https,
        scheme => bail!("invalid scheme: {}", scheme),
    };

    Ok(UrlInfo {
        kind,
        path: url.path.to_string(),
    })
}

#[cfg(test)]
mod test {

    use super::*;

    #[test]

    fn test() {
        let url = parse_url("ssh://username@host:5555/dir/repo.git").unwrap();

        assert_eq!(&url.kind, &UrlKind::Ssh);

        let url = parse_url("https://github.com/wiiznokes/gitnote.git").unwrap();

        assert_eq!(&url.kind, &UrlKind::Https);

        let url = parse_url("git@github.com:wiiznokes/gitnote.git").unwrap();

        assert_eq!(&url.kind, &UrlKind::Ssh);

        let url = parse_url("git@git.sr.ht:~user/notes").unwrap();

        assert_eq!(&url.kind, &UrlKind::Ssh);
    }
}
