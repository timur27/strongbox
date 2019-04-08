package org.carlspring.strongbox.validation.cron;

import org.carlspring.strongbox.cron.jobs.CronJobDefinition;
import org.carlspring.strongbox.cron.jobs.CronJobsDefinitionsRegistry;
import org.carlspring.strongbox.forms.cron.CronTaskDefinitionForm;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Optional;

import liquibase.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Przemyslaw Fusik
 */
public class CronTaskDefinitionFormValidator
        implements ConstraintValidator<CronTaskDefinitionFormValid, CronTaskDefinitionForm>
{

    @Autowired
    private CronJobsDefinitionsRegistry cronJobsDefinitionsRegistry;

    @Override
    public boolean isValid(CronTaskDefinitionForm form,
                           ConstraintValidatorContext context)
    {
        String id = StringUtils.trimToEmpty(form.getId());
        Optional<CronJobDefinition> cronJobDefinition = cronJobsDefinitionsRegistry.get(id);
        if (!cronJobDefinition.isPresent())
        {
            context.buildConstraintViolationWithTemplate("Cron job not found")
                   .addPropertyNode("id")
                   .addConstraintViolation();
            return false;
        }

        return true;
    }
}
