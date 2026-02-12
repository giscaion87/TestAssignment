math.randomseed(os.time())

local token = os.getenv("JWT_TOKEN")
if not token or #token == 0 then
  error("JWT_TOKEN env var not set")
end

local delay_ms = tonumber(os.getenv("REQUEST_DELAY_MS") or "0")

local headers = {
  ["Content-Type"] = "application/json",
  ["Authorization"] = "Bearer " .. token
}

local function maybe_delay()
  if delay_ms <= 0 then
    return
  end
  local deadline = os.clock() + (delay_ms / 1000)
  while os.clock() < deadline do end
end

local function pick_event()
  local r = math.random(1, 100)
  if r <= 5 then
    return "e_hot_1"
  elseif r <= 7 then
    return "e_hot_2"
  else
    return "e" .. tostring(math.random(0, 999))
  end
end

local function pick_winner()
  local r = math.random(1, 100)
  if r <= 4 then
    return "w_hot_1"
  elseif r <= 5 then
    return "w_hot_2"
  else
    return "w" .. tostring(math.random(0, 199))
  end
end

request = function()
  maybe_delay()
  local event_id = pick_event()
  local event_winner_id = pick_winner()
  local body = string.format(
    '{"event_id":"%s","event_name":"Final-%s","event_winner_id":"%s"}',
    event_id,
    event_id,
    event_winner_id
  )
  return wrk.format("POST", "/publish", headers, body)
end
