/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.state.services;

import javax.persistence.EntityManager;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.dao.AlertsDAO;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.stack.OsFamily;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.verification.Times;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

/**
 * Tests the {@link CachedAlertFlushService}.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(CachedAlertFlushService.class)
public class CachedAlertFlushServiceTest extends EasyMockSupport {

  private Injector m_injector;

  @Before
  public void before() {
    // create an injector which will inject the mocks
    m_injector = Guice.createInjector(new MockModule());
    Configuration configuration = m_injector.getInstance(Configuration.class);
    EasyMock.reset(configuration);
  }

  /**
   * Tests that the flush service does not run if caching is disabled.
   *
   * @throws Exception
   */
  @Test
  public void testServiceIsDisabled() throws Exception {
    Configuration configuration = m_injector.getInstance(Configuration.class);
    EasyMock.expect(configuration.isAlertCacheEnabled()).andReturn(Boolean.FALSE).atLeastOnce();

    // mock the stopAsync method
    CachedAlertFlushService service = PowerMockito.spy(new CachedAlertFlushService());
    PowerMockito.doReturn(null).when(service).stopAsync();

    replayAll();

    m_injector.injectMembers(service);
    service.startUp();

    PowerMockito.verifyPrivate(service).invoke("stopAsync");
    verifyAll();
  }

  /**
   * Tests that the flush service runs and flushes if it's enabled.
   *
   * @throws Exception
   */
  @Test
  public void testServiceIsEnabled() throws Exception {
    Configuration configuration = m_injector.getInstance(Configuration.class);
    EasyMock.expect(configuration.isAlertCacheEnabled()).andReturn(Boolean.TRUE).atLeastOnce();

    AlertsDAO alertsDAO = m_injector.getInstance(AlertsDAO.class);
    alertsDAO.flushCachedEntitiesToJPA();
    EasyMock.expectLastCall().once();

    // mock the stopAsync method
    CachedAlertFlushService service = PowerMockito.spy(new CachedAlertFlushService());
    PowerMockito.doReturn(null).when(service).stopAsync();

    replayAll();

    m_injector.injectMembers(service);
    service.startUp();
    service.runOneIteration();

    PowerMockito.verifyPrivate(service, new Times(0)).invoke("stopAsync");
    verifyAll();
  }


  /**
   *
   */
  private class MockModule implements Module {
    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(Binder binder) {
      Cluster cluster = EasyMock.createNiceMock(Cluster.class);

      // required for since the configuration is being mocked
      Configuration configuration = createNiceMock(Configuration.class);
      EasyMock.expect(configuration.getAlertEventPublisherPoolSize()).andReturn(2).anyTimes();

      EasyMock.replay(configuration);

      binder.bind(Configuration.class).toInstance(configuration);
      binder.bind(Clusters.class).toInstance(createNiceMock(Clusters.class));
      binder.bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
      binder.bind(DBAccessor.class).toInstance(createNiceMock(DBAccessor.class));
      binder.bind(Cluster.class).toInstance(cluster);
      binder.bind(AlertDefinitionDAO.class).toInstance(createNiceMock(AlertDefinitionDAO.class));
      binder.bind(AlertsDAO.class).toInstance(createNiceMock(AlertsDAO.class));
      binder.bind(EntityManager.class).toInstance(createNiceMock(EntityManager.class));
    }
  }
}