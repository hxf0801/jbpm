/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jbpm.services.task.commands;

import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.drools.core.xml.jaxb.util.JaxbMapAdapter;
import org.kie.internal.command.Context;

@XmlRootElement(name = "update-process-extra-byInstance-command")
@XmlAccessorType(XmlAccessType.NONE)
public class UpdateProcessExtraByInstanceCommand extends UserGroupCallbackTaskCommand<Void> {

    private static final long serialVersionUID = 1L;

    @XmlJavaTypeAdapter(JaxbMapAdapter.class)
    @XmlElement
    protected Map<String, Object> data;

    public UpdateProcessExtraByInstanceCommand() {}

    public UpdateProcessExtraByInstanceCommand(long processInstanceId, Map<String, Object> data) {
        this.taskId = processInstanceId;
        this.data = data;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public Void execute(Context cntxt) {
        TaskContext context = (TaskContext) cntxt;
        context.getTaskQueryService().updateProcessExtraByInstanceId(taskId, data);
        return null;

    }
}
