local key = KEYS[1]
local now = tonumber(ARGV[1])
local window = tonumber(ARGV[2])
local limit = tonumber(ARGV[3])

local clearBefore = now - window
redis.call('ZREMRANGEBYSCORE', key, 0, clearBefore)

local currentCount = redis.call('ZCARD', key)
local allowed = 0

if currentCount < limit then
    redis.call('ZADD', key, now, now)
    allowed = 1
    currentCount = currentCount + 1
end

redis.call('PEXPIRE', key, window)

local remaining = limit - currentCount
local resetTime = 0
if currentCount > 0 then
    local oldest = redis.call('ZRANGE', key, 0, 0, 'WITHSCORES')
    if oldest[2] then
        resetTime = math.floor(oldest[2] + window)
    end
end

return {allowed, remaining, resetTime}
