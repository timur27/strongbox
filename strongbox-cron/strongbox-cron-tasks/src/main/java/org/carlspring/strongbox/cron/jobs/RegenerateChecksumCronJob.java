package org.carlspring.strongbox.cron.jobs;

import org.carlspring.strongbox.configuration.ConfigurationManager;
import org.carlspring.strongbox.cron.domain.CronTaskConfigurationDto;
import org.carlspring.strongbox.cron.jobs.fields.*;
import org.carlspring.strongbox.services.ChecksumService;
import org.carlspring.strongbox.storage.Storage;
import org.carlspring.strongbox.storage.repository.Repository;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

/**
 * @author Kate Novik.
 */
public class RegenerateChecksumCronJob
        extends JavaCronJob
{

    private static final String PROPERTY_STORAGE_ID = "storageId";

    private static final String PROPERTY_REPOSITORY_ID = "repositoryId";

    private static final String PROPERTY_BASE_PATH = "basePath";

    private static final String PROPERTY_FORCE_REGENERATION = "forceRegeneration";

    private static final Set<CronJobField> FIELDS = ImmutableSet.of(
            new CronJobStorageIdAutocompleteField(new CronJobStringTypeField(
                    new CronJobOptionalField(new CronJobNamedField(PROPERTY_STORAGE_ID)))),
            new CronJobRepositoryIdAutocompleteField(new CronJobStringTypeField(
                    new CronJobOptionalField(new CronJobNamedField(PROPERTY_REPOSITORY_ID)))),
            new CronJobBooleanTypeField(
                    new CronJobOptionalField(new CronJobNamedField(PROPERTY_FORCE_REGENERATION))),
            new CronJobStringTypeField(
                    new CronJobOptionalField(new CronJobNamedField(PROPERTY_BASE_PATH))));

    @Inject
    private ChecksumService checksumService;

    @Inject
    private ConfigurationManager configurationManager;

    @Override
    public void executeTask(CronTaskConfigurationDto config)
            throws Throwable
    {
        String storageId = config.getProperty(PROPERTY_STORAGE_ID);
        String repositoryId = config.getProperty(PROPERTY_REPOSITORY_ID);
        String basePath = config.getProperty(PROPERTY_BASE_PATH);

        /**
         * The values of forceRegeneration are:
         * - true  - to re-write existing checksum and to regenerate missing checksum,
         * - false - to regenerate missing checksum only
         */
        boolean forceRegeneration = Boolean.valueOf(config.getProperty(PROPERTY_FORCE_REGENERATION));

        if (storageId == null)
        {
            Map<String, Storage> storages = getStorages();
            for (String storage : storages.keySet())
            {
                regenerateRepositoriesChecksum(storage, forceRegeneration);
            }
        }
        else if (repositoryId == null)
        {
            regenerateRepositoriesChecksum(storageId, forceRegeneration);
        }
        else
        {
            checksumService.regenerateChecksum(storageId, repositoryId, basePath, forceRegeneration);
        }
    }

    @Override
    public CronJobDefinition getCronJobDefinition()
    {
        return CronJobDefinition.newBuilder()
                                .jobClass(RegenerateChecksumCronJob.class.getCanonicalName())
                                .name("Regenerate Checksum Cron Job")
                                .description("Regenerate Checksum Cron Job")
                                .fields(FIELDS)
                                .build();
    }

    /**
     * To regenerate artifact's checksum in repositories
     *
     * @param storageId         path of storage
     * @param forceRegeneration true - to re-write existing checksum and to regenerate missing checksum,
     *                          false - to regenerate missing checksum only
     * @throws IOException
     */
    private void regenerateRepositoriesChecksum(String storageId,
                                                boolean forceRegeneration)
            throws IOException
    {
        Map<String, ? extends Repository> repositories = getRepositories(storageId);

        for (String repositoryId : repositories.keySet())
        {
            checksumService.regenerateChecksum(storageId, repositoryId, null, forceRegeneration);
        }
    }

    private Map<String, Storage> getStorages()
    {
        return configurationManager.getConfiguration().getStorages();
    }

    private Map<String, ? extends Repository> getRepositories(String storageId)
    {
        return getStorages().get(storageId).getRepositories();
    }


}
