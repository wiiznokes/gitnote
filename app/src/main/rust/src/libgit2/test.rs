use super::*;

#[test]
#[ignore = "local repo"]
fn timestamp() {
    open_repo("../../../../../repo_test").unwrap();

    let _res = get_timestamps();
}
