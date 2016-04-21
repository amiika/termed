package fi.thl.termed.service;

import com.google.common.base.Function;
import com.google.common.collect.Sets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import fi.thl.termed.domain.Query;
import fi.thl.termed.domain.Resource;
import fi.thl.termed.domain.ResourceId;
import fi.thl.termed.domain.Scheme;
import fi.thl.termed.domain.User;
import fi.thl.termed.repository.ResourceIndex;
import fi.thl.termed.repository.ResourceRepository;
import fi.thl.termed.repository.SchemeRepository;
import fi.thl.termed.repository.dao.ResourceDao;
import fi.thl.termed.repository.spesification.ResourceSpecificationBySchemeId;
import fi.thl.termed.repository.spesification.Specification;

import static fi.thl.termed.util.ObjectUtils.coalesce;
import static fi.thl.termed.util.UUIDs.nameUUIDFromString;
import static java.util.UUID.randomUUID;
import static org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization;

@Service("schemeService")
@Transactional
public class SchemeServiceImpl implements SchemeService {

  @Autowired
  private SchemeRepository schemeRepository;

  @Autowired
  private ResourceIndex resourceIndex;

  @Autowired
  private ResourceDao resourceDao;

  @Autowired
  private ResourceRepository resourceRepository;

  @Override
  public List<Scheme> get(User currentUser) {
    return schemeRepository.get();
  }

  @Override
  public List<Scheme> get(Specification<UUID, Scheme> specification, User currentUser) {
    return schemeRepository.get(specification);
  }

  @Override
  public List<Scheme> get(Query query, User currentUser) {
    // search not supported, return all schemes
    return schemeRepository.get();
  }

  @Override
  public Scheme get(UUID schemeId, User currentUser) {
    return schemeRepository.get(schemeId);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @Override
  public int save(List<Scheme> schemes, User currentUser) {
    for (Scheme scheme : schemes) {
      save(scheme, currentUser);
    }
    return schemes.size();
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public Scheme save(UUID id, Scheme scheme, User currentUser) {
    scheme.setId(id);
    return save(scheme, currentUser);
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public Scheme save(Scheme scheme, User currentUser) {
    scheme.setId(coalesce(scheme.getId(),
                          nameUUIDFromString(scheme.getCode()),
                          nameUUIDFromString(scheme.getUri()),
                          randomUUID()));

    List<ResourceId> oldResourceIds = getResourceIds(scheme);
    schemeRepository.save(scheme);

    reindexSchemeResourcesAfterCommit(oldResourceIds, getResourceIds(scheme));

    return get(scheme.getId(), currentUser);
  }

  private List<ResourceId> getResourceIds(Scheme scheme) {
    return resourceDao.getKeys(new ResourceSpecificationBySchemeId(scheme.getId()));
  }

  private void reindexSchemeResourcesAfterCommit(final List<ResourceId> oldResourceIds,
                                                 final List<ResourceId> newResourceIds) {
    registerSynchronization(new TransactionSynchronizationAdapter() {
      @Override
      public void afterCommit() {
        cleanUp();
        reindex();
      }

      private void cleanUp() {
        Set<ResourceId> removedIds = Sets.difference(Sets.newHashSet(oldResourceIds),
                                                     Sets.newHashSet(newResourceIds));
        for (ResourceId removedId : removedIds) {
          resourceIndex.deleteFromIndex(removedId);
        }
      }

      private void reindex() {
        if (!newResourceIds.isEmpty()) {
          resourceIndex.reindex(newResourceIds, new Function<List<ResourceId>, List<Resource>>() {
            public List<Resource> apply(List<ResourceId> ids) {
              return resourceRepository.get(ids);
            }
          });
        }
      }
    });
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public void delete(UUID schemeId, User currentUser) {
    List<ResourceId> ids = resourceDao.getKeys(new ResourceSpecificationBySchemeId(schemeId));

    schemeRepository.delete(schemeId);

    for (ResourceId id : ids) {
      resourceIndex.deleteFromIndex(id);
    }
  }

}
