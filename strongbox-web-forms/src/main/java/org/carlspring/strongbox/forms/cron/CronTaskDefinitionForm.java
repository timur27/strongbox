package org.carlspring.strongbox.forms.cron;

import org.carlspring.strongbox.validation.cron.CronTaskDefinitionFormValid;

import java.util.Set;

/**
 * @author Przemyslaw Fusik
 */
@CronTaskDefinitionFormValid(message = "Invalid cron task definition")
public class CronTaskDefinitionForm
{

    private String id;

    private Set<CronTaskDefinitionFormField> fields;

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public Set<CronTaskDefinitionFormField> getFields()
    {
        return fields;
    }

    public void setFields(Set<CronTaskDefinitionFormField> fields)
    {
        this.fields = fields;
    }
}
