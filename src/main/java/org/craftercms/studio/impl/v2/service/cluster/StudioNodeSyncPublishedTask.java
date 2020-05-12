/*
 * Copyright (C) 2007-2020 Crafter Software Corporation. All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as published by
 * the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.craftercms.studio.impl.v2.service.cluster;

import static org.craftercms.studio.api.v1.constant.GitRepositories.PUBLISHED;
import static org.craftercms.studio.api.v2.utils.StudioConfiguration.REPO_SYNC_DB_COMMIT_MESSAGE_NO_PROCESSING;
import static org.craftercms.studio.impl.v1.repository.git.GitContentRepositoryConstants.GIT_ROOT;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import org.craftercms.commons.crypto.CryptoException;
import org.craftercms.studio.api.v1.constant.GitRepositories;
import org.craftercms.studio.api.v1.exception.ServiceLayerException;
import org.craftercms.studio.api.v1.log.Logger;
import org.craftercms.studio.api.v1.log.LoggerFactory;
import org.craftercms.studio.api.v2.dal.ClusterMember;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.FetchResult;

public class StudioNodeSyncPublishedTask extends StudioNodeSyncBaseTask {

    private static final Logger logger = LoggerFactory.getLogger(StudioNodeSyncPublishedTask.class);

    protected static final Map<String, ReentrantLock> singleWorkerLockMap = new HashMap<String, ReentrantLock>();

    protected boolean lockSiteInternal(String siteId) {
        ReentrantLock singleWorkerLock = singleWorkerLockMap.get(siteId);
        if (singleWorkerLock == null) {
            singleWorkerLock = new ReentrantLock();
            singleWorkerLockMap.put(siteId, singleWorkerLock);
        }
        return singleWorkerLock.tryLock();
    }
    
    protected void unlockSiteInternal(String siteId) {
        ReentrantLock singleWorkerLock = singleWorkerLockMap.get(siteId);
        if (singleWorkerLock != null) {
            singleWorkerLock.unlock();
        }
    }

    // We always sync published since we're not tracking it yet
    // TODO: SJ: Be smarter about this, find a way to sync only when required and only from repos ahead of us
    // TODO: SJ: Consider adding a column to the cluster table indicating where every node is and sync from latest
    protected boolean isSyncRequiredInternal(String siteId, String siteDatabaseLastCommitId) {
        return true;
    }

    protected boolean createSiteInternal(String siteId, String siteUuid, String searchEngine) {
        return false;
    }

    // Published never clones, instead, it lets the sanbox process handle that. Return true.
    protected boolean cloneSiteInternal(String siteId, GitRepositories repoType) {
        return true;
    }

    protected void updateContentInternal(String siteId, String lastCommitId) throws IOException, CryptoException,
                                                                                    ServiceLayerException {
        logger.debug("Update published repo for site " + siteId);
        Path siteSandboxPath = buildRepoPath(PUBLISHED).resolve(GIT_ROOT);
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repo = builder
                .setGitDir(siteSandboxPath.toFile())
                .readEnvironment()
                .findGitDir()
                .build();

        try (Git git = new Git(repo)) {
            Set<String> environments = getAllPublishingEnvironments(siteId);
            logger.debug("Update published repo from all active cluster members");
            for (ClusterMember remoteNode : clusterNodes) {
                try {
                    logger.debug("Fetch from cluster member " + remoteNode.getLocalAddress());
                    final Path tempKey = Files.createTempFile(UUID.randomUUID().toString(), ".tmp");
                    FetchCommand fetch = git.fetch().setRemote(remoteNode.getGitRemoteName());
                    fetch = configureAuthenticationForCommand(remoteNode, fetch, tempKey);
                    fetch.call();
                    Files.delete(tempKey);
                } catch (GitAPIException e) {
                    logger.error("Error while fetching published repo for site " + siteId + " from remote " +
                            remoteNode.getGitRemoteName());
                    logger.error(e.getMessage());
                }
                logger.debug("Update all environments for site " + siteId + " from cluster member " +
                        remoteNode.getLocalAddress());
                for (String branch : environments) {
                    try {
                        updatePublishedBranch(git, remoteNode, branch);
                    } catch (GitAPIException e) {
                        logger.error("Error while updating published repo for site " + siteId + " from remote " +
                                remoteNode.getGitRemoteName() + " environment " + branch);
                        logger.error(e.getMessage());
                    }
                }
            }
        }

    }

    private void updatePublishedBranch(Git git, ClusterMember remoteNode, String branch) throws CryptoException,
            GitAPIException, IOException, ServiceLayerException {
        logger.debug("Update published environment " + branch + " from " + remoteNode.getLocalAddress() +
                " for site " + siteId);
        final Path tempKey = Files.createTempFile(UUID.randomUUID().toString(), ".tmp");

        Repository repo = git.getRepository();
        Ref ref = repo.exactRef(Constants.R_HEADS + branch);
        boolean createBranch = (ref == null);

        logger.debug("Checkout " + branch);
        CheckoutCommand checkoutCommand = git.checkout()
                .setName(branch)
                .setCreateBranch(createBranch);
        if (createBranch) {
            checkoutCommand.setStartPoint(remoteNode.getGitRemoteName() + "/" + branch);
        }
        checkoutCommand.call();

        FetchCommand fetchCommand = git.fetch().setRemote(remoteNode.getGitRemoteName());
        fetchCommand = configureAuthenticationForCommand(remoteNode, fetchCommand, tempKey);
        FetchResult fetchResult = fetchCommand.call();

        ObjectId commitToMerge;
        Ref r;
        if (fetchResult != null) {
            r = fetchResult.getAdvertisedRef(branch);
            if (r == null) {
                r = fetchResult.getAdvertisedRef(Constants.R_HEADS + branch);
            }
            if (r != null) {
                commitToMerge = r.getObjectId();

                MergeCommand mergeCommand = git.merge();
                mergeCommand.setMessage(studioConfiguration.getProperty(REPO_SYNC_DB_COMMIT_MESSAGE_NO_PROCESSING));
                mergeCommand.setCommit(true);
                mergeCommand.include(remoteNode.getGitRemoteName(), commitToMerge);
                mergeCommand.setStrategy(MergeStrategy.THEIRS);
                mergeCommand.call();
            }
        }

        Files.delete(tempKey);
    }

    private Set<String> getAllPublishingEnvironments(String site) {
        Set<String> environments = new HashSet<String>();
        environments.add(servicesConfig.getLiveEnvironment(site));
        if (servicesConfig.isStagingEnvironmentEnabled(site)) {
            environments.add(servicesConfig.getStagingEnvironment(site));
        }
        return environments;
    }
}
