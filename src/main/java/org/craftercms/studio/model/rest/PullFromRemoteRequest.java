/*
 * Copyright (C) 2007-2019 Crafter Software Corporation. All Rights Reserved.
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

package org.craftercms.studio.model.rest;

import org.apache.commons.lang3.StringUtils;

public class PullFromRemoteRequest {

    private String siteId;
    private String remoteName;
    private String remoteBranch;
    private String mergeStrategy = StringUtils.EMPTY;

    public String getSiteId() {
        return siteId;
    }

    public void setSiteId(String siteId) {
        this.siteId = siteId;
    }

    public String getRemoteName() {
        return remoteName;
    }

    public void setRemoteName(String remoteName) {
        this.remoteName = remoteName;
    }

    public String getRemoteBranch() {
        return remoteBranch;
    }

    public void setRemoteBranch(String remoteBranch) {
        this.remoteBranch = remoteBranch;
    }

    public String getMergeStrategy() {
        return StringUtils.isEmpty(mergeStrategy) ? StringUtils.EMPTY : mergeStrategy;
    }

    public void setMergeStrategy(String mergeStrategy) {
        this.mergeStrategy = mergeStrategy;
    }
}
