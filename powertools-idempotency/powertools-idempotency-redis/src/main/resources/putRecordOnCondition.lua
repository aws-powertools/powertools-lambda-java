local hashKey = KEYS[1]
local expiryKey = KEYS[2]
local statusKey = KEYS[3]
local inProgressExpiryKey = KEYS[4]
local timeNowSeconds = ARGV[1]
local timeNowMillis = ARGV[2]
local inProgressValue = ARGV[3]
local expiryValue = ARGV[4]
local statusValue = ARGV[5]
local inProgressExpiryValue = ''

if ARGV[6] ~= nil then inProgressExpiryValue = ARGV[6] end;

if redis.call('exists', hashKey) == 0
    or redis.call('hget', hashKey, expiryKey) < timeNowSeconds
    or (redis.call('hexists', hashKey, inProgressExpiryKey) ~= 0
        and redis.call('hget', hashKey, inProgressExpiryKey) < timeNowMillis
        and redis.call('hget', hashKey, statusKey) == inProgressValue)
then return redis.call('hset', hashKey, expiryKey, expiryValue, statusKey, statusValue, inProgressExpiryKey, inProgressExpiryValue) end;
