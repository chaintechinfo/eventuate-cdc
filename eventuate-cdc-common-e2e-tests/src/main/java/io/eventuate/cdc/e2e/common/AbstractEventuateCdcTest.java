package io.eventuate.cdc.e2e.common;

import io.eventuate.common.common.spring.jdbc.EventuateSpringJdbcStatementExecutor;
import io.eventuate.common.jdbc.EventuateCommonJdbcOperations;
import io.eventuate.common.jdbc.EventuateSchema;
import io.eventuate.common.jdbc.sqldialect.SqlDialectSelector;
import io.eventuate.util.test.async.Eventually;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public abstract class AbstractEventuateCdcTest {

  protected String subscriberId = generateId();

  protected EventuateCommonJdbcOperations eventuateCommonJdbcOperations;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private SqlDialectSelector sqlDialectSelector;

  @Value("${spring.datasource.driver-class-name}")
  private String driver;

  @Before
  public void init() {
    eventuateCommonJdbcOperations = new EventuateCommonJdbcOperations(new EventuateSpringJdbcStatementExecutor(jdbcTemplate),
            sqlDialectSelector.getDialect(driver));
  }

  @Test
  public void insertToEventTableAndWaitEventInBroker() throws Exception {
    String destination = generateId();

    String data = generateId() + getClass().getName();
    String rawData = "\"" + data + "\"";

    BlockingQueue<String> blockingQueue = new LinkedBlockingDeque<>();

    createConsumer(destination, blockingQueue::add);
    saveEvent(rawData, destination, new EventuateSchema(EventuateSchema.DEFAULT_SCHEMA));

    Eventually.eventually(120, 500, TimeUnit.MILLISECONDS, () -> {
      try {
        String m = blockingQueue.poll(100, TimeUnit.MILLISECONDS);
        assertTrue(m.contains(data));
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    });
  }

  protected String generateId() {
    return UUID.randomUUID().toString();
  }

  protected abstract void createConsumer(String topic, Consumer<String> consumer) throws Exception;
  protected abstract void saveEvent(String eventData, String eventType, EventuateSchema eventuateSchema);
}
