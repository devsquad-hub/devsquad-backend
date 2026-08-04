package com.devsquad.shared.persistence;

import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class JdbcClient {

  private final AgroalDataSource dataSource;

  public JdbcClient(AgroalDataSource dataSource) {
    this.dataSource = dataSource;
  }

  public StatementSpec sql(String sql) {
    return new StatementSpec(sql);
  }

  public final class StatementSpec {
    private final String source;
    private final Map<String, Object> parameters = new LinkedHashMap<>();

    private StatementSpec(String source) {
      this.source = source;
    }

    public StatementSpec param(String name, Object value) {
      parameters.put(name, value);
      return this;
    }

    public int update() {
      var parsed = NamedSql.parse(source, parameters);
      try (var connection = dataSource.getConnection();
          var statement = connection.prepareStatement(parsed.sql())) {
        bind(statement, parsed.arguments());
        return statement.executeUpdate();
      } catch (SQLException exception) {
        throw new SqlException(exception);
      }
    }

    public <T> QuerySpec<T> query(Class<T> type) {
      return query((resultSet, row) -> scalar(resultSet, type));
    }

    public <T> QuerySpec<T> query(RowMapper<T> mapper) {
      return new QuerySpec<>(NamedSql.parse(source, parameters), mapper);
    }
  }

  public final class QuerySpec<T> {
    private final NamedSql parsed;
    private final RowMapper<T> mapper;

    private QuerySpec(NamedSql parsed, RowMapper<T> mapper) {
      this.parsed = parsed;
      this.mapper = mapper;
    }

    public List<T> list() {
      try (var connection = dataSource.getConnection();
          var statement = connection.prepareStatement(parsed.sql())) {
        bind(statement, parsed.arguments());
        try (var resultSet = statement.executeQuery()) {
          var rows = new java.util.ArrayList<T>();
          for (var row = 0; resultSet.next(); row++) rows.add(mapper.map(resultSet, row));
          return List.copyOf(rows);
        }
      } catch (SQLException exception) {
        throw new SqlException(exception);
      }
    }

    public Optional<T> optional() {
      var rows = list();
      if (rows.size() > 1)
        throw new IllegalStateException("Expected at most one row but got " + rows.size());
      return rows.stream().findFirst();
    }

    public T single() {
      var rows = list();
      if (rows.size() != 1)
        throw new IllegalStateException("Expected one row but got " + rows.size());
      return rows.getFirst();
    }
  }

  private static void bind(java.sql.PreparedStatement statement, List<Object> arguments)
      throws SQLException {
    for (var index = 0; index < arguments.size(); index++)
      statement.setObject(index + 1, arguments.get(index));
  }

  @SuppressWarnings("unchecked")
  private static <T> T scalar(ResultSet resultSet, Class<T> type) throws SQLException {
    var value = resultSet.getObject(1);
    if (value == null || type.isInstance(value)) return (T) value;
    if (value instanceof Number number) {
      if (type == Integer.class) return (T) Integer.valueOf(number.intValue());
      if (type == Long.class) return (T) Long.valueOf(number.longValue());
      if (type == Double.class) return (T) Double.valueOf(number.doubleValue());
    }
    return resultSet.getObject(1, type);
  }

  @FunctionalInterface
  public interface RowMapper<T> {
    T map(ResultSet resultSet, int rowNumber) throws SQLException;
  }

  public static final class SqlException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public SqlException(SQLException cause) {
      super(cause);
    }

    public String sqlState() {
      return ((SQLException) getCause()).getSQLState();
    }
  }
}
