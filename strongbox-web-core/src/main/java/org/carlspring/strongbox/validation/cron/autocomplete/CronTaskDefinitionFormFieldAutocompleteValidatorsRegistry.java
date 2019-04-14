package org.carlspring.strongbox.validation.cron.autocomplete;

import javax.inject.Inject;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * @author Przemyslaw Fusik
 */
@Component
public class CronTaskDefinitionFormFieldAutocompleteValidatorsRegistry
{

    @Inject
    private List<CronTaskDefinitionFormFieldAutocompleteValidator> validators;

    public CronTaskDefinitionFormFieldAutocompleteValidator get(String autocompleteValue)
    {
        return validators.stream()
                         .filter(v -> v.supports(autocompleteValue))
                         .findFirst()
                         .orElseThrow(
                                 () -> new IllegalArgumentException(
                                         String.format("Autocomplete value %s not supported", autocompleteValue)));
    }
}
