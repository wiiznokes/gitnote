use anyhow::anyhow;
use rand_core::OsRng;
use ssh_key::{
    PrivateKey, PublicKey,
    private::{Ed25519Keypair, KeypairData},
    public::KeyData,
};
use zeroize::Zeroizing;

pub struct SshKeys {
    pub public: String,
    pub private: Zeroizing<String>,
}

pub fn gen_keys() -> anyhow::Result<SshKeys> {
    let comment = "GitNote";

    let pair = Ed25519Keypair::random(&mut OsRng);

    let private_key: PrivateKey =
        PrivateKey::new(KeypairData::Ed25519(pair.clone()), comment).map_err(|e| anyhow!("{e}"))?;

    let public_key: PublicKey = PublicKey::new(KeyData::Ed25519(pair.public), comment);

    let public = public_key.to_openssh().map_err(|e| anyhow!("{e}"))?;
    let private = private_key
        .to_openssh(ssh_key::LineEnding::LF)
        .map_err(|e| anyhow!("{e}"))?;

    Ok(SshKeys { public, private })
}
