package org.carlspring.strongbox.cron.jobs;

import org.carlspring.strongbox.cron.domain.CronTaskConfigurationDto;
import org.carlspring.strongbox.cron.jobs.fields.*;
import org.carlspring.strongbox.providers.repository.proxied.LocalStorageProxyRepositoryExpiredArtifactsCleaner;

import javax.inject.Inject;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

/**
 * @author Przemyslaw Fusik
 */
public class CleanupExpiredArtifactsFromProxyRepositoriesCronJob
        extends JavaCronJob
{

    private static final String PROPERTY_LAST_ACCESSED_TIME_IN_DAYS = "lastAccessedTimeInDays";

    private static final String PROPERTY_MIN_SIZE_IN_BYTES = "minSizeInBytes";

    private static final Set<CronJobField> FIELDS = ImmutableSet.of(
            new CronJobIntegerTypeField(
                    new CronJobRequiredField(new CronJobNamedField(PROPERTY_LAST_ACCESSED_TIME_IN_DAYS))),
            new CronJobIntegerTypeField(
                    new CronJobOptionalField(new CronJobNamedField(PROPERTY_MIN_SIZE_IN_BYTES))));

    @Inject
    private LocalStorageProxyRepositoryExpiredArtifactsCleaner proxyRepositoryObsoleteArtifactsCleaner;

    @Override
    public void executeTask(final CronTaskConfigurationDto config)
            throws Throwable
    {
        final String lastAccessedTimeInDaysText = config.getRequiredProperty(PROPERTY_LAST_ACCESSED_TIME_IN_DAYS);
        final String minSizeInBytesText = config.getProperty(PROPERTY_MIN_SIZE_IN_BYTES);

        final Integer lastAccessedTimeInDays;
        try
        {
            lastAccessedTimeInDays = Integer.valueOf(lastAccessedTimeInDaysText);
        }
        catch (NumberFormatException ex)
        {
            logger.error("Invalid integer value [" + lastAccessedTimeInDaysText +
                         "] of 'lastAccessedTimeInDays' property. Cron job won't be fired.", ex);
            return;
        }

        Long minSizeInBytes = Long.valueOf(-1);
        if (minSizeInBytesText != null)
        {
            try
            {
                minSizeInBytes = Long.valueOf(minSizeInBytesText);
            }
            catch (NumberFormatException ex)
            {
                logger.error("Invalid Long value [" + minSizeInBytesText +
                             "] of 'minSizeInBytes' property. Cron job won't be fired.", ex);
                return;
            }
        }

        proxyRepositoryObsoleteArtifactsCleaner.cleanup(lastAccessedTimeInDays, minSizeInBytes);
    }

    @Override
    public CronJobDefinition getCronJobDefinition()
    {
        return CronJobDefinition.newBuilder()
                                .jobClass(CleanupExpiredArtifactsFromProxyRepositoriesCronJob.class.getCanonicalName())
                                .name("Cleanup Expired Artifacts From Proxy Repositories Cron Job")
                                .description("Cleanup Expired Artifacts From Proxy Repositories Cron Job")
                                .fields(FIELDS)
                                .build();
    }

}
