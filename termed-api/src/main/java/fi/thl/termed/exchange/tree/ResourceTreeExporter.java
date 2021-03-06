package fi.thl.termed.exchange.tree;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import java.util.List;
import java.util.Map;

import fi.thl.termed.domain.Resource;
import fi.thl.termed.domain.ResourceId;
import fi.thl.termed.domain.User;
import fi.thl.termed.exchange.AbstractExporter;
import fi.thl.termed.service.Service;
import fi.thl.termed.util.GraphUtils;
import fi.thl.termed.util.Tree;

/**
 * Export resources with reference/referrer tree
 */
public class ResourceTreeExporter extends AbstractExporter<ResourceId, Resource, List<Resource>> {

  public ResourceTreeExporter(Service<ResourceId, Resource> service) {
    super(service);
  }

  @Override
  protected Map<String, Class> requiredArgs() {
    return ImmutableMap.<String, Class>of("attributeId", String.class, "referrers", Boolean.class);
  }

  @Override
  protected List<Resource> doExport(List<Resource> values, Map<String, Object> args, User user) {
    String attributeId = (String) args.get("attributeId");
    Boolean referrers = (Boolean) args.get("referrers");

    Function<Resource, List<Resource>> referenceLoadingFunction =
        referrers ? new IndexedReferrerLoader(service, user, attributeId)
                  : new IndexedReferenceLoader(service, user, attributeId);

    // function to convert and load resource into tree via attributeId
    Function<Resource, Tree<Resource>> toTree =
        new GraphUtils.ToTreeFunction<Resource>(referenceLoadingFunction);

    // function to convert tree back into resource
    // where resource have refs populated based on the tree
    Function<Tree<Resource>, Resource> fromTree =
        new ToResourceTree(attributeId, referrers);

    return Lists.transform(values, Functions.compose(fromTree, toTree));
  }

  /**
   * Recursively populates resource reference/referrer values from given tree.
   */
  private class ToResourceTree implements Function<Tree<Resource>, Resource> {

    private String attributeId;
    private boolean referrers;

    public ToResourceTree(String attributeId, boolean referrers) {
      this.attributeId = attributeId;
      this.referrers = referrers;
    }

    @Override
    public Resource apply(Tree<Resource> input) {
      Resource resource = new Resource(input.getData());

      if (referrers) {
        Multimap<String, Resource> refs = resource.getReferrers();
        refs.removeAll(attributeId);
        refs.putAll(attributeId, Iterables.transform(input.getChildren(), this));
        resource.setReferrers(refs);
      } else {
        Multimap<String, Resource> refs = resource.getReferences();
        refs.removeAll(attributeId);
        refs.putAll(attributeId, Iterables.transform(input.getChildren(), this));
        resource.setReferences(refs);
      }

      return resource;
    }

  }

}
