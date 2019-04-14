package org.carlspring.strongbox.validation.cron.autocomplete;

import org.carlspring.strongbox.services.ConfigurationManagementService;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

/**
 * @author Przemyslaw Fusik
 */
@Component
public class CronTaskDefinitionFormFieldRepositoryIdAutocompleteValidator
        implements CronTaskDefinitionFormFieldAutocompleteValidator
{

    @Inject
    private ConfigurationManagementService configurationManagementService;

    @Override
    public boolean isValid(String value)
    {
        //configurationManagementService.get

        return false;
    }

    @Override
    public boolean supports(String name)
    {
        return "repositoryId".equals(name);
    }
}
