package org.carlspring.strongbox.validation.cron.type;

import org.springframework.stereotype.Component;

/**
 * @author Przemyslaw Fusik
 */
@Component
public class StringCronTaskDefinitionFormFieldTypeValidator
        implements CronTaskDefinitionFormFieldTypeValidator
{

    @Override
    public boolean isValid(String value)
    {
        return true;
    }

    @Override
    public boolean supports(String type)
    {
        return String.class.getSimpleName().toLowerCase().equals(type);
    }

}
