package org.carlspring.strongbox.validation.cron;

import org.carlspring.strongbox.cron.jobs.CronJobDefinition;
import org.carlspring.strongbox.cron.jobs.CronJobsDefinitionsRegistry;
import org.carlspring.strongbox.cron.jobs.fields.CronJobField;
import org.carlspring.strongbox.forms.cron.CronTaskDefinitionForm;
import org.carlspring.strongbox.forms.cron.CronTaskDefinitionFormField;
import org.carlspring.strongbox.validation.cron.autocomplete.CronTaskDefinitionFormFieldAutocompleteValidator;
import org.carlspring.strongbox.validation.cron.autocomplete.CronTaskDefinitionFormFieldAutocompleteValidatorsRegistry;
import org.carlspring.strongbox.validation.cron.type.CronTaskDefinitionFormFieldTypeValidator;
import org.carlspring.strongbox.validation.cron.type.CronTaskDefinitionFormFieldTypeValidatorsRegistry;

import javax.inject.Inject;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.quartz.CronExpression;

/**
 * @author Przemyslaw Fusik
 */
public class CronTaskDefinitionFormValidator
        implements ConstraintValidator<CronTaskDefinitionFormValid, CronTaskDefinitionForm>
{

    @Inject
    private CronJobsDefinitionsRegistry cronJobsDefinitionsRegistry;

    @Inject
    private CronTaskDefinitionFormFieldTypeValidatorsRegistry cronTaskDefinitionFormFieldTypeValidatorsRegistry;

    @Inject
    private CronTaskDefinitionFormFieldAutocompleteValidatorsRegistry cronTaskDefinitionFormFieldAutocompleteValidatorsRegistry;

    @Override
    public boolean isValid(CronTaskDefinitionForm form,
                           ConstraintValidatorContext context)
    {

        CronJobDefinition cronJobDefinition;
        try
        {
            cronJobDefinition = getCorrespondingCronJobDefinition(form, context);
        }
        catch (CronTaskDefinitionFormValidatorException ex)
        {
            return false;
        }

        boolean isValid = true;
        boolean cronExpressionIsValid = true;

        if (form.isImmediateExecution() &&
            form.isOneTimeExecution() &&
            StringUtils.isNotBlank(form.getCronExpression()))
        {
            context.buildConstraintViolationWithTemplate(
                    "Cron expression should not be provided when both immediateExecution and oneTimeExecution are set to true")
                   .addPropertyNode("cronExpression")
                   .addConstraintViolation();
            isValid = false;
            cronExpressionIsValid = false;
        }

        if (cronExpressionIsValid && StringUtils.isBlank(form.getCronExpression()))
        {
            context.buildConstraintViolationWithTemplate(
                    "Cron expression is required")
                   .addPropertyNode("cronExpression")
                   .addConstraintViolation();
            isValid = false;
            cronExpressionIsValid = false;
        }

        if (cronExpressionIsValid && !CronExpression.isValidExpression(form.getCronExpression()))
        {
            context.buildConstraintViolationWithTemplate(
                    "Cron expression is invalid")
                   .addPropertyNode("cronExpression")
                   .addConstraintViolation();
            isValid = false;
        }

        for (CronJobField definitionField : cronJobDefinition.getFields())
        {
            String definitionFieldName = definitionField.getName();
            CronTaskDefinitionFormField correspondingFormField = null;
            int correspondingFormFieldIndex = -1;
            for (int i = 0; i < form.getFields().size(); i++)
            {
                CronTaskDefinitionFormField formField = form.getFields().get(i);

                String formFieldName = formField.getName();
                if (StringUtils.equals(definitionFieldName, formFieldName))
                {
                    correspondingFormField = formField;
                    correspondingFormFieldIndex = i;
                    break;
                }
            }
            if (correspondingFormField == null)
            {
                if (definitionField.isRequired())
                {
                    context.buildConstraintViolationWithTemplate(
                            String.format("Required field [%s] not provided", definitionFieldName))
                           .addPropertyNode("fields")
                           .addConstraintViolation();
                    isValid = false;
                }
                // field is not required and is not provided
                continue;
            }

            String formFieldValue = correspondingFormField.getValue();
            if (StringUtils.isBlank(formFieldValue) && definitionField.isRequired())
            {
                context.buildConstraintViolationWithTemplate(
                        String.format("Required field value [%s] not provided", definitionFieldName))
                       .addPropertyNode("fields")
                       .addPropertyNode("value")
                       .inIterable().atIndex(correspondingFormFieldIndex)
                       .addConstraintViolation();
                isValid = false;
                continue;
            }

            String definitionFieldType = definitionField.getType();
            CronTaskDefinitionFormFieldTypeValidator cronTaskDefinitionFormFieldTypeValidator = cronTaskDefinitionFormFieldTypeValidatorsRegistry.get(
                    definitionFieldType);
            if (!cronTaskDefinitionFormFieldTypeValidator.isValid(formFieldValue))
            {
                context.buildConstraintViolationWithTemplate(
                        String.format("Invalid value [%s] type provided. [%s] was expected.", formFieldValue,
                                      definitionFieldType))
                       .addPropertyNode("fields")
                       .addPropertyNode("value")
                       .inIterable().atIndex(correspondingFormFieldIndex)
                       .addConstraintViolation();
                isValid = false;
                continue;
            }

            String autocompleteValue = definitionField.getAutocompleteValue();
            if (autocompleteValue != null)
            {
                CronTaskDefinitionFormFieldAutocompleteValidator cronTaskDefinitionFormFieldAutocompleteValidator = cronTaskDefinitionFormFieldAutocompleteValidatorsRegistry.get(
                        autocompleteValue);
                if (!cronTaskDefinitionFormFieldAutocompleteValidator.isValid(formFieldValue))
                {
                    context.buildConstraintViolationWithTemplate(
                            String.format("Invalid value [%s] provided. Possible values do not contain this value.",
                                          formFieldValue))
                           .addPropertyNode("fields")
                           .addPropertyNode("value")
                           .inIterable().atIndex(correspondingFormFieldIndex)
                           .addConstraintViolation();
                    isValid = false;
                    continue;
                }
            }
        }

        return isValid;
    }

    private CronJobDefinition getCorrespondingCronJobDefinition(CronTaskDefinitionForm form,
                                                                ConstraintValidatorContext context)
    {
        String id = StringUtils.trimToEmpty(form.getId());
        Optional<CronJobDefinition> cronJobDefinition = cronJobsDefinitionsRegistry.get(id);
        return cronJobDefinition.orElseThrow(() ->
                                             {
                                                 context.buildConstraintViolationWithTemplate(
                                                         "Cron job not found")
                                                        .addPropertyNode("id")
                                                        .addConstraintViolation();
                                                 return new CronTaskDefinitionFormValidatorException();
                                             }
        );
    }

    static class CronTaskDefinitionFormValidatorException
            extends RuntimeException
    {

    }
}
