package org.carlspring.strongbox.cron.jobs;

import org.carlspring.strongbox.cron.domain.CronTaskConfigurationDto;
import org.carlspring.strongbox.cron.services.CronTaskConfigurationService;
import org.carlspring.strongbox.event.cron.CronTaskEvent;
import org.carlspring.strongbox.event.cron.CronTaskEventListenerRegistry;
import org.carlspring.strongbox.event.cron.CronTaskEventTypeEnum;
import org.carlspring.strongbox.testing.TestCaseWithNugetPackageGeneration;

import javax.inject.Inject;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @author carlspring
 */
public class BaseCronJobWithNugetIndexingTestCase
        extends TestCaseWithNugetPackageGeneration implements ApplicationListener<CronTaskEvent>, ApplicationContextAware
{
    public static final long CRON_TASK_CHECK_INTERVAL = 500L;

    @Inject
    protected CronTaskEventListenerRegistry cronTaskEventListenerRegistry;

    @Inject
    protected CronTaskConfigurationService cronTaskConfigurationService;

    /**
     * A map containing the cron task configurations used by this test.
     */
    protected Map<String, CronTaskConfigurationDto> cronTaskConfigurations = new LinkedHashMap<>();

    protected int expectedEventType = CronTaskEventTypeEnum.EVENT_CRON_TASK_EXECUTION_COMPLETE.getType();

    protected CronTaskEvent receivedEvent;

    protected boolean receivedExpectedEvent = false;

    protected String expectedJobName;

    public void init(TestInfo testInfo)
            throws Exception
    {
        Optional<Method> method = testInfo.getTestMethod();
        expectedJobName = method.map(Method::getName).orElse(null);
    }
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
        throws BeansException
    {
        ((ConfigurableApplicationContext) applicationContext).addApplicationListener(this);        
    }

    @Override
    public void onApplicationEvent(CronTaskEvent event)
    {
        handle(event);
    }

    protected CronTaskConfigurationDto addCronJobConfig(String jobName,
                                                        Class<? extends JavaCronJob> className,
                                                        String storageId,
                                                        String repositoryId)
            throws Exception
    {
        return addCronJobConfig(jobName,className,storageId, repositoryId,null);
    }

    protected CronTaskConfigurationDto addCronJobConfig(String jobName,
                                                        Class<? extends JavaCronJob> className,
                                                        String storageId,
                                                        String repositoryId,
                                                        Consumer<Map<String, String>> additionalProperties)
            throws Exception
    {
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("storageId", storageId);
        properties.put("repositoryId", repositoryId);
        if (additionalProperties != null)
        {
            additionalProperties.accept(properties);
        }
        return addCronJobConfig(jobName, className, properties);
    }

    protected CronTaskConfigurationDto addCronJobConfig(String jobName, Class<? extends JavaCronJob> className, Map<String, String> properties)
            throws Exception
    {
        CronTaskConfigurationDto cronTaskConfiguration = new CronTaskConfigurationDto();
        cronTaskConfiguration.setCronExpression("0 11 11 11 11 ? 2100");
        cronTaskConfiguration.setOneTimeExecution(true);
        cronTaskConfiguration.setImmediateExecution(true);
        cronTaskConfiguration.setUuid(jobName);
        cronTaskConfiguration.setName(jobName);
        cronTaskConfiguration.addProperty("jobClass", className.getName());

        for (String propertyKey : properties.keySet())
        {
            cronTaskConfiguration.addProperty(propertyKey, properties.get(propertyKey));
        }

        cronTaskConfigurationService.saveConfiguration(cronTaskConfiguration);

        addCronTaskConfiguration(jobName, cronTaskConfiguration);

        return cronTaskConfiguration;
    }

    public void handle(CronTaskEvent event)
    {
        if (event.getType() == expectedEventType && expectedJobName.equals(event.getName()))
        {
            receivedExpectedEvent = true;
            receivedEvent = event;
        }
    }

    public boolean expectEvent()
            throws InterruptedException
    {
        return expectEvent(5000, CRON_TASK_CHECK_INTERVAL);
    }

    /**
     * Waits for an event to occur.
     *
     * @param maxWaitTime           The maximum wait time (in milliseconds)
     * @param checkInterval         The interval (in milliseconds) at which to check for the occurrence of the event
     */
    public boolean expectEvent(long maxWaitTime, long checkInterval) throws InterruptedException
    {
        int totalWait = 0;
        while (!receivedExpectedEvent &&
               (maxWaitTime > 0 && totalWait <= maxWaitTime || // If a maxWaitTime has been defined,
                maxWaitTime == 0 ))                            // otherwise, default to forever
        {
            Thread.sleep(checkInterval);
            totalWait += checkInterval;
        }

        return receivedExpectedEvent;
    }

    public CronTaskConfigurationDto getCronTaskConfiguration(String key)
    {
        return cronTaskConfigurations.get(key);
    }

    public CronTaskConfigurationDto addCronTaskConfiguration(String key,
                                                             CronTaskConfigurationDto value)
    {
        return cronTaskConfigurations.put(key, value);
    }

    public CronTaskConfigurationDto removeCronTaskConfiguration(String key)
    {
        return cronTaskConfigurations.remove(key);
    }

    public int getExpectedEventType()
    {
        return expectedEventType;
    }

    public void setExpectedEventType(int expectedEventType)
    {
        this.expectedEventType = expectedEventType;
    }

    public CronTaskEvent getReceivedEvent()
    {
        return receivedEvent;
    }

    public void setReceivedEvent(CronTaskEvent receivedEvent)
    {
        this.receivedEvent = receivedEvent;
    }

    public boolean isReceivedExpectedEvent()
    {
        return receivedExpectedEvent;
    }

    public void setReceivedExpectedEvent(boolean receivedExpectedEvent)
    {
        this.receivedExpectedEvent = receivedExpectedEvent;
    }

}
