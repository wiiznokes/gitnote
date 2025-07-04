use super::*;

#[test]
#[ignore = "local repo"]
fn timestamp() {
    open_repo("../../../../../repo_test").unwrap();

    let res = get_timestamps();

    dbg!(&res);
}
