use super::*;

#[test]
#[ignore = "local repo"]
fn timestamp() {
    open_repo("../../../../../repo_test").unwrap();

    let res = get_timestamps();

    dbg!(&res);
}

#[test]
#[ignore = "local repo"]
fn timestamp2() {
    open_repo("../../../../../note-pv").unwrap();

    let res = get_timestamps();

    let mut res = res.unwrap().into_iter().collect::<Vec<_>>();

    res.sort_by(|a, b| a.1.cmp(&b.1));

    dbg!(&res);
}
