package org.carlspring.strongbox.cron.jobs;

import org.carlspring.strongbox.cron.jobs.fields.CronJobField;

import java.util.Set;

import org.springframework.util.Assert;

/**
 * @author Przemyslaw Fusik
 */
public class CronJobDefinition
{

    private String id;

    private String name;

    private String description;

    private Set<CronJobField> fields;

    public String getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public String getDescription()
    {
        return description;
    }

    public Set<CronJobField> getFields()
    {
        return fields;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        CronJobDefinition that = (CronJobDefinition) o;

        return id.equals(that.id);
    }

    @Override
    public int hashCode()
    {
        return id.hashCode();
    }

    private CronJobDefinition(Builder builder)
    {
        Assert.notNull(builder.id, "id should not be null");
        id = builder.id;
        name = builder.name;
        description = builder.description;
        fields = builder.fields;
    }

    public static Builder newBuilder()
    {
        return new Builder();
    }


    public static final class Builder
    {

        private String id;
        private String name;
        private String description;
        private Set<CronJobField> fields;

        private Builder()
        {
        }

        public Builder id(String val)
        {
            id = val;
            return this;
        }

        public Builder name(String val)
        {
            name = val;
            return this;
        }

        public Builder description(String val)
        {
            description = val;
            return this;
        }

        public Builder fields(Set<CronJobField> val)
        {
            fields = val;
            return this;
        }

        public CronJobDefinition build()
        {
            return new CronJobDefinition(this);
        }
    }
}
