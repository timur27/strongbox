package org.carlspring.strongbox.validation.cron;

import org.carlspring.strongbox.cron.jobs.CronJobDefinition;
import org.carlspring.strongbox.cron.jobs.CronJobsDefinitionsRegistry;
import org.carlspring.strongbox.cron.jobs.fields.CronJobField;
import org.carlspring.strongbox.forms.cron.CronTaskDefinitionForm;
import org.carlspring.strongbox.forms.cron.CronTaskDefinitionFormField;
import org.carlspring.strongbox.validation.cron.autocomplete.CronTaskDefinitionFormFieldAutocompleteValidatorsRegistry;
import org.carlspring.strongbox.validation.cron.type.CronTaskDefinitionFormFieldTypeValidator;
import org.carlspring.strongbox.validation.cron.type.CronTaskDefinitionFormFieldTypeValidatorsRegistry;

import javax.inject.Inject;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

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

            if (definitionField.isRequired())
            {
                if (correspondingFormField == null)
                {
                    context.buildConstraintViolationWithTemplate(
                            String.format("Required field [%s] not provided", definitionFieldName))
                           .addPropertyNode("fields")
                           .addConstraintViolation();
                    return false;
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
                    return false;
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
                return false;
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
                return false;
            }

            String autocompleteValue = definitionField.getAutocompleteValue();
            if (autocompleteValue != null)
            {
                cronTaskDefinitionFormFieldAutocompleteValidatorsRegistry.get(autocompleteValue);
            }

            // TODO validate composite autocomplete
        }

        return true;
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
