package org.carlspring.strongbox.cron.jobs.fields;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * @author Przemyslaw Fusik
 */
@JsonSerialize(using = CronJobFieldJsonSerializer.class)
public abstract class CronJobField
{

    private final CronJobField field;

    public CronJobField(CronJobField field)
    {
        this.field = field;
    }

    public abstract String getKey();

    public abstract String getValue();

    protected CronJobField getField()
    {
        return field;
    }
}
