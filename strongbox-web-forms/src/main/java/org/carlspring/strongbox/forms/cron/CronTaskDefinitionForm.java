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
}
