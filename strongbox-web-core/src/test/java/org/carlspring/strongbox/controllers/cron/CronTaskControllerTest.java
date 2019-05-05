package org.carlspring.strongbox.controllers.cron;

import org.carlspring.strongbox.config.IntegrationTest;
import org.carlspring.strongbox.cron.domain.CronTaskConfigurationDto;
import org.carlspring.strongbox.cron.domain.CronTasksConfigurationDto;
import org.carlspring.strongbox.cron.jobs.CleanupExpiredArtifactsFromProxyRepositoriesCronJob;
import org.carlspring.strongbox.cron.jobs.RebuildMavenIndexesCronJob;
import org.carlspring.strongbox.cron.jobs.RebuildMavenMetadataCronJob;
import org.carlspring.strongbox.cron.jobs.RegenerateChecksumCronJob;
import org.carlspring.strongbox.forms.cron.CronTaskConfigurationForm;
import org.carlspring.strongbox.forms.cron.CronTaskConfigurationFormField;
import org.carlspring.strongbox.rest.common.RestAssuredBaseTest;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import com.fasterxml.jackson.core.JsonProcessingException;
import edu.emory.mathcs.backport.java.util.Arrays;
import io.restassured.http.Headers;
import io.restassured.module.mockmvc.response.MockMvcResponse;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.carlspring.strongbox.controllers.cron.CronTaskController.CRON_CONFIG_FILE_NAME_KEY;
import static org.carlspring.strongbox.controllers.cron.CronTaskController.HEADER_NAME_CRON_TASK_ID;
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

    /* TODO FIXME
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
    */

    @Test
    public void shouldReturnInvalidIdValidationError()
    {
        CronTaskConfigurationForm cronTaskConfigurationForm = new CronTaskConfigurationForm();
        cronTaskConfigurationForm.setJobClass("mummy");

        given().contentType(MediaType.APPLICATION_JSON_VALUE)
               .accept(MediaType.APPLICATION_JSON_VALUE)
               .body(cronTaskConfigurationForm)
               .when()
               .put(getContextBaseUrl())
               .peek()
               .then()
               .statusCode(HttpStatus.BAD_REQUEST.value())
               .expect(MockMvcResultMatchers.jsonPath("errors[0].messages").value(hasItem("Cron job not found")))
               .expect(MockMvcResultMatchers.jsonPath("errors[0].name").value(equalTo("jobClass")));
    }

    @Test
    public void shouldRequireRequiredFields()
    {
        CronTaskConfigurationForm cronTaskConfigurationForm = new CronTaskConfigurationForm();
        cronTaskConfigurationForm.setJobClass(
                CleanupExpiredArtifactsFromProxyRepositoriesCronJob.class.getCanonicalName());
        cronTaskConfigurationForm.setCronExpression("0 11 11 11 11 ? 2100");

        given().contentType(MediaType.APPLICATION_JSON_VALUE)
               .accept(MediaType.APPLICATION_JSON_VALUE)
               .body(cronTaskConfigurationForm)
               .when()
               .put(getContextBaseUrl())
               .peek()
               .then()
               .statusCode(HttpStatus.BAD_REQUEST.value())
               .expect(MockMvcResultMatchers.jsonPath("errors[0].messages").value(hasItem(stringContainsInOrder(
                       Arrays.asList(new String[]{ "Required field",
                                                   "not provided" })))))
               .expect(MockMvcResultMatchers.jsonPath("errors[0].name").value(equalTo("fields")));
    }

    @Test
    public void cronExpressionIsRequired()
    {
        CronTaskConfigurationForm cronTaskConfigurationForm = new CronTaskConfigurationForm();
        cronTaskConfigurationForm.setJobClass(RebuildMavenIndexesCronJob.class.getCanonicalName());
        cronTaskConfigurationForm.setFields(Arrays.asList(
                new CronTaskConfigurationFormField[]{ CronTaskConfigurationFormField.newBuilder().name(
                        "storageId").value(
                        "storage0").build(),
                                                      CronTaskConfigurationFormField.newBuilder().name(
                                                              "repositoryId").value(
                                                              "releases").build() }));

        given().contentType(MediaType.APPLICATION_JSON_VALUE)
               .accept(MediaType.APPLICATION_JSON_VALUE)
               .body(cronTaskConfigurationForm)
               .when()
               .put(getContextBaseUrl())
               .peek()
               .then()
               .statusCode(HttpStatus.BAD_REQUEST.value())
               .expect(MockMvcResultMatchers.jsonPath("errors[0].messages").value(hasItem(stringContainsInOrder(
                       Arrays.asList(
                               new String[]{ "Cron expression is required" })))))
               .expect(MockMvcResultMatchers.jsonPath("errors[0].name").value(equalTo("cronExpression")));
    }

    @Test
    public void cronExpressionIsValidatable()
    {
        CronTaskConfigurationForm cronTaskConfigurationForm = new CronTaskConfigurationForm();
        cronTaskConfigurationForm.setJobClass(RebuildMavenIndexesCronJob.class.getCanonicalName());
        cronTaskConfigurationForm.setCronExpression("daddy");
        cronTaskConfigurationForm.setFields(Arrays.asList(
                new CronTaskConfigurationFormField[]{ CronTaskConfigurationFormField.newBuilder().name(
                        "storageId").value(
                        "storage0").build(),
                                                      CronTaskConfigurationFormField.newBuilder().name(
                                                              "repositoryId").value(
                                                              "releases").build() }));

        given().contentType(MediaType.APPLICATION_JSON_VALUE)
               .accept(MediaType.APPLICATION_JSON_VALUE)
               .body(cronTaskConfigurationForm)
               .when()
               .put(getContextBaseUrl())
               .peek()
               .then()
               .statusCode(HttpStatus.BAD_REQUEST.value())
               .expect(MockMvcResultMatchers.jsonPath("errors[0].messages").value(hasItem(stringContainsInOrder(
                       Arrays.asList(
                               new String[]{ "Cron expression is invalid" })))))
               .expect(MockMvcResultMatchers.jsonPath("errors[0].name").value(equalTo("cronExpression")));
    }

    @Test
    public void cronExpressionShouldNotBeProvidedIfOneTimeExecutionAndImmediateExecutionAreSet()
    {
        CronTaskConfigurationForm cronTaskConfigurationForm = new CronTaskConfigurationForm();
        cronTaskConfigurationForm.setFields(Arrays.asList(
                new CronTaskConfigurationFormField[]{ CronTaskConfigurationFormField.newBuilder().name(
                        "storageId").value(
                        "storage0").build(),
                                                      CronTaskConfigurationFormField.newBuilder().name(
                                                              "repositoryId").value(
                                                              "releases").build() }));

        cronTaskConfigurationForm.setJobClass(RebuildMavenIndexesCronJob.class.getCanonicalName());
        cronTaskConfigurationForm.setCronExpression("0 11 11 11 11 ? 2100");
        cronTaskConfigurationForm.setImmediateExecution(true);
        cronTaskConfigurationForm.setOneTimeExecution(true);

        given().contentType(MediaType.APPLICATION_JSON_VALUE)
               .accept(MediaType.APPLICATION_JSON_VALUE)
               .body(cronTaskConfigurationForm)
               .when()
               .put(getContextBaseUrl())
               .peek()
               .then()
               .statusCode(HttpStatus.BAD_REQUEST.value())
               .expect(MockMvcResultMatchers.jsonPath("errors[0].messages").value(hasItem(stringContainsInOrder(
                       Arrays.asList(
                               new String[]{ "Cron expression should not be provided when both immediateExecution and oneTimeExecution are set to true" })))))
               .expect(MockMvcResultMatchers.jsonPath("errors[0].name").value(equalTo("cronExpression")));
    }

    @Test
    public void valueShouldBeProvidedForRequiredField()
    {
        CronTaskConfigurationForm cronTaskConfigurationForm = new CronTaskConfigurationForm();
        cronTaskConfigurationForm.setJobClass(RebuildMavenIndexesCronJob.class.getCanonicalName());
        cronTaskConfigurationForm.setCronExpression("0 11 11 11 11 ? 2100");
        cronTaskConfigurationForm.setFields(Arrays.asList(
                new CronTaskConfigurationFormField[]{ CronTaskConfigurationFormField.newBuilder().name(
                        "storageId").value(
                        "storage0").build(),
                                                      CronTaskConfigurationFormField.newBuilder().name(
                                                              "repositoryId").value(
                                                              "").build() }));

        given().contentType(MediaType.APPLICATION_JSON_VALUE)
               .accept(MediaType.APPLICATION_JSON_VALUE)
               .body(cronTaskConfigurationForm)
               .when()
               .put(getContextBaseUrl())
               .peek()
               .then()
               .statusCode(HttpStatus.BAD_REQUEST.value())
               .expect(MockMvcResultMatchers.jsonPath("errors[0].messages").value(hasItem(stringContainsInOrder(
                       Arrays.asList(
                               new String[]{ "Required field value [repositoryId] not provided" })))))
               .expect(MockMvcResultMatchers.jsonPath("errors[0].name").value(equalTo("fields[1].value")));
    }

    @Test
    public void repositoryIdShouldBeAutocompletablyValidated()
    {
        CronTaskConfigurationForm cronTaskConfigurationForm = new CronTaskConfigurationForm();
        cronTaskConfigurationForm.setJobClass(RebuildMavenMetadataCronJob.class.getCanonicalName());
        cronTaskConfigurationForm.setCronExpression("0 11 11 11 11 ? 2100");
        cronTaskConfigurationForm.setFields(
                Arrays.asList(new CronTaskConfigurationFormField[]{ CronTaskConfigurationFormField.newBuilder().name(
                        "repositoryId").value("mummy").build() }));

        given().contentType(MediaType.APPLICATION_JSON_VALUE)
               .accept(MediaType.APPLICATION_JSON_VALUE)
               .body(cronTaskConfigurationForm)
               .when()
               .put(getContextBaseUrl())
               .peek()
               .then()
               .statusCode(HttpStatus.BAD_REQUEST.value())
               .expect(MockMvcResultMatchers.jsonPath("errors[0].messages").value(hasItem(stringContainsInOrder(
                       Arrays.asList(
                               new String[]{ "Invalid value [mummy] provided. Possible values do not contain this value." })))))
               .expect(MockMvcResultMatchers.jsonPath("errors[0].name").value(equalTo("fields[0].value")));
    }

    @Test
    public void storageIdIdShouldBeAutocompletablyValidated()
            throws JsonProcessingException
    {
        CronTaskConfigurationForm cronTaskConfigurationForm = new CronTaskConfigurationForm();
        cronTaskConfigurationForm.setJobClass(RebuildMavenMetadataCronJob.class.getCanonicalName());
        cronTaskConfigurationForm.setCronExpression("0 11 11 11 11 ? 2100");
        cronTaskConfigurationForm.setFields(
                Arrays.asList(new CronTaskConfigurationFormField[]{ CronTaskConfigurationFormField.newBuilder().name(
                        "storageId").value("mummy").build() }));

        System.out.println(objectMapper.writeValueAsString(cronTaskConfigurationForm));

        given().contentType(MediaType.APPLICATION_JSON_VALUE)
               .accept(MediaType.APPLICATION_JSON_VALUE)
               .body(cronTaskConfigurationForm)
               .when()
               .put(getContextBaseUrl())
               .peek()
               .then()
               .statusCode(HttpStatus.BAD_REQUEST.value())
               .expect(MockMvcResultMatchers.jsonPath("errors[0].messages").value(hasItem(stringContainsInOrder(
                       Arrays.asList(
                               new String[]{ "Invalid value [mummy] provided. Possible values do not contain this value." })))))
               .expect(MockMvcResultMatchers.jsonPath("errors[0].name").value(equalTo("fields[0].value")));
    }

    @Test
    public void shouldValidateIntTypeFields()
    {
        CronTaskConfigurationForm cronTaskConfigurationForm = new CronTaskConfigurationForm();
        cronTaskConfigurationForm.setJobClass(
                CleanupExpiredArtifactsFromProxyRepositoriesCronJob.class.getCanonicalName());
        cronTaskConfigurationForm.setCronExpression("0 11 11 11 11 ? 2100");
        cronTaskConfigurationForm.setFields(
                Arrays.asList(new CronTaskConfigurationFormField[]{ CronTaskConfigurationFormField.newBuilder().name(
                        "lastAccessedTimeInDays").value("piecdziesiat").build() }));

        given().contentType(MediaType.APPLICATION_JSON_VALUE)
               .accept(MediaType.APPLICATION_JSON_VALUE)
               .body(cronTaskConfigurationForm)
               .when()
               .put(getContextBaseUrl())
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
        CronTaskConfigurationForm cronTaskConfigurationForm = new CronTaskConfigurationForm();
        cronTaskConfigurationForm.setJobClass(RegenerateChecksumCronJob.class.getCanonicalName());
        cronTaskConfigurationForm.setCronExpression("0 11 11 11 11 ? 2100");
        cronTaskConfigurationForm.setFields(
                Arrays.asList(new CronTaskConfigurationFormField[]{ CronTaskConfigurationFormField.newBuilder().name(
                        "forceRegeneration").value("prawda").build() }));

        given().contentType(MediaType.APPLICATION_JSON_VALUE)
               .accept(MediaType.APPLICATION_JSON_VALUE)
               .body(cronTaskConfigurationForm)
               .when()
               .put(getContextBaseUrl())
               .peek()
               .then()
               .statusCode(HttpStatus.BAD_REQUEST.value())
               .expect(MockMvcResultMatchers.jsonPath("errors[0].messages").value(hasItem(stringContainsInOrder(
                       Arrays.asList(
                               new String[]{ "Invalid value [prawda] type provided. [boolean] was expected." })))))
               .expect(MockMvcResultMatchers.jsonPath("errors[0].name").value(equalTo("fields[0].value")));
    }

    @Test
    public void afterSuccessfulCronTaskCreationHeadersShouldContainCronUuid()
    {
        CronTaskConfigurationForm cronTaskConfigurationForm = new CronTaskConfigurationForm();
        cronTaskConfigurationForm.setJobClass(RegenerateChecksumCronJob.class.getCanonicalName());
        cronTaskConfigurationForm.setCronExpression("0 11 11 11 11 ? 2100");
        cronTaskConfigurationForm.setFields(
                Arrays.asList(new CronTaskConfigurationFormField[]{ CronTaskConfigurationFormField.newBuilder().name(
                        "forceRegeneration").value("false").build() }));

        Headers headers = given().contentType(MediaType.APPLICATION_JSON_VALUE)
                                 .accept(MediaType.APPLICATION_JSON_VALUE)
                                 .body(cronTaskConfigurationForm)
                                 .when()
                                 .put(getContextBaseUrl())
                                 .peek()
                                 .then()
                                 .statusCode(HttpStatus.OK.value())
                                 .and()
                                 .extract()
                                 .headers();

        String cronUuid = headers.getValue(HEADER_NAME_CRON_TASK_ID);
        assertThat(cronUuid).isNotEmpty();

        deleteConfig(cronUuid);
    }

    @Test
    public void completeCronTaskCrudTest()
    {
        CronTaskConfigurationForm cronTaskConfigurationForm = new CronTaskConfigurationForm();
        cronTaskConfigurationForm.setJobClass(RegenerateChecksumCronJob.class.getCanonicalName());
        cronTaskConfigurationForm.setCronExpression("0 11 11 11 11 ? 2100");
        cronTaskConfigurationForm.setFields(
                Arrays.asList(new CronTaskConfigurationFormField[]{ CronTaskConfigurationFormField.newBuilder().name(
                        "forceRegeneration").value("false").build() }));

        Headers headers = given().contentType(MediaType.APPLICATION_JSON_VALUE)
                                 .accept(MediaType.APPLICATION_JSON_VALUE)
                                 .body(cronTaskConfigurationForm)
                                 .when()
                                 .put(getContextBaseUrl())
                                 .peek()
                                 .then()
                                 .statusCode(HttpStatus.OK.value())
                                 .and()
                                 .extract()
                                 .headers();

        String cronUuid = headers.getValue(HEADER_NAME_CRON_TASK_ID);
        assertThat(cronUuid).isNotEmpty();

        CronTaskConfigurationDto config = given().contentType(MediaType.APPLICATION_JSON_VALUE)
                                                 .accept(MediaType.APPLICATION_JSON_VALUE)
                                                 .when()
                                                 .get(getContextBaseUrl() + "/" + cronUuid)
                                                 .peek()
                                                 .then()
                                                 .statusCode(HttpStatus.OK.value())
                                                 .and()
                                                 .extract()
                                                 .as(CronTaskConfigurationDto.class);

        assertThat(config).isNotNull();
        assertThat(config.getCronExpression()).isEqualTo("0 11 11 11 11 ? 2100");
        assertThat(config.getProperty("forceRegeneration")).isEqualTo("false");

        cronTaskConfigurationForm.setCronExpression("0 11 11 11 11 ? 2099");
        cronTaskConfigurationForm.setFields(
                Arrays.asList(new CronTaskConfigurationFormField[]{ CronTaskConfigurationFormField.newBuilder().name(
                        "forceRegeneration").value("true").build() }));

        given().contentType(MediaType.APPLICATION_JSON_VALUE)
               .accept(MediaType.APPLICATION_JSON_VALUE)
               .body(cronTaskConfigurationForm)
               .when()
               .put(getContextBaseUrl() + "/" + cronUuid)
               .peek()
               .then()
               .statusCode(HttpStatus.OK.value())
               .and()
               .extract();

        config = given().contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .when()
                        .get(getContextBaseUrl())
                        .peek()
                        .then()
                        .statusCode(HttpStatus.OK.value())
                        .and()
                        .extract()
                        .as(CronTaskConfigurationDto.class);

        assertThat(config).isNotNull();
        assertThat(config.getCronExpression()).isEqualTo("0 11 11 11 11 ? 2099");
        assertThat(config.getProperty("forceRegeneration")).isEqualTo("true");

        deleteConfig(cronUuid);
    }

    /* TODO FIXME
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
    */

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

}
