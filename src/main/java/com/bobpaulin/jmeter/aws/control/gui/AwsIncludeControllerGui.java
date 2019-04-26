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
package com.bobpaulin.jmeter.aws.control.gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;

import org.apache.jmeter.control.gui.AbstractControllerGui;
import org.apache.jmeter.gui.util.MenuFactory;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jorphan.gui.layout.VerticalLayout;

import com.bobpaulin.jmeter.aws.control.AwsIncludeController;

public class AwsIncludeControllerGui extends AbstractControllerGui
{

    private static final long serialVersionUID = 240L;
    
    private JLabel awsBucketNameLabel;
    
    private JTextField awsBucketNameText;
    
    private JLabel awsKeyNameLabel;
    
    private JTextField awsKeyNameText;

    /**
     * Initializes the gui panel for the ModuleController instance.
     */
    public AwsIncludeControllerGui() {
        init();
    }
    
    /**
     * 
     * Not used
     */
    @Override
    public String getLabelResource() {
    	return "";
    }

    @Override
    public String getStaticLabel() {
        return "AWS Include Controller";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(TestElement el) {
        super.configure(el);
        AwsIncludeController controller = (AwsIncludeController) el;
        this.awsBucketNameText.setText(controller.getAwsBucketName());
        this.awsKeyNameText.setText(controller.getAwsKeyName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TestElement createTestElement() {
        AwsIncludeController mc = new AwsIncludeController();
        configureTestElement(mc);
        return mc;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void modifyTestElement(TestElement element) {
        configureTestElement(element);
        AwsIncludeController controller = (AwsIncludeController)element;
        controller.setAwsBucketName(this.awsBucketNameText.getText());
        controller.setAwsKeyName(this.awsKeyNameText.getText());
    }

    /**
     * Implements JMeterGUIComponent.clearGui
     */
    @Override
    public void clearGui() {
        super.clearGui();
        awsBucketNameText.setText("");
        awsKeyNameText.setText("");
    }

    @Override
    public JPopupMenu createPopupMenu() {
        JPopupMenu menu = new JPopupMenu();
        MenuFactory.addEditMenu(menu, true);
        MenuFactory.addFileMenu(menu);
        return menu;
    }

    private void init() { // WARNING: called from ctor so must not be overridden (i.e. must be private or final)
        setLayout(new VerticalLayout(5, VerticalLayout.BOTH, VerticalLayout.TOP));
        setBorder(makeBorder());
        add(makeTitlePanel());
        
        JPanel mainPanel = new JPanel(new GridLayout(2, 1));
        mainPanel.add(createBucketNamePanel());
        mainPanel.add(createKeyNamePanel());
        add(mainPanel, BorderLayout.CENTER);
    }
    
    private JPanel createBucketNamePanel() {
        JPanel bucketNamePanel = new JPanel(new BorderLayout(5, 0));

        // Condition LABEL
        awsBucketNameLabel = new JLabel("S3 Bucket Name");
        bucketNamePanel.add(awsBucketNameLabel, BorderLayout.WEST);
        awsBucketNameText = new JTextField(10);
        
        awsBucketNameLabel.setLabelFor(awsBucketNameText);
        bucketNamePanel.add(awsBucketNameText, BorderLayout.CENTER);
        return bucketNamePanel;
    }
    
    private JPanel createKeyNamePanel() {
        JPanel keyNamePanel = new JPanel(new BorderLayout(5, 0));

        // Condition LABEL
        awsKeyNameLabel = new JLabel("S3 Key Name");
        keyNamePanel.add(awsKeyNameLabel, BorderLayout.WEST);
        awsKeyNameText = new JTextField(10);
        
        awsKeyNameLabel.setLabelFor(awsKeyNameText);
        keyNamePanel.add(awsKeyNameText, BorderLayout.CENTER);
        return keyNamePanel;
    }
}