package com.oneandone.go.plugin.maven.config;

import com.oneandone.go.plugin.maven.message.PackageMaterialProperties;
import com.oneandone.go.plugin.maven.message.ValidationError;
import com.oneandone.go.plugin.maven.message.ValidationResultMessage;
import com.oneandone.go.plugin.maven.util.RepositoryURL;
import com.thoughtworks.go.plugin.api.logging.Logger;
import lombok.Getter;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.TimeZone;

/** Representation of a maven repository configuration. */
public class MavenRepoConfig {

    /** The logging instance for this class. */
    private static final Logger LOGGER = Logger.getLoggerFor(MavenRepoConfig.class);

    /** The specified properties. */
    private final PackageMaterialProperties repoConfig;

    /** The repository URL. */
    private final String repositoryURL;

    /**
     * The authentication username.
     *
     * @return the authentication username
     */
    @Getter private final String username;

    /**
     * The authentication password.
     *
     * @return the authentication password
     */
    @Getter private final String password;

    /**
     * The HTTP proxy.
     *
     * @return the HTTP proxy
     */
    @Getter private final String proxy;

    /** The time zone or {@code null}. */
    private final String timeZone;

    /**
     * Constructs the repository configuration by the specified properties.
     *
     * @param repoConfig the properties
     */
    public MavenRepoConfig(final PackageMaterialProperties repoConfig) {
        this.repoConfig = repoConfig;

        this.repositoryURL = repoConfig.getValue(ConfigurationProperties.REPOSITORY_CONFIGURATION_KEY_REPO_URL).orNull();
        this.username = repoConfig.getValue(ConfigurationProperties.REPOSITORY_CONFIGURATION_KEY_USERNAME).orNull();
        this.password = repoConfig.getValue(ConfigurationProperties.REPOSITORY_CONFIGURATION_KEY_PASSWORD).orNull();
        this.proxy = repoConfig.getValue(ConfigurationProperties.REPOSITORY_CONFIGURATION_KEY_PROXY).orNull();
        this.timeZone = repoConfig.getValue(ConfigurationProperties.REPOSITORY_CONFIGURATION_TIME_ZONE).orNull();
    }

    /**
     * Returns the repository URL.
     *
     * @return the repository URL
     */
    public RepositoryURL getRepoUrl() {
        final RepositoryURL repoUrl = new RepositoryURL(repositoryURL, username, password);
        if (!repoUrl.isHttp()) {
            throw new RuntimeException("Only http/https urls are supported");
        }
        return repoUrl;
    }

    /**
     * Returns the specified time zone from the repository configuration or the default time zone if none specified.
     *
     * @return the time zone
     */
    public TimeZone getTimeZone() {
        if (timeZone == null) {
            return TimeZone.getDefault();
        }
        return TimeZone.getTimeZone(timeZone);
    }

    /** @return {@link RepositoryURL#getURLWithBasicAuth()} */
    public String getRepoUrlAsStringWithBasicAuth() {
        return this.getRepoUrl().getURLWithBasicAuth();
    }

    /** @return {@link RepositoryURL#getURL()} ()} */
    public String getRepoUrlAsString() {
        return this.getRepoUrl().getURL();
    }

    /**
     * Returns {@code true} if the repository URL is missing (is {@code null} or empty), otherwise {@code false}.
     *
     * @return {@code true} if the repository URL is missing (is {@code null} or empty), otherwise {@code false}
     */
    public boolean isRepoUrlMissing() {
        return repositoryURL == null || repositoryURL.trim().isEmpty();
    }

    /**
     * Validates {@code this} configuration and returns the validation result.
     *
     * @return the validation result
     */
    public ValidationResultMessage validate() {
        final ValidationResultMessage validationResult = new ValidationResultMessage();
        if (isRepoUrlMissing()) {
            final String message = "Repository url not specified";
            LOGGER.error(message);
            validationResult.addError(new ValidationError(ConfigurationProperties.REPOSITORY_CONFIGURATION_KEY_REPO_URL, message));
            return validationResult;
        }

        try {
            if (!this.getRepoUrl().isHttp()) {
                validationResult.addError(new ValidationError(ConfigurationProperties.REPOSITORY_CONFIGURATION_KEY_REPO_URL, "Invalid URL: Only http is supported."));
            }

            final URL repoUrl = new URL(repositoryURL);
            if (repoUrl.getUserInfo() != null) {
                validationResult.addError(new ValidationError(ConfigurationProperties.REPOSITORY_CONFIGURATION_KEY_REPO_URL, "User info should not be provided as part of the URL. Please provide credentials using USERNAME and PASSWORD configuration keys."));
            }
        } catch (final MalformedURLException e) {
            LOGGER.error(e.getMessage());
            validationResult.addError(new ValidationError(ConfigurationProperties.REPOSITORY_CONFIGURATION_KEY_REPO_URL, "Malformed URL specified: " + e.getMessage()));
        }

        ConfigurationProperties.detectInvalidKeys(repoConfig, validationResult,
                ConfigurationProperties.REPOSITORY_CONFIGURATION_KEY_REPO_URL,
                ConfigurationProperties.REPOSITORY_CONFIGURATION_KEY_USERNAME,
                ConfigurationProperties.REPOSITORY_CONFIGURATION_KEY_PASSWORD,
                ConfigurationProperties.REPOSITORY_CONFIGURATION_KEY_PROXY,
                ConfigurationProperties.REPOSITORY_CONFIGURATION_TIME_ZONE
        );
        return validationResult;
    }
}
