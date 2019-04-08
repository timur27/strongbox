package org.carlspring.strongbox.cron.jobs;

import org.carlspring.strongbox.cron.domain.CronTaskConfigurationDto;
import org.carlspring.strongbox.cron.jobs.fields.CronJobField;
import org.carlspring.strongbox.cron.jobs.fields.CronJobNamedField;
import org.carlspring.strongbox.cron.jobs.fields.CronJobRequiredField;
import org.carlspring.strongbox.cron.jobs.fields.CronJobStringTypeField;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyCodeSource;

/**
 * @author carlspring
 * @author Yougeshwar
 */
public class GroovyCronJob
        extends AbstractCronJob
{

    private static final String PATH_PROPERTY = "script.path";

    private static final Set<CronJobField> FIELDS = ImmutableSet.of(new CronJobStringTypeField(
            new CronJobRequiredField(new CronJobNamedField(PATH_PROPERTY))));

    @Override
    @SuppressFBWarnings(value = "DP_CREATE_CLASSLOADER_INSIDE_DO_PRIVILEGED")
    public void executeTask(CronTaskConfigurationDto config)
            throws Throwable
    {
        try
        {
            Class scriptClass = new GroovyClassLoader().parseClass(
                    new GroovyCodeSource(Paths.get(getScriptPath(config)).toUri()));
            Object scriptInstance = scriptClass.getConstructor().newInstance();
            //noinspection unchecked
            scriptClass.getDeclaredMethod("execute", new Class[]{}).invoke(scriptInstance);
        }
        catch (IOException | IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e)
        {
            logger.error("IOException: ", e);
        }
    }

    @Override
    public CronJobDefinition getCronJobDefinition()
    {
        return CronJobDefinition.newBuilder()
                                .id(GroovyCronJob.class.getCanonicalName())
                                .name("Groovy Cron Job")
                                .description("Groovy Cron Job")
                                .fields(FIELDS)
                                .build();
    }

    public String getScriptPath(CronTaskConfigurationDto configuration)
    {
        return configuration.getRequiredProperty(PATH_PROPERTY);
    }

}
