/* Copyright 2006-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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
package grails.plugin.databasemigration

import java.sql.Connection

import liquibase.Liquibase
import liquibase.database.Database
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.hibernate.FlushMode
import org.hibernate.Session
import org.springframework.orm.hibernate3.SessionFactoryUtils
import org.springframework.orm.hibernate3.SessionHolder
import org.springframework.transaction.support.TransactionSynchronizationManager

/**
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
class MigrationUtils {

	private MigrationUtils() {
		// static only
	}

	/** Set at startup. */
	static GrailsApplication application

	/** Set from _Events.groovy in eventPackageAppEnd. */
	static String scriptName

	static Database getDatabase(Connection connection, String defaultSchema) {
		def database = DatabaseFactory.instance.findCorrectDatabaseImplementation(
			new JdbcConnection(connection))
		if (defaultSchema) {
			database.defaultSchemaName = defaultSchema
		}
//		database.defaultSchemaName = connection.catalog // TODO
		database
	}

	static Database getDatabase(String defaultSchema = null) {
		def connection = findSessionFactory().currentSession.connection()
		getDatabase connection, defaultSchema
	}

	static Liquibase getLiquibase(Database database) {
		getLiquibase database, getChangelogFileName()
	}

	static Liquibase getLiquibase(Database database, String changelogFileName) {
		def resourceAccessor = application.mainContext.migrationResourceAccessor
		new Liquibase(changelogFileName, resourceAccessor, database)
	}

	static void executeInSession(Closure c) {
		boolean participate = initSession()
		try {
			c()
		}
		finally {
			if (!participate) {
				flushAndClose()
			}
		}
	}

	private static boolean initSession() {
		def sessionFactory = findSessionFactory()
		if (TransactionSynchronizationManager.hasResource(sessionFactory)) {
			return true
		}

		Session session = SessionFactoryUtils.getSession(sessionFactory, true)
		session.flushMode = FlushMode.AUTO
		TransactionSynchronizationManager.bindResource sessionFactory, new SessionHolder(session)
		false
	}

	private static void flushAndClose() {
		def sessionFactory = findSessionFactory()
		def session = TransactionSynchronizationManager.unbindResource(sessionFactory).session
		if (!FlushMode.MANUAL == session.flushMode) {
			session.flush()
		}
		SessionFactoryUtils.closeSession session
	}

	private static findSessionFactory() {

		def factoryBean = application.mainContext.getBean('&sessionFactory')
		if (factoryBean.getClass().simpleName == 'DelayedSessionFactoryBean') {
			// get the un-proxied version since at this point it's ok to get a connection;
			// only an issue during plugin tests
			return factoryBean.realSessionFactory
		}

		application.mainContext.sessionFactory
	}

	static boolean canAutoMigrate() {

		// in a war
		if (application.warDeployed) {
			return true
		}

		// in run-app
		if ('RunApp'.equals(scriptName)) {
			return true
		}

		false
	}

	static ConfigObject getConfig() {
		application.config.grails.plugin.databasemigration
	}

	static String getDbDocLocation() {
		getConfig().dbDocLocation ?: 'target/dbdoc'
	}

	static String getChangelogFileName() {
		getConfig().changelogFileName ?: 'changelog.groovy'
	}

	static String getChangelogLocation() {
		getConfig().changelogLocation ?: 'grails-app/conf/migrations'
	}

	static ConfigObject getChangelogProperties() {
		getConfig().changelogProperties ?: [:]
	}
}
