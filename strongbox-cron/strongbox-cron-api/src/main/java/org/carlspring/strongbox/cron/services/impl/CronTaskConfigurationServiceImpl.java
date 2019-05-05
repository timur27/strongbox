package org.carlspring.strongbox.cron.services.impl;

import org.carlspring.strongbox.cron.domain.CronTaskConfigurationDto;
import org.carlspring.strongbox.cron.domain.CronTasksConfigurationDto;
import org.carlspring.strongbox.cron.services.CronJobSchedulerService;
import org.carlspring.strongbox.cron.services.CronTaskConfigurationService;
import org.carlspring.strongbox.cron.services.CronTaskDataService;
import org.carlspring.strongbox.event.cron.CronTaskEventListenerRegistry;

import javax.inject.Inject;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.stereotype.Service;

@Service
class CronTaskConfigurationServiceImpl
        implements CronTaskConfigurationService, ApplicationListener<ContextStartedEvent>
{

    private final Logger logger = LoggerFactory.getLogger(CronTaskConfigurationServiceImpl.class);

    @Inject
    protected CronTaskEventListenerRegistry cronTaskEventListenerRegistry;

    @Inject
    private CronTaskDataService cronTaskDataService;

    @Inject
    private CronJobSchedulerService cronJobSchedulerService;

    @Override
    public void onApplicationEvent(ContextStartedEvent event)
    {
        CronTasksConfigurationDto cronTasksConfiguration = getTasksConfigurationDto();

        for (CronTaskConfigurationDto dto : cronTasksConfiguration.getCronTaskConfigurations())
        {
            if (dto.isOneTimeExecution())
            {
                continue;
            }

            cronJobSchedulerService.scheduleJob(dto);
        }
    }

    public UUID saveConfiguration(CronTaskConfigurationDto configuration)
    {
        logger.debug("CronTaskConfigurationService.saveConfiguration()");

        UUID configurationId = cronTaskDataService.save(configuration);
        cronJobSchedulerService.scheduleJob(configuration);

        cronTaskEventListenerRegistry.dispatchCronTaskCreatedEvent(configuration.getUuid());

        return configurationId;
    }

    public void deleteConfiguration(String cronTaskConfigurationUuid)
    {
        logger.debug("Deleting cron task configuration {}", cronTaskConfigurationUuid);

        cronTaskDataService.delete(cronTaskConfigurationUuid);
        cronJobSchedulerService.deleteJob(cronTaskConfigurationUuid);

        cronTaskEventListenerRegistry.dispatchCronTaskDeletedEvent(cronTaskConfigurationUuid);
    }

    @Override
    public CronTaskConfigurationDto getTaskConfigurationDto(String uuid)
    {
        return cronTaskDataService.getTaskConfigurationDto(uuid);
    }

    public CronTasksConfigurationDto getTasksConfigurationDto()
    {
        return cronTaskDataService.getTasksConfigurationDto();
    }

}
