/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.ml.action;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.HandledTransportAction;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.core.ml.action.PutCalendarAction;
import org.elasticsearch.xpack.core.ml.action.UpdateCalendarJobAction;
import org.elasticsearch.xpack.ml.job.JobManager;
import org.elasticsearch.xpack.ml.job.persistence.JobResultsProvider;

import java.util.Set;

public class TransportUpdateCalendarJobAction extends HandledTransportAction<UpdateCalendarJobAction.Request, PutCalendarAction.Response> {

    private final JobResultsProvider jobResultsProvider;
    private final JobManager jobManager;

    @Inject
    public TransportUpdateCalendarJobAction(Settings settings, ThreadPool threadPool,
                                            TransportService transportService, ActionFilters actionFilters,
                                            IndexNameExpressionResolver indexNameExpressionResolver,
                                            JobResultsProvider jobResultsProvider, JobManager jobManager) {
        super(settings, UpdateCalendarJobAction.NAME, threadPool, transportService, actionFilters,
                indexNameExpressionResolver, UpdateCalendarJobAction.Request::new);
        this.jobResultsProvider = jobResultsProvider;
        this.jobManager = jobManager;
    }

    @Override
    protected void doExecute(UpdateCalendarJobAction.Request request, ActionListener<PutCalendarAction.Response> listener) {

        Set<String> jobIdsToAdd = Strings.tokenizeByCommaToSet(request.getJobIdsToAddExpression());
        Set<String> jobIdsToRemove = Strings.tokenizeByCommaToSet(request.getJobIdsToRemoveExpression());

        jobResultsProvider.updateCalendar(request.getCalendarId(), jobIdsToAdd, jobIdsToRemove,
                c -> {
                    jobManager.updateProcessOnCalendarChanged(c.getJobIds());
                    listener.onResponse(new PutCalendarAction.Response(c));
                }, listener::onFailure);
    }
}
