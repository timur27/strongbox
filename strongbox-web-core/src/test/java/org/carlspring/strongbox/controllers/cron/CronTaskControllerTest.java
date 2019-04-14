package org.carlspring.strongbox.controllers.cron;

import org.carlspring.strongbox.config.IntegrationTest;
import org.carlspring.strongbox.cron.domain.CronTaskConfigurationDto;
import org.carlspring.strongbox.cron.domain.CronTasksConfigurationDto;
import org.carlspring.strongbox.cron.jobs.CleanupExpiredArtifactsFromProxyRepositoriesCronJob;
import org.carlspring.strongbox.cron.jobs.MyTask;
import org.carlspring.strongbox.cron.jobs.RebuildMavenMetadataCronJob;
import org.carlspring.strongbox.cron.jobs.RegenerateChecksumCronJob;
import org.carlspring.strongbox.forms.cron.CronTaskConfigurationForm;
import org.carlspring.strongbox.forms.cron.CronTaskDefinitionForm;
import org.carlspring.strongbox.forms.cron.CronTaskDefinitionFormField;
import org.carlspring.strongbox.rest.common.RestAssuredBaseTest;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import edu.emory.mathcs.backport.java.util.Arrays;
import io.restassured.module.mockmvc.response.MockMvcResponse;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.EnabledIf;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.carlspring.strongbox.controllers.cron.CronTaskController.CRON_CONFIG_FILE_NAME_KEY;
import static org.carlspring.strongbox.controllers.cron.CronTaskController.CRON_CONFIG_JOB_CLASS_KEY;
import static org.carlspring.strongbox.net.MediaType.APPLICATION_YAML_VALUE;
import static org.carlspring.strongbox.rest.client.RestAssuredArtifactClient.OK;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Alex Oreshkevich
 * @author Pablo Tirado
 */
