INSERT INTO bet (bet_id, user_id, event_id, event_market_id, event_winner_id, bet_amount, created_at)
SELECT
  'b' || x AS bet_id,
  'u' || (x % 10000) AS user_id,
  CASE
    WHEN x % 20 = 0 THEN 'e_hot_1'
    WHEN x % 50 = 0 THEN 'e_hot_2'
    ELSE 'e' || (x % 1000)
  END AS event_id,
  'm' || (x % 50) AS event_market_id,
  CASE
    WHEN x % 25 = 0 THEN 'w_hot_1'
    WHEN x % 80 = 0 THEN 'w_hot_2'
    ELSE 'w' || (x % 200)
  END AS event_winner_id,
  CAST((x % 10000) / 100.0 AS DECIMAL(18,2)) AS bet_amount,
  CURRENT_TIMESTAMP AS created_at
FROM SYSTEM_RANGE(1, 100000) AS t(x);
