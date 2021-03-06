/*
 * Copyright 1999-2015 dangdang.com.
 * <p>
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
 * </p>
 */

package com.dangdang.ddframe.job.lite.integrate;

import com.dangdang.ddframe.job.api.ElasticJob;
import com.dangdang.ddframe.job.api.type.dataflow.api.DataflowJobConfiguration;
import com.dangdang.ddframe.job.lite.api.config.LiteJobConfiguration;
import com.google.common.base.Optional;
import org.junit.After;
import org.junit.Before;

public abstract class AbstractBaseStdJobAutoInitTest extends AbstractBaseStdJobTest {
    
    protected AbstractBaseStdJobAutoInitTest(final Class<? extends ElasticJob> elasticJobClass, final Optional<DataflowJobConfiguration.DataflowType> dataflowType) {
        super(elasticJobClass, false, dataflowType);
    }
    
    protected void setLiteJobConfig(final LiteJobConfiguration liteJobConfig) {
    }
    
    @Before
    public void autoJobInit() {
        setLiteJobConfig(getLiteJobConfig());
        initJob();
        assertRegCenterCommonInfoWithEnabled();
    }
    
    @After
    public void assertAfterJobRun() {
        assertRegCenterListenerInfo();
    }
}
