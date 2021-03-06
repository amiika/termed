package fi.thl.termed.spesification.sql;

import com.google.common.base.Objects;

import java.util.UUID;

import fi.thl.termed.domain.Resource;
import fi.thl.termed.domain.ResourceId;

public class ResourcesBySchemeId extends SqlSpecification<ResourceId, Resource> {

  private UUID schemeId;

  public ResourcesBySchemeId(UUID schemeId) {
    this.schemeId = schemeId;
  }

  @Override
  public boolean accept(ResourceId resourceId, Resource resource) {
    return Objects.equal(resourceId.getSchemeId(), schemeId);
  }

  @Override
  public String sqlQueryTemplate() {
    return "scheme_id = ?";
  }

  @Override
  public Object[] sqlQueryParameters() {
    return new Object[]{schemeId};
  }

}
