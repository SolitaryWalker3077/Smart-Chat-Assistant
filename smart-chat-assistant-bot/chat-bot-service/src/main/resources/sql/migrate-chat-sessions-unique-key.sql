-- Run once for an existing chat_sessions table without a unique chat_id index.
-- Check for duplicates first; resolve any results before applying the index.
SELECT chat_id, COUNT(*) AS session_count
FROM chat_sessions
GROUP BY chat_id
HAVING COUNT(*) > 1;

ALTER TABLE chat_sessions
    ADD UNIQUE KEY uk_chat_sessions_chat_id (chat_id);
