/*
 * Kimios - Document Management System Software
 * Copyright (C) 2008-2015  DevLib'
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * aong with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.kimios.kernel.index.solr;

import org.kimios.kernel.dms.*;
import org.kimios.kernel.dms.model.*;
import org.kimios.kernel.events.model.EventContext;
import org.kimios.kernel.events.GenericEventHandler;
import org.kimios.api.events.annotations.DmsEvent;
import org.kimios.api.events.annotations.DmsEventName;
import org.kimios.api.events.annotations.DmsEventOccur;
import org.kimios.kernel.filetransfer.model.DataTransfer;
import org.kimios.kernel.index.AbstractIndexManager;
import org.kimios.kernel.index.SolrIndexManager;
import org.kimios.kernel.index.TransactionHelper;
import org.kimios.kernel.jobs.JobImpl;
import org.kimios.kernel.jobs.ThreadManager;
import org.kimios.kernel.jobs.model.TaskInfo;
import org.kimios.kernel.security.model.DMEntityACL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

public class SolrIndexer extends GenericEventHandler
{
    private static Logger log = LoggerFactory.getLogger(SolrIndexer.class);

    private AbstractIndexManager indexManager;

    public AbstractIndexManager getIndexManager()
    {
        return indexManager;
    }

    public void setIndexManager(AbstractIndexManager indexManager)
    {
        this.indexManager = indexManager;
        if(log.isDebugEnabled()){
            log.debug("Index Manager Instance : {}", indexManager);
        }
    }

    @DmsEvent(eventName = { DmsEventName.DOCUMENT_UPDATE }, when = DmsEventOccur.AFTER)
    public void documentUpdate(Object[] obj, Object retour, EventContext ctx) throws Exception
    {
        log.debug("Indexing document update: " + (Long) obj[1]);
        Document doc = FactoryInstantiator.getInstance()
                .getDocumentFactory().getDocument((Long) obj[1]);
        try {
            indexManager.indexDocument(doc);
        } catch (Exception e) {
            log.error(" index action Exception on Document " + doc.getUid(), e);
        }
    }

    @DmsEvent(eventName = { DmsEventName.DOCUMENT_COPY }, when = DmsEventOccur.AFTER)
    public void documentCopy(Object[] obj, Object retour, EventContext ctx) throws Exception
    {
        log.debug("indexing document on copy: " + (Long) obj[1]);
        Document doc = (Document)EventContext.getParameters().get("document");
        try {
            indexManager.indexDocument(doc);
        } catch (Exception e) {
            log.error(" index action Exception on Document " + doc.getUid(), e);
        }
    }

    @DmsEvent(eventName = { DmsEventName.DOCUMENT_VERSION_UPDATE }, when = DmsEventOccur.AFTER)
    public void documentVersionUpdate(Object[] obj, Object retour, EventContext ctx) throws Exception
    {
        log.debug("indexing version update: " + (Long) obj[1]);
        Document doc = FactoryInstantiator.getInstance()
                .getDocumentFactory().getDocument((Long) obj[1]);
        try {
            indexManager.indexDocument(doc);
        } catch (Exception e) {
            log.error(" index action Exception on Document " + doc.getUid(), e);
        }
    }

    @DmsEvent(eventName = { DmsEventName.FILE_UPLOAD }, when = DmsEventOccur.AFTER)
    public void documentVersionUpdateUpload(Object[] obj, Object retour, EventContext ctx) throws Exception
    {
        DMEntity doc = ctx.getEntity();
        if(doc == null || !(doc instanceof Document)){
            /*
                Check inside parameters
             */
            doc = (Document)EventContext.getParameters().get("document");
            if(log.isDebugEnabled()){
                log.debug("doc from event param " + doc);
            }

            if(retour instanceof Long){
                //load from id
                doc = FactoryInstantiator.getInstance().getDocumentFactory().getDocument((Long)retour);
            } else if(retour instanceof DataTransfer){
                doc = FactoryInstantiator.getInstance()
                        .getDocumentVersionFactory().getDocumentVersion(((DataTransfer) retour).getDocumentVersionUid())
                        .getDocument();
            }
        }
        if(doc == null){
            log.error("on FILE_UPLOAD, no document has been found for indexing. Method return was {}", retour);
        }
        if(log.isDebugEnabled()){
            log.info("give document {} to {} on FILE_UPLOAD", doc, indexManager);
        }
        try {
            indexManager.indexDocument(doc);
        } catch (Exception e) {
            log.error(" index action Exception on Document " + doc, e);
        }
    }

    @DmsEvent(eventName = { DmsEventName.FOLDER_DELETE }, when = DmsEventOccur.AFTER)
    public void folderDelete(Object[] obj, Object retour, EventContext ctx) throws Exception
    {
        Folder fold = (Folder) EventContext.getParameters().get("removed");
        try {
            indexManager.deletePath(fold.getPath());
            //remove folder from index
            indexManager.deleteDocument(fold);
        } catch (Exception e) {
            log.error(" index removing action Exception on Document on folder remove " + fold.getPath(), e);
        }
    }

    @DmsEvent(eventName = { DmsEventName.WORKSPACE_DELETE }, when = DmsEventOccur.AFTER)
    public void workspaceDelete(Object[] obj, Object retour, EventContext ctx) throws Exception
    {
        Workspace wk = (Workspace) EventContext.getParameters().get("removed");
        try {
            indexManager.deletePath(wk.getPath());
        } catch (Exception e) {
            log.error(" index removing action Exception on Document on Workspace remove " + wk.getPath(), e);
        }
    }

    @DmsEvent(eventName = { DmsEventName.DOCUMENT_DELETE }, when = DmsEventOccur.AFTER)
    public void documentDelete(Object[] obj, Object retour, EventContext ctx) throws Exception
    {
        Document doc = new Document();
        doc.setUid((Long) obj[1]);
        try {
            indexManager.deleteDocument(doc);
        } catch (Exception e) {
            log.error(" index removing action Exception on Document " + doc.getUid(), e);
        }
    }

    @DmsEvent(eventName = { DmsEventName.ENTITY_ACL_UPDATE }, when = DmsEventOccur.AFTER)
    public void updateAcl(Object[] obj, Object retour, EventContext ctx)
    {
        try {
            log.debug("starting Launching index acl update process " + Thread.currentThread().getName());
            List<DMEntityACL> acls = (List<DMEntityACL>) EventContext.getParameters().get("acls");
            if (acls == null && retour instanceof TaskInfo) {
                TaskInfo info = (TaskInfo)retour;
                acls = (List<DMEntityACL>) info.getResults().get("acls");
            }
            if (acls == null || acls.size() == 0) {
                log.debug("No datas for acl update " + Thread.currentThread().getName());
                return;
            }
            try {
                log.debug("Launching index acl update process " + Thread.currentThread().getId());
                HashMap<Long, List<DMEntityACL>> hash = new HashMap<Long, List<DMEntityACL>>();
                for (DMEntityACL r : acls) {
                    if (r.getDmEntityType() == DMEntityType.DOCUMENT) {
                        if (hash.containsKey(r.getDmEntityUid())) {
                            hash.get(r.getDmEntityUid()).add(r);
                        } else {
                            hash.put(r.getDmEntityUid(), new Vector<DMEntityACL>());
                            hash.get(r.getDmEntityUid()).add(r);
                        }
                    }
                }
                int i = 0;
                for (Long uid : hash.keySet()) {
                    indexManager.updateAcls(uid, hash.get(uid), false);
                    i++;
                    if((i % 200) == 0){
                        indexManager.commit();
                    }
                }
                try{
                    indexManager.commit();
                }catch (Exception e){
                    log.error("Error while commiting index on acl update");
                }
                log.debug("Ending index acl update process " + Thread.currentThread().getId());
            } catch (Throwable ex) {
                log.error("Index acl update failed " + Thread.currentThread().getId(), ex);
            }
        } catch (Exception e) {
            log.error("Starting Index acl update failed", e);
        }
    }

    @DmsEvent(eventName = { DmsEventName.FOLDER_UPDATE }, when = DmsEventOccur.AFTER)
    public void updateFolder(Object[] o, Object retour, EventContext ctx)
    {
        log.debug("handling folder update for index");
        try {

            String oldPath = ctx.getEntity().getPath();
            FactoryInstantiator fc = FactoryInstantiator.getInstance();
            Folder f = fc.getFolderFactory().getFolder((Long) o[1]);
            String newPath = f.getPath();
            indexManager.updatePath(oldPath, newPath);


            Folder f2 = (Folder)ctx.getParameters().get("virtualFolder");
            List<VirtualFolderMetaData> values = (List)ctx.getParameters().get("virtualFolderMetas");
            if(f2 != null && values != null && values.size() > 0 && indexManager instanceof SolrIndexManager){
                log.debug("start virtual folder index !!!!!");
                ((SolrIndexManager)indexManager).indexFolder(f, values);
            }
        } catch (Exception e) {
            log.error("Exception during index update : ", e);
        }
    }

    /* Handle virtual folder indexing */
    @DmsEvent(eventName = { DmsEventName.FOLDER_CREATE }, when = DmsEventOccur.AFTER)
    public void virtualFolderAdd(Object[] o, Object retour, EventContext ctx)
    {
        log.debug("handling virtual folder add for index");
        try {
            Folder f = (Folder)ctx.getParameters().get("virtualFolder");
            List<VirtualFolderMetaData> values = (List)ctx.getParameters().get("virtualFolderMetas");
            if(f != null && values != null && values.size() > 0 && indexManager instanceof SolrIndexManager){
                log.debug("start virtual folder index !!!!!");
                ((SolrIndexManager)indexManager).indexFolder(f, values);
            }
        } catch (Exception e) {
            log.error("Exception during index update : ", e);
        }
    }


    @DmsEvent(eventName = { DmsEventName.WORKSPACE_UPDATE }, when = DmsEventOccur.AFTER)
    public void updateWorkspace(Object[] o, Object retour, EventContext ctx)
    {
        log.debug("handling workspace update for index");
        try {
            Workspace w = FactoryInstantiator.getInstance().getWorkspaceFactory().getWorkspace((Long) o[1]);
            String oldName = ctx.getEntity().getName();
            if (!w.getName().equals(oldName)) {
                String newPath = w.getPath();
                String oldPath = "/" + oldName;
                indexManager.updatePath(oldPath, newPath);
            }
        } catch (Exception e) {
            log.error("Exception during index update : ", e);
        }
    }

    @DmsEvent(eventName = { DmsEventName.DOCUMENT_CHECKOUT }, when = DmsEventOccur.AFTER)
    public void documentCheckout(Object[] obj, Object retour, EventContext ctx) throws Exception
    {
        log.debug("Indexing document update: " + (Long) obj[1]);
        Document doc = FactoryInstantiator.getInstance()
            .getDocumentFactory().getDocument((Long) obj[1]);
        try {
            indexManager.indexDocument(doc);
        } catch (Exception e) {
            log.error(" index action Exception on Document " + doc.getUid(), e);
        }
    }

    @DmsEvent(eventName = { DmsEventName.DOCUMENT_CHECKIN }, when = DmsEventOccur.AFTER)
    public void documentCheckin(Object[] obj, Object retour, EventContext ctx) throws Exception
    {
        log.debug("Indexing document update: " + (Long) obj[1]);
        Document doc = FactoryInstantiator.getInstance()
            .getDocumentFactory().getDocument((Long) obj[1]);
        try {
            indexManager.indexDocument(doc);
        } catch (Exception e) {
            log.error(" index action Exception on Document " + doc.getUid(), e);
        }
    }

    @DmsEvent( eventName = {DmsEventName.WORKFLOW_STATUS_REQUEST_CREATE}, when = DmsEventOccur.AFTER)
    public void documentWorkflowCreate(Object[] obj, Object retour, EventContext ctx) throws Exception
    {
        log.debug("Indexing document update: " + (Long) obj[1]);
        Document doc = FactoryInstantiator.getInstance()
            .getDocumentFactory().getDocument((Long) obj[1]);
        try {
            indexManager.indexDocument(doc);
        } catch (Exception e) {
            log.error(" index action Exception on Document " + doc.getUid(), e);
        }
    }

    @DmsEvent( eventName = {DmsEventName.WORKFLOW_STATUS_REQUEST_ACCEPT}, when = DmsEventOccur.AFTER)
    public void documentWorkflowUpdate(Object[] obj, Object retour, EventContext ctx) throws Exception
    {
        log.debug("Indexing document update: " + (Long) obj[1]);
        Document doc = FactoryInstantiator.getInstance()
            .getDocumentFactory().getDocument((Long) obj[1]);
        try {
            indexManager.indexDocument(doc);
        } catch (Exception e) {
            log.error(" index action Exception on Document " + doc.getUid(), e);
        }
    }

    @DmsEvent( eventName = {DmsEventName.WORKFLOW_CANCEL}, when = DmsEventOccur.AFTER)
    public void documentWorkflowCancel(Object[] obj, Object retour, EventContext ctx) throws Exception
    {
        log.debug("Indexing document update: " + (Long) obj[1]);
        Document doc = FactoryInstantiator.getInstance()
            .getDocumentFactory().getDocument((Long) obj[1]);
        try {
            indexManager.indexDocument(doc);
        } catch (Exception e) {
            log.error(" index action Exception on Document " + doc.getUid(), e);
        }
    }

    @DmsEvent( eventName = {DmsEventName.EXTENSION_ENTITY_ATTRIBUTE_SET}, when = DmsEventOccur.AFTER)
    public void setEntityAttribute(Object[] obj, Object retour, EventContext ctx) throws Exception
    {
        log.debug("Indexing on entity attribute set: " + (Long) obj[1]);
        try {
            if(ctx.getEntity() != null && ctx.getEntity().getType() == DMEntityType.DOCUMENT){
                indexManager.indexDocument(ctx.getEntity());
            }

        } catch (Exception e) {
            log.error(" index action Exception on entity attribute set");
        }
    }

    @DmsEvent( eventName = {DmsEventName.DOCUMENT_TRASH}, when = DmsEventOccur.AFTER)
    public void trashDocument(Object[] obj, Object retour, EventContext ctx) throws Exception
    {
        log.debug("Removing entities from index on trash: " + (Long) obj[1]);
        try {
            if(ctx.getEntity() == null)
                ctx.setEntity((DMEntity)EventContext.getParameters().get("document"));
            if(ctx.getEntity() != null && ctx.getEntity().getType() == DMEntityType.DOCUMENT){
                indexManager.deleteDocument(ctx.getEntity());
            } else if(ctx.getEntity() != null && (ctx.getEntity().getType() == DMEntityType.WORKSPACE ||
                ctx.getEntity().getType() == DMEntityType.FOLDER)) {
                log.info("removing from index with path " + ctx.getEntity().getPath());
                indexManager.deletePath(ctx.getEntity().getPath());
            }

        } catch (Exception e) {
            log.error(" index action Exception on entity attribute set");
        }
    }

    @DmsEvent( eventName = {DmsEventName.DOCUMENT_UNTRASH}, when = DmsEventOccur.AFTER)
    public void untrashDocument(Object[] obj, Object retour, final EventContext ctx) throws Exception
    {
        log.debug("Removing document from trash, so index it back " + (Long) obj[1]);
        try {
            if(ctx.getEntity() == null)
                ctx.setEntity((DMEntity)EventContext.getParameters().get("document"));
            if(ctx.getEntity() != null && ctx.getEntity().getType() == DMEntityType.DOCUMENT){
                indexManager.indexDocument(ctx.getEntity());
            } else if(ctx.getEntity() != null && (ctx.getEntity().getType() == DMEntityType.WORKSPACE ||
                    ctx.getEntity().getType() == DMEntityType.FOLDER)) {
                //reindex doc ref with thread !

                // load entities befores !!!

                final String path = ctx.getEntity().getPath().replaceAll("__TRASHED_ENTITY__", "");
                log.info("look for path : " + path);
                final List<DMEntity> items = FactoryInstantiator.getInstance()
                        .getDmEntityFactory()
                        .getEntities(path);
                String taskId = UUID.randomUUID().toString();
                ThreadManager.getInstance()
                        .startJob(ctx.getSession(), new JobImpl(taskId) {
                            @Override
                            public Object execute() throws Exception {
                                log.info("reindexing doc after untrash!");
                                TransactionHelper thHelper = new TransactionHelper();
                                Object o = thHelper.startNew(null);
                                log.info("found doc to reindex: {}", items);
                                for(DMEntity item: items){

                                    log.info("reindexed path {}", item.getPath());
                                    item.setPath(item.getPath().replaceAll("__TRASHED_ENTITY__", ""));
                                    if(item instanceof Document)
                                        indexManager.indexDocument(item);
                                }
                                log.info("reindexing doc after untrash! done");
                                thHelper.rollback(o);
                                return null;

                            }
                        });

            }

        } catch (Exception e) {
            log.error(" index action Exception on entity attribute set");
        }
    }
}
