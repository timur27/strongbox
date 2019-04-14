package org.carlspring.strongbox.validation.cron.type;

/**
 * @author Przemyslaw Fusik
 */
public interface CronTaskDefinitionFormFieldTypeValidator
{

    boolean isValid(String value);

    boolean supports(String type);
}