@IntegrationTest
@ActiveProfiles(profiles = "test")
public class CronTaskControllerTest
        extends RestAssuredBaseTest
{

    @Override
    @BeforeEach
    public void init()
            throws Exception
    {
        super.init();
        setContextBaseUrl("/api/configuration/crontasks");
    }

    @Test
    public void getConfigurations()
    {
        MockMvcResponse response = getCronConfigurations();

        assertEquals(OK, response.getStatusCode(), "Failed to get list of cron tasks: " + response.getStatusLine());

        CronTasksConfigurationDto cronTasks = response.as(CronTasksConfigurationDto.class);
        assertFalse(cronTasks.getCronTaskConfigurations().isEmpty(), "List of cron tasks is empty!");
    }

    @Test
    @EnabledIf(expression = "#{containsObject('repositoryIndexManager')}", loadContext = true)
    public void createNewCronJob()
    {
        final String name = "Cron Job Test";
        final String cronExpression = "0 0 0 * * ?";
        final String className = MyTask.class.getName();

        // 1. Create cron job.
        CronTaskConfigurationForm configurationForm = createForm(name, cronExpression, className);
        createConfig(configurationForm);

        final String cronUuid = getCronUuid(name);

        // 2. Delete cron job.
        deleteConfig(cronUuid);
    }

    @Test
    @EnabledIf(expression = "#{containsObject('repositoryIndexManager')}", loadContext = true)
    public void updateDownloadRemoteMavenIndexCronJob()
    {
        final String currentCronExpression = "0 0 5 * * ?";
        final String newCronExpression = "0 0 0 * * ?";

        List<CronTaskConfigurationDto> configurationList = getDownloadRemoteMavenIndexOfCarlspringCronJobs();
        assertFalse(configurationList.isEmpty());

        CronTaskConfigurationDto configuration = configurationList.get(0);
        assertEquals(configuration.getProperties().keySet().size(), 4);
        assertEquals(configuration.getProperties().get("cronExpression"), currentCronExpression);

        configuration.addProperty("cronExpression", newCronExpression);

        CronTaskConfigurationForm configurationForm = convertToForm(configuration);

        given().contentType(MediaType.APPLICATION_JSON_VALUE)
               .accept(MediaType.APPLICATION_JSON_VALUE)
               .body(configurationForm)
               .when()
               .put(getContextBaseUrl() + "/" + configuration.getUuid())
               .peek()
               .then()
               .statusCode(OK);

        configurationList = getDownloadRemoteMavenIndexOfCarlspringCronJobs();
        assertFalse(configurationList.isEmpty());

        configuration = configurationList.get(0);
        assertEquals(configuration.getProperties().keySet().size(), 4);
        assertEquals(configuration.getProperties().get("cronExpression"), newCronExpression);

        // Revert changes
        configuration.addProperty("cronExpression", currentCronExpression);
        configurationForm = convertToForm(configuration);

        given().contentType(MediaType.APPLICATION_JSON_VALUE)
               .accept(MediaType.APPLICATION_JSON_VALUE)
               .body(configurationForm)
               .when()
               .put(getContextBaseUrl() + "/" + configuration.getUuid())
               .peek()
               .then()
               .statusCode(OK);
    }

    @Test
    public void testJavaCronTaskConfiguration()
    {
        final String cronName = "CRJ001";
        final String cronExpression = "0 11 11 11 11 ? 2100";
        final String className = MyTask.class.getName();

        // 1. Create cron config.
        CronTaskConfigurationForm configurationForm = createForm(cronName, cronExpression, className);
        createConfig(configurationForm);

        final String cronUuid = getCronUuid(cronName);

        // 2. Update cron config.
        saveCronConfig(cronUuid, configurationForm);

        // Remove comments to test cron job execution
        // saveCronConfig("0 0/2 * 1/1 * ? *", cronUuid, cronName, MyTask.class.getName());

        deleteConfig(cronUuid);
    }

    @Test
    public void shouldReturnInvalidIdValidationError()
    {
        CronTaskDefinitionForm cronTaskDefinitionForm = new CronTaskDefinitionForm();
        cronTaskDefinitionForm.setId("mummy");

        given().contentType(MediaType.APPLICATION_JSON_VALUE)
               .accept(MediaType.APPLICATION_JSON_VALUE)
               .body(cronTaskDefinitionForm)
               .when()
               .put(getContextBaseUrl() + "/new-way")
               .peek()
               .then()
               .statusCode(HttpStatus.BAD_REQUEST.value())
               .expect(MockMvcResultMatchers.jsonPath("errors[0].messages").value(hasItem("Cron job not found")))
               .expect(MockMvcResultMatchers.jsonPath("errors[0].name").value(equalTo("id")));
    }

    @Test
    public void shouldRequireRequiredFields()
    {
        CronTaskDefinitionForm cronTaskDefinitionForm = new CronTaskDefinitionForm();
        cronTaskDefinitionForm.setId(CleanupExpiredArtifactsFromProxyRepositoriesCronJob.class.getCanonicalName());

        given().contentType(MediaType.APPLICATION_JSON_VALUE)
               .accept(MediaType.APPLICATION_JSON_VALUE)
               .body(cronTaskDefinitionForm)
               .when()
               .put(getContextBaseUrl() + "/new-way")
               .peek()
               .then()
               .statusCode(HttpStatus.BAD_REQUEST.value())
               .expect(MockMvcResultMatchers.jsonPath("errors[0].messages").value(hasItem(stringContainsInOrder(
                       Arrays.asList(new String[]{ "Required field",
                                                   "not provided" })))))
               .expect(MockMvcResultMatchers.jsonPath("errors[0].name").value(equalTo("fields")));
    }

    @Test
    public void valueShouldBeProvidedForRequiredField()
    {
        CronTaskDefinitionForm cronTaskDefinitionForm = new CronTaskDefinitionForm();
        cronTaskDefinitionForm.setId(RebuildMavenMetadataCronJob.class.getCanonicalName());
        cronTaskDefinitionForm.setFields(
                Arrays.asList(new CronTaskDefinitionFormField[]{ CronTaskDefinitionFormField.newBuilder().name(
                        "repositoryId").value("").build() }));

        given().contentType(MediaType.APPLICATION_JSON_VALUE)
               .accept(MediaType.APPLICATION_JSON_VALUE)
               .body(cronTaskDefinitionForm)
               .when()
               .put(getContextBaseUrl() + "/new-way")
               .peek()
               .then()
               .statusCode(HttpStatus.BAD_REQUEST.value())
               .statusCode(HttpStatus.BAD_REQUEST.value())
               .expect(MockMvcResultMatchers.jsonPath("errors[0].messages").value(hasItem(stringContainsInOrder(
                       Arrays.asList(
                               new String[]{ "Required field value [repositoryId] not provided" })))))
               .expect(MockMvcResultMatchers.jsonPath("errors[0].name").value(equalTo("fields[0].value")));
    }

    @Test
    public void shouldValidateIntTypeFields()
    {
        CronTaskDefinitionForm cronTaskDefinitionForm = new CronTaskDefinitionForm();
        cronTaskDefinitionForm.setId(CleanupExpiredArtifactsFromProxyRepositoriesCronJob.class.getCanonicalName());
        cronTaskDefinitionForm.setFields(
                Arrays.asList(new CronTaskDefinitionFormField[]{ CronTaskDefinitionFormField.newBuilder().name(
                        "lastAccessedTimeInDays").value("piecdziesiat").build() }));

        given().contentType(MediaType.APPLICATION_JSON_VALUE)
               .accept(MediaType.APPLICATION_JSON_VALUE)
               .body(cronTaskDefinitionForm)
               .when()
               .put(getContextBaseUrl() + "/new-way")
               .peek()
               .then()
               .statusCode(HttpStatus.BAD_REQUEST.value())
               .expect(MockMvcResultMatchers.jsonPath("errors[0].messages").value(hasItem(stringContainsInOrder(
                       Arrays.asList(
                               new String[]{ "Invalid value [piecdziesiat] type provided. [int] was expected." })))))
               .expect(MockMvcResultMatchers.jsonPath("errors[0].name").value(equalTo("fields[0].value")));
    }

    @Test
    public void shouldValidateBooleanTypeFields()
    {
        CronTaskDefinitionForm cronTaskDefinitionForm = new CronTaskDefinitionForm();
        cronTaskDefinitionForm.setId(RegenerateChecksumCronJob.class.getCanonicalName());
        cronTaskDefinitionForm.setFields(
                Arrays.asList(new CronTaskDefinitionFormField[]{ CronTaskDefinitionFormField.newBuilder().name(
                        "forceRegeneration").value("prawda").build() }));

        given().contentType(MediaType.APPLICATION_JSON_VALUE)
               .accept(MediaType.APPLICATION_JSON_VALUE)
               .body(cronTaskDefinitionForm)
               .when()
               .put(getContextBaseUrl() + "/new-way")
               .peek()
               .then()
               .statusCode(HttpStatus.BAD_REQUEST.value())
               .expect(MockMvcResultMatchers.jsonPath("errors[0].messages").value(hasItem(stringContainsInOrder(
                       Arrays.asList(
                               new String[]{ "Invalid value [prawda] type provided. [boolean] was expected." })))))
               .expect(MockMvcResultMatchers.jsonPath("errors[0].name").value(equalTo("fields[0].value")));
    }

    @Test
    public void testGroovyCronTaskConfiguration()
            throws Exception
    {
        final String cronName = "CRJG001";
        final String cronExpression = "0 11 11 11 11 ? 2100";

        // 1. Create cron config.
        CronTaskConfigurationForm configurationForm = createForm(cronName, cronExpression, null);
        createConfig(configurationForm);

        final String cronUuid = getCronUuid(cronName);

        // 2. Update cron config.
        saveCronConfig(cronUuid, configurationForm);
        uploadGroovyScript(cronUuid);

        // Remove comments to test cron job execution *
        // listOfGroovyScriptsName();
        // saveCronConfig("0 0/2 * 1/1 * ? *", cronUuid, cronName, null);

        deleteConfig(cronUuid);
    }

    @Test
    public void testListCronJobs()
    {
        given().contentType(MediaType.APPLICATION_JSON_VALUE)
               .accept(MediaType.APPLICATION_JSON_VALUE)
               .when()
               .get(getContextBaseUrl() + "/types/list")
               .peek()
               .then()
               .statusCode(OK);
    }

    private CronTaskConfigurationForm createForm(String name,
                                                 String cronExpression,
                                                 String className)
    {
        CronTaskConfigurationForm form = new CronTaskConfigurationForm();
        form.setName(name);
        form.addProperty("cronExpression", cronExpression);

        if (className != null)
        {
            form.addProperty(CRON_CONFIG_JOB_CLASS_KEY, className);
        }

        form.setOneTimeExecution(true);
        form.setImmediateExecution(true);

        return form;
    }

    private void createConfig(CronTaskConfigurationForm configurationForm)
    {
        MockMvcResponse response = createCronConfig(configurationForm);

        assertEquals(OK, response.getStatusCode(), "Failed to create cron config job: " + response.getStatusLine());
    }

    private MockMvcResponse createCronConfig(CronTaskConfigurationForm configurationForm)
    {
        return given().contentType(MediaType.APPLICATION_JSON_VALUE)
                      .accept(MediaType.APPLICATION_JSON_VALUE)
                      .body(configurationForm)
                      .when()
                      .put(getContextBaseUrl() + "/")
                      .peek();
    }

    private List<CronTaskConfigurationDto> getDownloadRemoteMavenIndexOfCarlspringCronJobs()
    {

        final CronTasksConfigurationDto cronTasksConfiguration = given().accept(APPLICATION_YAML_VALUE)
                                                                        .when()
                                                                        .get(getContextBaseUrl() + "/")
                                                                        .peek()
                                                                        .as(CronTasksConfigurationDto.class);

        return cronTasksConfiguration.getCronTaskConfigurations()
                                     .stream()
                                     .filter(p -> "org.carlspring.strongbox.cron.jobs.DownloadRemoteMavenIndexCronJob".equals(
                                             p.getRequiredProperty(CRON_CONFIG_JOB_CLASS_KEY)))
                                     .filter(p -> "storage-common-proxies".equals(p.getProperty("storageId")))
                                     .filter(p -> "carlspring".equals(p.getProperty("repositoryId")))
                                     .collect(Collectors.toList());
    }

    private CronTaskConfigurationForm convertToForm(CronTaskConfigurationDto configuration)
    {
        CronTaskConfigurationForm form = new CronTaskConfigurationForm();
        form.setName(configuration.getName());
        form.setProperties(configuration.getProperties());
        form.setOneTimeExecution(configuration.isOneTimeExecution());
        form.setImmediateExecution(configuration.shouldExecuteImmediately());

        return form;
    }


    private void saveCronConfig(String uuid,
                                CronTaskConfigurationForm form)
    {
        MockMvcResponse response = given().contentType(MediaType.APPLICATION_JSON_VALUE)
                                          .accept(MediaType.APPLICATION_JSON_VALUE)
                                          .body(form)
                                          .when()
                                          .put(getContextBaseUrl() + "/" + uuid)
                                          .peek();

        int status = response.getStatusCode();
        if (OK != status)
        {
            logger.error(status + " | " + response.getStatusLine());
        }

        assertEquals(OK, status, "Failed to schedule job!");

        // Retrieve saved config
        response = getCronConfig(uuid);

        assertEquals(OK, response.getStatusCode(), "Failed to get cron task config! " + response.getStatusLine());

        logger.debug("Retrieved config " + response.getBody().asString());
    }

    private void uploadGroovyScript(String uuid)
            throws Exception
    {
        String fileName = "GroovyTask.groovy";
        File file = new File("target/test-classes/groovy/" + fileName);

        String url = getContextBaseUrl() + "/cron/groovy/" + uuid;

        String contentDisposition = "attachment; filename=\"" + fileName + "\"";
        byte[] bytes;

        try (InputStream is = new FileInputStream(file))
        {
            bytes = IOUtils.toByteArray(is);
        }

        given().contentType(MediaType.APPLICATION_JSON_VALUE)
               .accept(MediaType.APPLICATION_JSON_VALUE)
               .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
               .header(CRON_CONFIG_FILE_NAME_KEY, fileName)
               .body(bytes)
               .when()
               .put(url)
               .peek()
               .then()
               .statusCode(OK);
    }

    /**
     * Retrieve list of Groovy script file names
     */
    private void listOfGroovyScriptsName()
    {
        given().contentType(MediaType.APPLICATION_JSON_VALUE)
               .when()
               .get(getContextBaseUrl() + "/groovy/names")
               .peek()
               .then()
               .statusCode(OK);
    }

    private void deleteConfig(String cronUuid)
    {
        MockMvcResponse response = deleteCronConfig(cronUuid);

        assertEquals(OK, response.getStatusCode(), "Failed to deleteCronConfig job: " + response.getStatusLine());

        // Retrieve deleted config
        response = getCronConfig(cronUuid);

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatusCode(), "Cron task config exists!");
    }

    private MockMvcResponse deleteCronConfig(String uuid)
    {
        return given().contentType(MediaType.APPLICATION_JSON_VALUE)
                      .accept(MediaType.APPLICATION_JSON_VALUE)
                      .when()
                      .delete(getContextBaseUrl() + "/" + uuid)
                      .peek();
    }

    private MockMvcResponse getCronConfigurations()
    {
        return given().accept(MediaType.APPLICATION_JSON_VALUE)
                      .when()
                      .get(getContextBaseUrl())
                      .peek();
    }

    private MockMvcResponse getCronConfig(String uuid)
    {
        return given().contentType(MediaType.APPLICATION_JSON_VALUE)
                      .accept(MediaType.APPLICATION_JSON_VALUE)
                      .when()
                      .get(getContextBaseUrl() + "/" + uuid)
                      .peek();
    }

    private String getCronUuid(String cronName)
    {
        final CronTasksConfigurationDto cronTasksConfiguration = given().accept(MediaType.APPLICATION_JSON_VALUE)
                                                                        .when()
                                                                        .get(getContextBaseUrl())
                                                                        .peek()
                                                                        .as(CronTasksConfigurationDto.class);

        final CronTaskConfigurationDto cronTaskConfiguration = cronTasksConfiguration.getCronTaskConfigurations()
                                                                                     .stream()
                                                                                     .filter(p -> cronName.equals(
                                                                                             p.getName()))
                                                                                     .findFirst().get();

        return cronTaskConfiguration.getUuid();
    }

}
