package org.carlspring.strongbox.repository;

import org.carlspring.strongbox.config.MavenIndexerEnabledCondition;
import org.carlspring.strongbox.cron.domain.CronTaskConfigurationDto;
import org.carlspring.strongbox.cron.jobs.DownloadRemoteMavenIndexCronJob;
import org.carlspring.strongbox.cron.jobs.RebuildMavenIndexesCronJob;
import org.carlspring.strongbox.cron.services.CronTaskDataService;
import org.carlspring.strongbox.providers.io.RepositoryPath;
import org.carlspring.strongbox.providers.io.RepositoryPathResolver;
import org.carlspring.strongbox.storage.Storage;
import org.carlspring.strongbox.storage.indexing.IndexTypeEnum;
import org.carlspring.strongbox.storage.indexing.RepositoryIndexManager;
import org.carlspring.strongbox.storage.indexing.RepositoryIndexer;
import org.carlspring.strongbox.storage.indexing.RepositoryIndexerFactory;
import org.carlspring.strongbox.storage.repository.Repository;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;

import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

/**
 * @author carlspring
 */
@Component
@Conditional(MavenIndexerEnabledCondition.class)
public class IndexedMavenRepositoryManagementStrategy
        extends MavenRepositoryManagementStrategy
{

    @Inject
    private RepositoryIndexManager repositoryIndexManager;

    @Inject
    private RepositoryIndexerFactory repositoryIndexerFactory;

    @Inject
    private CronTaskDataService cronTaskDataService;

    @Inject
    private IndexedMavenRepositoryFeatures repositoryFeatures;

    @Inject
    private RepositoryPathResolver repositoryPathResolver;

    @Override
    protected void createRepositoryInternal(Storage storage, Repository repository)
            throws IOException, RepositoryManagementStrategyException
    {
        if (repositoryFeatures.isIndexingEnabled(repository))
        {

            String storageId = storage.getId();
            String repositoryId = repository.getId();

            RepositoryPath repositoryBasedir = repositoryPathResolver.resolve(repository);

            if (repository.isProxyRepository())
            {
                // Create a remote index
                createRepositoryIndexer(storageId, repositoryId, IndexTypeEnum.REMOTE.getType(), repositoryBasedir);

                // Create a scheduled task for downloading the remote's index
                createRemoteIndexDownloaderCronTask(storageId, repositoryId);
            }
            
            // Create a local index
            createRepositoryIndexer(storageId, repositoryId, IndexTypeEnum.LOCAL.getType(), repositoryBasedir);

            createRebuildMavenIndexCronJob(storageId, repositoryId);
        }
    }

    @Override
    public void removeRepository(String storageId,
                                 String repositoryId)
        throws IOException
    {
        Storage storage = getStorage(storageId);
        Repository repository = storage.getRepository(repositoryId);
        if (repository.isProxyRepository())
        {
            String remoteContextId = storageId + ":" + repositoryId + ":" + IndexTypeEnum.REMOTE.getType();
            repositoryIndexManager.closeIndexer(remoteContextId);
        }
        String localContextId = storageId + ":" + repositoryId + ":" + IndexTypeEnum.LOCAL.getType();
        repositoryIndexManager.closeIndexer(localContextId);
        
        super.removeRepository(storageId, repositoryId);
    }



    private void createRemoteIndexDownloaderCronTask(String storageId,
                                                     String repositoryId)
            throws RepositoryManagementStrategyException
    {
        CronTaskConfigurationDto configuration = new CronTaskConfigurationDto();
        configuration.setName("Remote index download for " + storageId + ":" + repositoryId);
        configuration.setJobClass(DownloadRemoteMavenIndexCronJob.class.getCanonicalName());
        configuration.setCronExpression("0 0 0 * * ?"); // Execute once daily at 00:00:00
        configuration.addProperty("storageId", storageId);
        configuration.addProperty("repositoryId", repositoryId);
        configuration.setImmediateExecution(true);

        try
        {
            cronTaskDataService.save(configuration);
        }
        catch (Exception e)
        {
            throw new RepositoryManagementStrategyException(e.getMessage(), e);
        }
    }

    private void createRebuildMavenIndexCronJob(String storageId,
                                                String repositoryId)
            throws RepositoryManagementStrategyException
    {
        CronTaskConfigurationDto configuration = new CronTaskConfigurationDto();
        configuration.setName("Rebuild Maven Index Cron Job for " + storageId + ":" + repositoryId);
        configuration.setJobClass(RebuildMavenIndexesCronJob.class.getCanonicalName());
        configuration.setCronExpression("0 0 2 * * ?");
        configuration.addProperty("storageId", storageId);
        configuration.addProperty("repositoryId", repositoryId);
        configuration.setImmediateExecution(true);

        try
        {
            cronTaskDataService.save(configuration);
        }
        catch (Exception e)
        {
            throw new RepositoryManagementStrategyException(e.getMessage(), e);
        }
    }

    public RepositoryIndexer createRepositoryIndexer(String storageId,
                                                     String repositoryId,
                                                     String indexType,
                                                     RepositoryPath repositoryBasedir)
            throws IOException
    {
        RepositoryPath repositoryIndexDir = repositoryBasedir.resolve(".index").resolve(indexType);

        if (!Files.exists(repositoryIndexDir))
        {
            //noinspection ResultOfMethodCallIgnored
            Files.createDirectories(repositoryIndexDir);
        }

        RepositoryIndexer repositoryIndexer = repositoryIndexerFactory.createRepositoryIndexer(storageId,
                                                                                               repositoryId,
                                                                                               indexType,
                                                                                               repositoryBasedir,
                                                                                               repositoryIndexDir);

        String contextId = storageId + ":" + repositoryId + ":" + indexType;

        repositoryIndexManager.addRepositoryIndexer(contextId, repositoryIndexer);

        return repositoryIndexer;
    }

    @Override
    public void createRepositoryStructure(final Repository repository)
            throws IOException
    {
        super.createRepositoryStructure(repository);
        final RepositoryPath rootRepositoryPath = repositoryPathResolver.resolve(repository);
        final RepositoryPath indexRepositoryPath = rootRepositoryPath.resolve(".index");
        if (!Files.exists(indexRepositoryPath))
        {
            Files.createDirectories(indexRepositoryPath);
        }
    }

}
