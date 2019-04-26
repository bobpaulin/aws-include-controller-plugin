/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.bobpaulin.jmeter.aws.control;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;

import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.control.GenericController;
import org.apache.jmeter.control.IncludeController;
import org.apache.jmeter.control.ReplaceableController;
import org.apache.jmeter.control.TestFragmentController;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.save.SaveService;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestPlan;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.HashTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;

public class AwsIncludeController extends GenericController implements ReplaceableController {
    private static final Logger log = LoggerFactory.getLogger(IncludeController.class);

    private static final long serialVersionUID = 241L;
    
    private static final String AWS_BUCKET_NAME = "AwsIncludeController.awsbucketname";
    
    private static final String AWS_KEY_NAME = "AwsIncludeController.awskeyname";

    private static  final String PREFIX =
        JMeterUtils.getPropDefault(
                "includecontroller.prefix", //$NON-NLS-1$
                ""); //$NON-NLS-1$

    private HashTree subtree = null;
    private TestElement sub = null;

    /**
     * No-arg constructor
     *
     * @see java.lang.Object#Object()
     */
    public AwsIncludeController() {
        super();
    }

    @Override
    public Object clone() {
        // TODO - fix so that this is only called once per test, instead of at every clone
        // Perhaps save previous filename, and only load if it has changed?
        this.resolveReplacementSubTree(null);
        AwsIncludeController clone = (AwsIncludeController) super.clone();
        clone.setAwsBucketName(this.getAwsBucketName());
        clone.setAwsKeyName(this.getAwsKeyName());
        if (this.subtree != null) {
            if (this.subtree.size() == 1) {
                for (Object o : this.subtree.keySet()) {
                    this.sub = (TestElement) o;
                }
            }
            clone.subtree = (HashTree)this.subtree.clone();
            clone.sub = this.sub==null ? null : (TestElement) this.sub.clone();
        }
        return clone;
    }
    
    public void setAwsBucketName(String bucketName) {
    	this.setProperty(AWS_BUCKET_NAME, bucketName);
    }
    
    public String getAwsBucketName() {
    	return this.getPropertyAsString(AWS_BUCKET_NAME);
    }
    
    public void setAwsKeyName(String keyName) {
    	this.setProperty(AWS_KEY_NAME, keyName);
    }
    
    public String getAwsKeyName() {
    	return this.getPropertyAsString(AWS_KEY_NAME);
    }

    /**
     * The way ReplaceableController works is clone is called first,
     * followed by replace(HashTree) and finally getReplacement().
     */
    @Override
    public HashTree getReplacementSubTree() {
        return subtree;
    }

    public TestElement getReplacementElement() {
        return sub;
    }

    @Override
    public void resolveReplacementSubTree(JMeterTreeNode context) {
        this.subtree = this.loadIncludedElements();
    }

    /**
     * load the included elements using SaveService
     *
     * @return tree with loaded elements
     */
    protected HashTree loadIncludedElements() {
        // only try to load the JMX test plan if there is one from S3
    	AmazonS3 s3client = AmazonS3ClientBuilder.defaultClient();
        final String awsBucketName = getAwsBucketName();
        final String awsKeyName = getAwsKeyName();
        HashTree tree = null;
        if (StringUtils.isNotBlank(awsKeyName) && StringUtils.isNotBlank(awsBucketName)) {
            String fileName=PREFIX+awsBucketName + File.separator + awsKeyName;
            try {
            	GetObjectRequest getObjectRequest = new GetObjectRequest(awsBucketName, awsKeyName);
            	File destinationFile = new File(fileName.trim());
            	s3client.getObject(getObjectRequest, destinationFile);
                
                final String absolutePath = destinationFile.getAbsolutePath();
                log.info("loadIncludedElements -- try to load included module: {}", absolutePath);
                if(!destinationFile.exists() && !destinationFile.isAbsolute()){
                        log.error("AWS Include Controller '{}' can't load '{}' - see log for details", this.getName(),
                                fileName);
                        throw new IOException("loadIncludedElements -failed for: " + absolutePath +
                                " and " + destinationFile.getAbsolutePath());
                }
                
                tree = SaveService.loadTree(destinationFile);
                // filter the tree for a TestFragment.
                tree = getProperBranch(tree);
                removeDisabledItems(tree);
                return tree;
            } catch (NoClassDefFoundError ex) // Allow for missing optional jars
            {
                String msg = "Including file \""+ fileName 
                            + "\" failed for Include Controller \""+ this.getName()
                            +"\", missing jar file";
                log.warn(msg, ex);
                JMeterUtils.reportErrorToUser(msg+" - see log for details");
            } catch (FileNotFoundException ex) {
                String msg = "File \""+ fileName 
                        + "\" not found for Include Controller \""+ this.getName()+"\"";
                JMeterUtils.reportErrorToUser(msg+" - see log for details");
                log.warn(msg, ex);
            } catch (Exception ex) {
                String msg = "Including file \"" + fileName 
                            + "\" failed for Include Controller \"" + this.getName()
                            +"\", unexpected error";
                JMeterUtils.reportErrorToUser(msg+" - see log for details");
                log.warn(msg, ex);
            }
        }
        return tree;
    }

    /**
     * Extract from tree (included test plan) all Test Elements located in a Test Fragment
     * @param tree HashTree included Test Plan
     * @return HashTree Subset within Test Fragment or Empty HashTree
     */
    private HashTree getProperBranch(HashTree tree) {
        for (Object o : new LinkedList<>(tree.list())) {
            TestElement item = (TestElement) o;

            //if we found a TestPlan, then we are on our way to the TestFragment
            if (item instanceof TestPlan)
            {
                return getProperBranch(tree.getTree(item));
            }

            if (item instanceof TestFragmentController)
            {
                return tree.getTree(item);
            }
        }
        log.warn("No Test Fragment was found in included Test Plan, returning empty HashTree");
        return new HashTree();
    }


    private void removeDisabledItems(HashTree tree) {
        for (Object o : new LinkedList<>(tree.list())) {
            TestElement item = (TestElement) o;
            if (!item.isEnabled()) {
                tree.remove(item);
            } else {
                removeDisabledItems(tree.getTree(item));// Recursive call
            }
        }
    }

}