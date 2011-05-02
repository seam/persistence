/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.seam.persistence;

/**
 * provides a means of configuring the default flush mode
 * <p/>
 * TODO: This needs more thought, especially with regard to how it works in with
 * {@link PersistenceContexts}
 *
 * @author Stuart Douglas
 */
public interface FlushModeManager {
    /**
     * @return the default flush mode for all seam managed persistence contexts
     */
    public FlushModeType getFlushModeType();

    public void setFlushModeType(FlushModeType flushModeType);
}
