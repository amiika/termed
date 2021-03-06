package fi.thl.termed.dao.jdbc;

import com.google.common.collect.Iterables;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import fi.thl.termed.domain.PropertyValueId;
import fi.thl.termed.spesification.sql.SqlSpecification;
import fi.thl.termed.util.LangValue;
import fi.thl.termed.util.UUIDs;

public class JdbcSchemePropertyValueDao extends AbstractJdbcDao<PropertyValueId<UUID>, LangValue> {

  public JdbcSchemePropertyValueDao(DataSource dataSource) {
    super(dataSource);
  }

  @Override
  public void insert(PropertyValueId<UUID> id, LangValue langValue) {
    jdbcTemplate.update(
        "insert into scheme_property_value (scheme_id, property_id, index, lang, value) values (?, ?, ?, ?, ?)",
        id.getSubjectId(),
        id.getPropertyId(),
        id.getIndex(),
        langValue.getLang(),
        langValue.getValue());
  }

  @Override
  public void update(PropertyValueId<UUID> id, LangValue langValue) {
    jdbcTemplate.update(
        "update scheme_property_value set lang = ?, value = ? where scheme_id = ? and property_id = ? and index = ?",
        langValue.getLang(),
        langValue.getValue(),
        id.getSubjectId(),
        id.getPropertyId(),
        id.getIndex());
  }

  @Override
  public void delete(PropertyValueId<UUID> id) {
    jdbcTemplate.update(
        "delete from scheme_property_value where scheme_id = ? and property_id = ? and index = ?",
        id.getSubjectId(), id.getPropertyId(), id.getIndex());
  }

  @Override
  protected <E> List<E> get(RowMapper<E> mapper) {
    return jdbcTemplate.query("select * from scheme_property_value", mapper);
  }

  @Override
  protected <E> List<E> get(SqlSpecification<PropertyValueId<UUID>, LangValue> specification,
                            RowMapper<E> mapper) {
    return jdbcTemplate.query(
        String.format("select * from scheme_property_value where %s order by index",
                      specification.sqlQueryTemplate()),
        specification.sqlQueryParameters(), mapper);
  }

  @Override
  public boolean exists(PropertyValueId<UUID> id) {
    return jdbcTemplate.queryForObject(
        "select count(*) from scheme_property_value where scheme_id = ? and property_id = ? and index = ?",
        Long.class,
        id.getSubjectId(),
        id.getPropertyId(),
        id.getIndex()) > 0;
  }

  @Override
  protected <E> E get(PropertyValueId<UUID> id, RowMapper<E> mapper) {
    return Iterables.getFirst(jdbcTemplate.query(
        "select * from scheme_property_value scheme_id = ? and property_id = ? and index = ?",
        mapper,
        id.getSubjectId(),
        id.getPropertyId(),
        id.getIndex()), null);
  }

  @Override
  protected RowMapper<PropertyValueId<UUID>> buildKeyMapper() {
    return new RowMapper<PropertyValueId<UUID>>() {
      public PropertyValueId<UUID> mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new PropertyValueId<UUID>(UUIDs.fromString(rs.getString("scheme_id")),
                                         rs.getString("property_id"),
                                         rs.getInt("index"));
      }
    };
  }

  @Override
  protected RowMapper<LangValue> buildValueMapper() {
    return new LangValueRowMapper();
  }

}
