/* Copyright 2006-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.plugin.databasemigration.test

import java.lang.reflect.Field
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

import org.codehaus.groovy.grails.orm.hibernate.ConfigurableLocalSessionFactoryBean
import org.hibernate.SessionFactory
import org.springframework.util.ReflectionUtils

/**
 * Only used for testing; see http://burtbeckwith.com/blog/?p=312
 *
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
class DelayedSessionFactoryBean extends ConfigurableLocalSessionFactoryBean {

	private boolean _initialized
	private SessionFactory _realSessionFactory

	@Override
	void afterPropertiesSet() {
		// do nothing for now, lazy init on first access
	}

	@Override
	SessionFactory getObject() {
		Proxy.newProxyInstance SessionFactory.classLoader, [SessionFactory] as Class[], new InvocationHandler() {
			Object invoke(proxy, Method method, Object[] args) {
				initialize()
				method.invoke _realSessionFactory, args
			}
		}
	}

	SessionFactory getRealSessionFactory() {
		initialize()
		_realSessionFactory
	}

	private synchronized void initialize() {
		if (_initialized) {
			return
		}

		_realSessionFactory = wrapSessionFactoryIfNecessary(buildSessionFactory())

		Field field = ReflectionUtils.findField(getClass(), 'sessionFactory')
		field.accessible = true
		field.set this, _realSessionFactory

		afterSessionFactoryCreation()

		_initialized = true
	}
}
