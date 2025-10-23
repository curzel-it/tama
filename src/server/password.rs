use argon2::{
    password_hash::{rand_core::OsRng, PasswordHash, PasswordHasher, PasswordVerifier, SaltString},
    Argon2,
};

pub fn hash_password(password: &str) -> Result<String, String> {
    let salt = SaltString::generate(&mut OsRng);
    let argon2 = Argon2::default();

    argon2
        .hash_password(password.as_bytes(), &salt)
        .map(|hash| hash.to_string())
        .map_err(|e| format!("Failed to hash password: {e}"))
}

pub fn verify_password(password: &str, hash: &str) -> Result<bool, String> {
    let parsed_hash = PasswordHash::new(hash)
        .map_err(|e| format!("Failed to parse password hash: {e}"))?;

    let argon2 = Argon2::default();

    Ok(argon2
        .verify_password(password.as_bytes(), &parsed_hash)
        .is_ok())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_hash_password() {
        let password = "test_password_123";
        let hash = hash_password(password);

        assert!(hash.is_ok());
        let hash_str = hash.unwrap();
        assert!(!hash_str.is_empty());
        assert!(hash_str.starts_with("$argon2"));
    }

    #[test]
    fn test_verify_password_correct() {
        let password = "test_password_123";
        let hash = hash_password(password).unwrap();

        let result = verify_password(password, &hash);
        assert!(result.is_ok());
        assert!(result.unwrap());
    }

    #[test]
    fn test_verify_password_incorrect() {
        let password = "test_password_123";
        let wrong_password = "wrong_password";
        let hash = hash_password(password).unwrap();

        let result = verify_password(wrong_password, &hash);
        assert!(result.is_ok());
        assert!(!result.unwrap());
    }

    #[test]
    fn test_verify_password_invalid_hash() {
        let password = "test_password_123";
        let invalid_hash = "not_a_valid_hash";

        let result = verify_password(password, invalid_hash);
        assert!(result.is_err());
    }

    #[test]
    fn test_hash_generates_different_salts() {
        let password = "test_password_123";
        let hash1 = hash_password(password).unwrap();
        let hash2 = hash_password(password).unwrap();

        assert_ne!(hash1, hash2);

        assert!(verify_password(password, &hash1).unwrap());
        assert!(verify_password(password, &hash2).unwrap());
    }
}
