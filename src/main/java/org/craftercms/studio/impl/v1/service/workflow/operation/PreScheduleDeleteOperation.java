/*
 * Crafter Studio Web-content authoring solution
 * Copyright (C) 2007-2016 Crafter Software Corporation.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.craftercms.studio.impl.v1.service.workflow.operation;


import org.craftercms.studio.api.v1.exception.ServiceException;
import org.craftercms.studio.api.v1.service.workflow.WorkflowService;
import org.craftercms.studio.api.v1.service.workflow.context.GoLiveContext;

import java.util.Date;
import java.util.Set;

public class PreScheduleDeleteOperation extends SubmitLifeCycleOperation {
    protected Date date;


    public PreScheduleDeleteOperation(WorkflowService dmWorkflowService, Set<String> uris, Date date, GoLiveContext context, Set<String> rescheduledUris) {
        super(dmWorkflowService, uris,context,rescheduledUris);
        this.date = date;
    }

    @Override
    public Object execute() throws ServiceException {
        workflowService.preScheduleDelete(uris, date, context, rescheduledUris);
        return null;
    }
}
