package org.carlspring.strongbox.services.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.carlspring.strongbox.artifact.ArtifactTag;
import org.carlspring.strongbox.artifact.coordinates.ArtifactCoordinates;
import org.carlspring.strongbox.domain.ArtifactEntry;
import org.carlspring.strongbox.domain.ArtifactTagEntry;
import org.carlspring.strongbox.domain.RepositoryArtifactIdGroup;
import org.carlspring.strongbox.services.ArtifactEntryService;
import org.carlspring.strongbox.services.ArtifactTagService;
import org.carlspring.strongbox.services.RepositoryArtifactIdGroupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.google.common.collect.Sets;
import com.orientechnologies.common.concur.ONeedRetryException;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

/**
 * @author Przemyslaw Fusik
 * @author sbespalov
 */
@Service
@Transactional
public class RepositoryArtifactIdGroupServiceImpl
        extends AbstractArtifactGroupService<RepositoryArtifactIdGroup>
        implements RepositoryArtifactIdGroupService
{

    private static final Logger logger = LoggerFactory.getLogger(RepositoryArtifactIdGroup.class);
    
    @Inject
    private ArtifactTagService artifactTagService;

    @Inject
    private ArtifactEntryService artifactEntryService;
    
    @Override
    public void addArtifactToGroup(RepositoryArtifactIdGroup artifactGroup,
                                   ArtifactEntry artifactEntry)
    {
        ArtifactCoordinates coordinates = artifactEntry.getArtifactCoordinates();
        Assert.notNull(coordinates, "coordinates should not be null");

        ArtifactTag lastVersionTag = artifactTagService.findOneOrCreate(ArtifactTagEntry.LAST_VERSION);

        Set<ArtifactEntry> lastVersionEntries = findLastVersionArtifactEntries(artifactGroup,
                                                                               lastVersionTag,
                                                                               artifactEntry);
        lastVersionEntries.stream()
                          .map(lastVersionEntry -> checkAndUpdateLastVersionTagIfNeeded(lastVersionEntry, artifactEntry,
                                                                                        lastVersionTag))
                          .distinct()
                          .forEach(artifactEntryService::save);

        artifactGroup.addArtifactEntry(artifactEntry);
        save(artifactGroup);
    }

    private <S extends ArtifactEntry> S checkAndUpdateLastVersionTagIfNeeded(S lastVersionEntry,
                                                                             S entity,
                                                                             ArtifactTag lastVersionTag)
    {
        S result = entity;
        int artifactCoordinatesComparison = entity.getArtifactCoordinates()
                                                  .compareTo(lastVersionEntry.getArtifactCoordinates());

        ArtifactCoordinates coordinates = entity.getArtifactCoordinates();
        if (artifactCoordinatesComparison == 0)
        {
            logger.debug(String.format("Set [%s] last version to [%s]",
                                       entity.getArtifactPath(),
                                       coordinates.getVersion()));
            entity.getTagSet().add(lastVersionTag);
        }
        else if (artifactCoordinatesComparison > 0)
        {
            logger.debug(String.format("Update [%s] last version from [%s] to [%s]",
                                       entity.getArtifactPath(),
                                       lastVersionEntry.getArtifactCoordinates().getVersion(),
                                       coordinates.getVersion()));
            entity.getTagSet().add(lastVersionTag);

            lastVersionEntry.getTagSet().remove(lastVersionTag);

            result = lastVersionEntry;
        }
        else
        {
            logger.debug(String.format("Keep [%s] last version [%s]",
                                       entity.getArtifactPath(),
                                       lastVersionEntry.getArtifactCoordinates().getVersion()));
            entity.getTagSet().remove(lastVersionTag);
        }
        
        return result;
    }

    private <S extends ArtifactEntry> Set<ArtifactEntry> findLastVersionArtifactEntries(RepositoryArtifactIdGroup artifactGroup,
                                                                                        ArtifactTag lastVersionTag,
                                                                                        S defaultArtifactEntry)
    {
        Set<ArtifactEntry> result = artifactGroup.getArtifactEntries()
                                                 .stream()
                                                 .filter(artifactEntry -> artifactEntry.getTagSet()
                                                                                       .contains(lastVersionTag))
                                                 .collect(Collectors.toSet());

        if (result.size() == 0)
        {
            result = Sets.newHashSet(defaultArtifactEntry);
        }

        return result;
    }
    
    public RepositoryArtifactIdGroup findOneOrCreate(String storageId,
                                                     String repositoryId,
                                                     String artifactId)
    {
        Optional<RepositoryArtifactIdGroup> optional = tryFind(storageId, repositoryId, artifactId);
        if (optional.isPresent())
        {
            return optional.get();
        }

        RepositoryArtifactIdGroup artifactGroup = create(storageId, repositoryId, artifactId);

        try
        {
            return save(artifactGroup);
        }
        catch (ONeedRetryException ex)
        {
            optional = tryFind(storageId, repositoryId, artifactId);
            if (optional.isPresent())
            {
                return optional.get();
            }
            throw ex;
        }
    }

    protected RepositoryArtifactIdGroup create(String storageId,
                                               String repositoryId,
                                               String artifactId)
    {
        return new RepositoryArtifactIdGroup(storageId, repositoryId, artifactId);
    }

    protected Optional<RepositoryArtifactIdGroup> tryFind(String storageId,
                                                          String repositoryId,
                                                          String artifactId)
    {
        Map<String, String> params = new HashMap<>();
        params.put("storageId", storageId);
        params.put("repositoryId", repositoryId);
        params.put("id", artifactId);

        String sQuery = buildQuery(params);

        OSQLSynchQuery<ODocument> oQuery = new OSQLSynchQuery<>(sQuery);
        oQuery.setLimit(1);

        List<RepositoryArtifactIdGroup> resultList = getDelegate().command(oQuery)
                                                                  .execute(params);
        return resultList.stream().findFirst();
    }

}
