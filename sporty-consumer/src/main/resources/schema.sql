CREATE TABLE IF NOT EXISTS bet (
  bet_id VARCHAR(64) PRIMARY KEY,
  user_id VARCHAR(64) NOT NULL,
  event_id VARCHAR(64) NOT NULL,
  event_market_id VARCHAR(64) NOT NULL,
  event_winner_id VARCHAR(64) NOT NULL,
  bet_amount DECIMAL(18,2) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_bet_event_winner_bet_id ON bet (event_id, event_winner_id, bet_id);
CREATE INDEX IF NOT EXISTS idx_bet_user_created_desc ON bet (user_id, created_at DESC);

CREATE TABLE IF NOT EXISTS outbox (
  id VARCHAR(128) PRIMARY KEY,
  payload BLOB NOT NULL,
  status VARCHAR(16) NOT NULL,
  attempt_count INT NOT NULL,
  next_attempt_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_outbox_status_next_attempt ON outbox (status, next_attempt_at);
CREATE INDEX IF NOT EXISTS idx_outbox_created_at ON outbox (created_at);
