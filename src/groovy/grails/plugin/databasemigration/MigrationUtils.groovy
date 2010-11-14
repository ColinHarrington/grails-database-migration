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
import liquibase.diff.Diff

import org.codehaus.groovy.grails.commons.ApplicationHolder as AH

/**
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
class MigrationUtils {

	private MigrationUtils() {
		// static only
	}

	static Database getDatabase(Connection connection) {
		DatabaseFactory.instance.findCorrectDatabaseImplementation new JdbcConnection(connection)
	}

	static Liquibase getLiquibase(Database database) {
		def resourceAccessor = AH.application.mainContext.migrationResourceAccessor
		String changelogXmlName = getConfig().changelogXmlName ?: 'changelog.xml'
		new Liquibase(changelogXmlName, resourceAccessor, database)
	}

	static ConfigObject getConfig() {
		AH.application.config.grails.plugin.databasemigration
	}

	static String getDbDocLocation() {
		getConfig().dbDocLocation ?: 'target/dbdoc'
	}
}
