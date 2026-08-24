package com.jairam.aicodeassistant.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring's scheduled-task support for the monolith's background maintenance jobs (e.g. the
 * refresh-token cleanup and the stale-index-job reaper). Scheduling is enabled only in the {@code
 * app} deployable, not in test slices, so unit contexts do not spin up scheduler threads.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class SchedulingConfig {}
