package fi.thl.termed.spesification.sql;

import com.google.common.base.Objects;

import java.util.UUID;

import fi.thl.termed.domain.Scheme;

public class SchemeByUri extends SqlSpecification<UUID, Scheme> {

  private String uri;

  public SchemeByUri(String uri) {
    this.uri = uri;
  }

  @Override
  public boolean accept(UUID key, Scheme value) {
    return Objects.equal(value.getCode(), uri);
  }

  @Override
  public String sqlQueryTemplate() {
    return "uri = ?";
  }

  @Override
  public Object[] sqlQueryParameters() {
    return new Object[]{uri};
  }

}
