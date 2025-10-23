use std::collections::HashMap;
use std::net::IpAddr;
use std::sync::Arc;
use std::time::{Duration, Instant};
use tokio::sync::RwLock;

#[derive(Clone)]
pub struct RateLimiter {
    limits: Arc<RwLock<HashMap<IpAddr, RateLimitInfo>>>,
    max_requests: usize,
    window: Duration,
}

struct RateLimitInfo {
    count: usize,
    window_start: Instant,
}

impl RateLimiter {
    pub fn new(max_requests: usize, window_seconds: u64) -> Self {
        Self {
            limits: Arc::new(RwLock::new(HashMap::new())),
            max_requests,
            window: Duration::from_secs(window_seconds),
        }
    }

    /// Check if the request should be allowed
    /// Returns Ok(()) if allowed, Err with retry-after seconds if rate limited
    pub async fn check(&self, ip: IpAddr) -> Result<(), u64> {
        let mut limits = self.limits.write().await;
        let now = Instant::now();

        let info = limits.entry(ip).or_insert(RateLimitInfo {
            count: 0,
            window_start: now,
        });

        // Check if window has expired
        if now.duration_since(info.window_start) >= self.window {
            // Reset window
            info.count = 0;
            info.window_start = now;
        }

        // Check rate limit
        if info.count >= self.max_requests {
            let retry_after = self.window.as_secs()
                - now.duration_since(info.window_start).as_secs();
            return Err(retry_after);
        }

        // Allow request
        info.count += 1;
        Ok(())
    }

    /// Clean up expired entries to prevent memory growth
    pub async fn cleanup_expired(&self) {
        let mut limits = self.limits.write().await;
        let now = Instant::now();

        limits.retain(|_, info| {
            now.duration_since(info.window_start) < self.window * 2
        });
    }
}

impl Default for RateLimiter {
    fn default() -> Self {
        Self::new(100, 60) // 100 requests per 60 seconds
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::net::{IpAddr, Ipv4Addr};

    #[tokio::test]
    async fn test_rate_limiter_allows_within_limit() {
        let limiter = RateLimiter::new(5, 60);
        let ip = IpAddr::V4(Ipv4Addr::new(127, 0, 0, 1));

        for _ in 0..5 {
            assert!(limiter.check(ip).await.is_ok());
        }
    }

    #[tokio::test]
    async fn test_rate_limiter_blocks_over_limit() {
        let limiter = RateLimiter::new(3, 60);
        let ip = IpAddr::V4(Ipv4Addr::new(127, 0, 0, 1));

        // First 3 should succeed
        for _ in 0..3 {
            assert!(limiter.check(ip).await.is_ok());
        }

        // 4th should fail
        assert!(limiter.check(ip).await.is_err());
    }

    #[tokio::test]
    async fn test_rate_limiter_resets_after_window() {
        let limiter = RateLimiter::new(2, 1); // 2 requests per 1 second
        let ip = IpAddr::V4(Ipv4Addr::new(127, 0, 0, 1));

        // Use up limit
        assert!(limiter.check(ip).await.is_ok());
        assert!(limiter.check(ip).await.is_ok());
        assert!(limiter.check(ip).await.is_err());

        // Wait for window to reset
        tokio::time::sleep(Duration::from_secs(2)).await;

        // Should work again
        assert!(limiter.check(ip).await.is_ok());
    }

    #[tokio::test]
    async fn test_rate_limiter_independent_ips() {
        let limiter = RateLimiter::new(2, 60);
        let ip1 = IpAddr::V4(Ipv4Addr::new(127, 0, 0, 1));
        let ip2 = IpAddr::V4(Ipv4Addr::new(127, 0, 0, 2));

        // Each IP has its own limit
        assert!(limiter.check(ip1).await.is_ok());
        assert!(limiter.check(ip1).await.is_ok());
        assert!(limiter.check(ip1).await.is_err());

        // IP2 should still work
        assert!(limiter.check(ip2).await.is_ok());
        assert!(limiter.check(ip2).await.is_ok());
        assert!(limiter.check(ip2).await.is_err());
    }

    #[tokio::test]
    async fn test_cleanup_expired() {
        let limiter = RateLimiter::new(10, 1);
        let ip = IpAddr::V4(Ipv4Addr::new(127, 0, 0, 1));

        limiter.check(ip).await.ok();

        {
            let limits = limiter.limits.read().await;
            assert_eq!(limits.len(), 1);
        }

        tokio::time::sleep(Duration::from_secs(3)).await;
        limiter.cleanup_expired().await;

        {
            let limits = limiter.limits.read().await;
            assert_eq!(limits.len(), 0);
        }
    }
}
