mod auth;
mod auth_endpoints;
mod channel_endpoints;
mod jwt;
mod middleware;
mod password;
mod rate_limiter;
mod server_logic;

use r2d2::Pool;
use r2d2_sqlite::SqliteConnectionManager;
use std::sync::Arc;
use server_logic::run_server;

pub type DbPool = Pool<SqliteConnectionManager>;

#[derive(Clone)]
pub struct AppState {
    pub db: DbPool,
    pub jwt_secret: String,
    pub auth_rate_limiter: Arc<rate_limiter::RateLimiter>,
    pub api_rate_limiter: Arc<rate_limiter::RateLimiter>,
    pub upload_rate_limiter: Arc<rate_limiter::RateLimiter>,
}

#[tokio::main]
async fn main() -> Result<(), String> {
    dotenvy::dotenv().ok();

    tracing_subscriber::fmt()
        .with_target(false)
        .compact()
        .init();

    let db_path = std::env::var("DATABASE_PATH").unwrap_or_else(|_| "tama.db".to_string());
    let port = std::env::var("SERVER_PORT")
        .ok()
        .and_then(|p| p.parse::<u16>().ok())
        .unwrap_or(3000);
    let jwt_secret = std::env::var("JWT_SECRET")
        .expect("JWT_SECRET must be set in .env file");

    run_server(&db_path, port, jwt_secret).await
}
