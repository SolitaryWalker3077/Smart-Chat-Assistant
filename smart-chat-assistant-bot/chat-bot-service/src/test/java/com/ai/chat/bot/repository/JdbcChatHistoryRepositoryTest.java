package com.ai.chat.bot.repository;

import com.ai.chat.bot.entity.ChatInfo;
import com.ai.chat.bot.mapper.ChatSessionMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.MapPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcChatHistoryRepositoryTest {

    @TempDir
    Path directory;

    private AnnotationConfigApplicationContext context;
    private ChatHistoryRepository repository;
    private final List<String> chatIds = new ArrayList<>();

    @BeforeEach
    void start() {
        openContext();
        if (System.getenv("CHAT_HISTORY_TEST_JDBC_URL") == null) {
            new JdbcTemplate(context.getBean(DataSource.class)).execute("""
                    CREATE TABLE IF NOT EXISTS chat_sessions (
                        id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                        chat_id VARCHAR(36) NOT NULL,
                        title VARCHAR(127) NOT NULL DEFAULT '新会话',
                        created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        CONSTRAINT uk_chat_sessions_chat_id UNIQUE (chat_id)
                    )
                    """);
        }
    }

    private void openContext() {
        context = createContext(directory);
        repository = context.getBean(ChatHistoryRepository.class);
    }

    private static AnnotationConfigApplicationContext createContext(Path directory) {
        String url = System.getenv("CHAT_HISTORY_TEST_JDBC_URL");
        boolean mysql = url != null;
        var context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("testDatabase", Map.of(
                "spring.datasource.url", mysql ? url : "jdbc:h2:file:"
                        + directory.resolve("sessions").toAbsolutePath().toString().replace('\\', '/')
                        + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE",
                "spring.datasource.username", mysql ? System.getenv("CHAT_HISTORY_TEST_DB_USER") : "sa",
                "spring.datasource.password", mysql ? System.getenv("CHAT_HISTORY_TEST_DB_PASSWORD") : "",
                "spring.datasource.driver-class-name", mysql ? "com.mysql.cj.jdbc.Driver" : "org.h2.Driver"
        )));
        context.register(DatabaseConfiguration.class);
        context.refresh();
        return context;
    }

    @AfterEach
    void cleanUp() {
        if (context != null && context.isActive()) {
            try {
                for (String chatId : chatIds) {
                    repository.clearByChatId(chatId);
                }
            } finally {
                context.close();
            }
        }
    }

    private String newChatId() {
        String chatId = UUID.randomUUID().toString();
        chatIds.add(chatId);
        return chatId;
    }

    private List<ChatInfo> testChats() {
        return repository.getChats().stream()
                .filter(chat -> chatIds.contains(chat.getChatId()))
                .toList();
    }

    @Test
    void databaseRepositoryIsTheOnlyBeanAndUpdatesKeepInsertionOrder() {
        assertThat(context.getBeansOfType(ChatHistoryRepository.class)).hasSize(1);
        assertThat(repository).isInstanceOf(JdbcChatHistoryRepository.class);
        String first = newChatId();
        String second = newChatId();
        repository.save(first, "第一条会话");
        repository.save(second, "第二条会话");
        repository.save(first, "更新后的标题");

        assertThat(testChats()).containsExactly(
                new ChatInfo(first, "更新后的标题"), new ChatInfo(second, "第二条会话"));
        assertThat(context.getBean(ChatSessionMapper.class).findAll())
                .filteredOn(session -> session.getChatId().equals(first))
                .singleElement().satisfies(session -> {
                    assertThat(session.getId()).isNotNull();
                    assertThat(session.getCreatedTime()).isNotNull();
                    assertThat(session.getUpdatedTime()).isNotNull();
                });
    }

    @Test
    void titleRulesHandleNullAndPromptsLongerThanTheDatabaseColumn() {
        String first = newChatId();
        String second = newChatId();
        repository.save(first, null);
        repository.save(second, "很长的用户输入".repeat(100));

        assertThat(testChats()).containsExactly(
                new ChatInfo(first, null), new ChatInfo(second, "很长的用户输入".repeat(100)));
    }

    @Test
    void sessionsAndDeletionsSurviveApplicationContextRestart() {
        String retained = newChatId();
        String deleted = newChatId();
        repository.save(retained, "重启后仍然存在");
        repository.save(deleted, "需要删除的会话");
        repository.clearByChatId(deleted);
        repository.clearByChatId(deleted);
        context.close();

        // Close the entire connection pool, then recreate Spring and MyBatis.
        openContext();
        assertThat(testChats()).containsExactly(new ChatInfo(retained, "重启后仍然存在"));
        repository.clearByChatId(retained);
        context.close();

        openContext();
        assertThat(testChats()).isEmpty();
    }

    @Test
    void concurrentSavesOfTheSameChatCreateOnlyOneRow() throws Exception {
        String chatId = newChatId();
        var executor = Executors.newFixedThreadPool(4);
        try {
            List<Callable<Void>> saves = new ArrayList<>();
            for (int i = 0; i < 12; i++) {
                String title = "并发标题" + i;
                saves.add(() -> {
                    repository.save(chatId, title);
                    return null;
                });
            }
            for (var result : executor.invokeAll(saves)) {
                result.get();
            }
            assertThat(testChats()).hasSize(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void sessionsSurviveSeparateJvmProcesses() throws Exception {
        String chatId = newChatId();
        context.close();
        try {
            for (String action : List.of("save", "readAndDelete", "verifyDeleted")) {
                Path output = directory.resolve(action + ".log");
                Process process = new ProcessBuilder(
                        Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                        "-cp", System.getProperty("surefire.test.class.path", System.getProperty("java.class.path")),
                        RestartProbe.class.getName(), directory.toString(), chatId, action)
                        .redirectErrorStream(true).redirectOutput(output.toFile()).start();
                boolean finished = process.waitFor(45, TimeUnit.SECONDS);
                if (!finished) {
                    process.destroyForcibly();
                    process.waitFor(5, TimeUnit.SECONDS);
                }
                assertThat(finished).as("Restart probe timed out: %s", action).isTrue();
                assertThat(process.exitValue()).withFailMessage(Files.readString(output)).isZero();
            }
        } finally {
            openContext();
        }
    }

    public static class RestartProbe {
        public static void main(String[] args) {
            try (var context = createContext(Path.of(args[0]))) {
                ChatHistoryRepository repository = context.getBean(ChatHistoryRepository.class);
                String chatId = args[1];
                switch (args[2]) {
                    case "save" -> repository.save(chatId, "跨进程持久化");
                    case "readAndDelete" -> {
                        assertThat(repository.getChats()).contains(new ChatInfo(chatId, "跨进程持久化"));
                        repository.clearByChatId(chatId);
                    }
                    case "verifyDeleted" -> assertThat(repository.getChats())
                            .noneMatch(chat -> chat.getChatId().equals(chatId));
                    default -> throw new IllegalArgumentException(args[2]);
                }
            }
        }
    }

    @Configuration(proxyBeanMethods = false)
    @Import({DataSourceAutoConfiguration.class, MybatisAutoConfiguration.class})
    @MapperScan(basePackageClasses = ChatSessionMapper.class)
    @ComponentScan(basePackageClasses = ChatHistoryRepository.class)
    static class DatabaseConfiguration {
    }
}
