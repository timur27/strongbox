package org.carlspring.strongbox.forms.cron;

import org.carlspring.strongbox.validation.cron.CronTaskDefinitionFormValid;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.ObjectUtils;


/**
 * @author Przemyslaw Fusik
 */
@CronTaskDefinitionFormValid(message = "Invalid cron task definition")
public class CronTaskDefinitionForm
{

    private String id;

    private String cronExpression;

    private boolean oneTimeExecution;

    private boolean immediateExecution;

    private List<CronTaskDefinitionFormField> fields;

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public List<CronTaskDefinitionFormField> getFields()
    {
        return ObjectUtils.defaultIfNull(fields, Collections.emptyList());
    }

    public void setFields(List<CronTaskDefinitionFormField> fields)
    {
        this.fields = fields;
    }

    public String getCronExpression()
    {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression)
    {
        this.cronExpression = cronExpression;
    }

    public boolean isOneTimeExecution()
    {
        return oneTimeExecution;
    }

    public void setOneTimeExecution(boolean oneTimeExecution)
    {
        this.oneTimeExecution = oneTimeExecution;
    }

    public boolean isImmediateExecution()
    {
        return immediateExecution;
    }

    public void setImmediateExecution(boolean immediateExecution)
    {
        this.immediateExecution = immediateExecution;
    }
}
