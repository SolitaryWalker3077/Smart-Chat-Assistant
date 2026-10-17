# 会话列表持久化

会话列表由 `JdbcChatHistoryRepository` 通过 MyBatis 的 `ChatSessionMapper`
读写 `chat_sessions`。`MemoryChatHistoryRepository` 保留为普通类，不再注册为 Spring Bean。
接口地址及返回的 `ChatInfo` 不变：标题最多 15 个字符，空标题显示“无标题”，列表按创建顺序返回。
同一 `chat_id` 再次保存时更新标题，不改变列表顺序。

## 数据库

新数据库通过现有的 `spring.ai.chat.memory.repository.jdbc` 初始化配置执行
`src/main/resources/sql/schema-mysql.sql`，创建会话列表表和消息历史表。

已有 `chat_sessions` 必须具有 `chat_id` 唯一索引，保证原子新增/更新及并发去重。
如果尚无此索引，在目标数据库执行一次
`src/main/resources/sql/migrate-chat-sessions-unique-key.sql`。
先检查脚本中的重复 ID 查询；如果有结果，处理重复数据后再执行 `ALTER TABLE`。
`CREATE TABLE IF NOT EXISTS` 不会更新已存在的表结构。

## 自动化验证

默认使用临时 H2 文件数据库，无需启动 Redis、MCP 或调用模型：

```powershell
mvn test -Dtest=JdbcChatHistoryRepositoryTest
```

使用真实 MySQL 验证时，先完成上面的表结构准备，并在当前终端设置：

```powershell
$env:CHAT_HISTORY_TEST_JDBC_URL = 'jdbc:mysql://localhost:3306/chat_bot?characterEncoding=utf8&useSSL=false&serverTimezone=UTC'
$env:CHAT_HISTORY_TEST_DB_USER = '<数据库用户名>'
$env:CHAT_HISTORY_TEST_DB_PASSWORD = '<数据库密码>'
mvn test -Dtest=JdbcChatHistoryRepositoryTest
Remove-Item Env:CHAT_HISTORY_TEST_JDBC_URL, Env:CHAT_HISTORY_TEST_DB_USER, Env:CHAT_HISTORY_TEST_DB_PASSWORD
```

测试覆盖唯一 Bean 注入、增改查删、标题规则、并发保存、应用上下文重建，以及独立 JVM
进程之间的数据持久化。测试只清理自己生成的随机 UUID 会话，不清空表。

这些测试启动数据库持久化组件，不启动完整聊天服务。完整服务的人工验收步骤：

1. 启动 `SpringChatBotApplication`，在页面发送消息并记录会话 ID。
2. 访问 `/chat/getChatIds`，确认对应会话；数据库可按 `chat_id` 查询验证。
3. 停止并重新启动服务，再次访问 `/chat/getChatIds` 或刷新页面，确认会话仍在。
4. 删除测试会话，再重启，确认该会话不再出现。

原内存 Map 中的数据不会自动迁移；此次改动保证使用数据库实现后保存的会话持久化。
