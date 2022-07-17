package com.lyft.data.gateway.ha.handler;

import com.lyft.data.gateway.ha.config.ExternalPrestoConfiguration;
import com.lyft.data.gateway.ha.module.QueryDetails;
import io.trino.sql.parser.ParsingOptions;
import io.trino.sql.parser.SqlParser;
import io.trino.sql.tree.Query;
import io.trino.sql.tree.ShowCatalogs;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;

import static io.trino.sql.parser.ParsingOptions.DecimalLiteralTreatment.AS_DECIMAL;


@Slf4j
public class ExternalPrestoRequestHandler implements Runnable {
    public static final String DEFAULT_CATALOG = "hive";
    public static final String DEFAULT_SOURCE = "NO_SOURCE";
    BlockingQueue<QueryDetails> queue;
    ExternalPrestoConfiguration prestoConfig;
    Connection connection;
    Statement statement;
//  private static final String JDBC_DRIVER = "com.qubole.jdbc.jdbc41.core.QDriver";

    public ExternalPrestoRequestHandler(ExternalPrestoConfiguration externalPresto,
                                        BlockingQueue<QueryDetails> queue) {
        this.queue = queue;
        this.prestoConfig = externalPresto;
    }

    public void run() {
        log.info("Starting thread");
        while (true) {
            try {
                QueryDetails queryDetails = queue.take();
                String query = queryDetails.getQuery();
                log.info("Executing query: {}", query);
                this.executeQuery(queryDetails);
            } catch (SQLException e) {
                log.error("Query execution error", e);
                e.printStackTrace();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    protected void executeQuery(QueryDetails queryDetails) throws SQLException {
        try {
            String query = queryDetails.getQuery();
            log.info("Query details: {}", queryDetails);
            Properties properties = new Properties();
            properties.setProperty("user", queryDetails.getUser());
            if (prestoConfig.getPassword() != null) {
                properties.setProperty("password", prestoConfig.getPassword());
            }
            String source = queryDetails.getSource() != null ? queryDetails.getSource() : DEFAULT_SOURCE;
            properties.setProperty("source", source);
            String catalog = queryDetails.getCatalog() != null ? queryDetails.getCatalog() : DEFAULT_CATALOG;
            String jdbcUrl = prestoConfig.getJdbcUrl() + "/" + catalog;
            connection = DriverManager.getConnection(jdbcUrl, properties);
            connection.setReadOnly(true);
            statement = connection.createStatement();
            Long querySubmissionStartTime = System.currentTimeMillis();
            this.statement.execute(query);
            Long querySubmissionEndTime = System.currentTimeMillis();
            long elapsedTime = (querySubmissionEndTime - querySubmissionStartTime);
            log.info(String.format("Query Info: Submission time %d ms", elapsedTime));
      Long queryStartTime = System.currentTimeMillis();
      ResultSet rs = this.statement.executeQuery(query);
      while(rs.next()) {
      }
      Long queryEndTime = System.currentTimeMillis();
      long elapsedTimeAfterResult = (queryEndTime-queryStartTime);
      log.info(String.format("Query Info: Time elapsed after result %d ms", elapsedTimeAfterResult));
      rs.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            connection.close();
        }
    }
}
