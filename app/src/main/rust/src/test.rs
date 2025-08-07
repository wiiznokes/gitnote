use rand_core::OsRng;
use ssh_key::{
    PrivateKey, PublicKey,
    private::{Ed25519Keypair, KeypairData},
    public::KeyData,
};

#[test]
#[ignore = "local testing"]
fn gen_keys() {
    let comment = "GitNote";

    let pair = Ed25519Keypair::random(&mut OsRng);

    let private_key: PrivateKey =
        PrivateKey::new(KeypairData::Ed25519(pair.clone()), comment).unwrap();

    let public_key: PublicKey = PublicKey::new(KeyData::Ed25519(pair.public), comment);

    let public_ssh = public_key.to_openssh().unwrap();
    let private_key = private_key.to_openssh(ssh_key::LineEnding::LF).unwrap();

    println!("Public Key: {public_ssh}");
    println!("Secret Key: {}", private_key.as_str());
}
