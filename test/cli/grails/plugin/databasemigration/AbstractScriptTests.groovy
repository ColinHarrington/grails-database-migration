/* Copyright 2010-2012 SpringSource.
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
package grails.plugin.databasemigration

import grails.test.AbstractCliTestCase
import groovy.sql.Sql

import org.h2.tools.Server

/**
 * Abstract base class for script tests.
 *
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
abstract class AbstractScriptTests extends AbstractCliTestCase {

	protected static final String TEST_CHANGELOG = 'changelog.cli.test.groovy'
	protected static final String SECONDARY_TEST_CHANGELOG = 'changelog.cli.secondary-test.groovy'
	protected static final String CHANGELOG_DIR = 'target/changelogs'

	protected static final String URL = 'jdbc:h2:tcp://localhost/./target/testdb/testdb'
	protected static final String SECONDARY_URL = 'jdbc:h2:tcp://localhost/./target/testdb/testdb-secondary'

	private Server server

	protected File file

	protected void setUp() {
		super.setUp()

		new File('target/testdb').deleteDir()
		new File(CHANGELOG_DIR).deleteDir()
		new File(CHANGELOG_DIR).mkdirs()

		server = Server.createTcpServer().start()
		assertEquals 9092, server.port

		executeUpdate AbstractScriptTests.URL, '''
			create table thing (
				id bigint generated by default as identity (start with 1),
				version bigint not null,
				name varchar(255) not null,
				primary key (id)
			)'''

        executeUpdate SECONDARY_URL, '''
			create table secondary_thing (
				id bigint generated by default as identity (start with 1),
				version bigint not null,
				name varchar(255) not null,
				primary key (id)
			)'''


	}

	protected void tearDown() {
		super.tearDown()
		server?.stop()
	}

	protected void executeAndCheck(List<String> command, boolean shouldSucceed = true) {
		execute command
		int exitCode = waitForProcess()
		if (shouldSucceed) {
			if (exitCode != 0) {
				println output
			}
			assertEquals 0, exitCode
		}
		else {
			if (exitCode == 0) {
				println output
			}
			assertFalse 0 == exitCode
		}
		verifyHeader()
	}

	protected void executeAndCheck(String command) {
		executeAndCheck([command])
	}

	protected void initFile(boolean groovy) {
		String name = 'changelog_' + System.currentTimeMillis() + (groovy ? '.groovy' : '.xml')
		file = new File(CHANGELOG_DIR, name)
		file.deleteOnExit()
		assertFalse file.exists()
	}

	protected Sql newSql(String url = URL) { Sql.newInstance(url, 'sa', '', 'org.h2.Driver') }

	protected void executeUpdate(String url, String sql, List values = null) {
		if (values) {
			newSql(url).executeUpdate sql, values
		}
		else {
			newSql(url).executeUpdate sql
		}
	}

	protected void executeInsert(String sql, List values) {
		newSql().executeInsert sql, values
	}

	protected List<String> findAllTableNames(String url = URL) {
		def tableNames = []
		newSql(url).eachRow('show tables') { tableNames << it.TABLE_NAME.toLowerCase() }
		tableNames
	}

	protected void assertTableCount(int count, String url = URL) {
		assertEquals 'checking table count', count, findAllTableNames(url).size()
	}

	protected void generateChangelog() {
		executeAndCheck(['dbm-generate-changelog', TEST_CHANGELOG])
	}

    protected void generateSecondaryChagelog() {
        executeAndCheck(['dbm-generate-changelog', SECONDARY_TEST_CHANGELOG, '--dataSource=secondary'])
    }

	protected void copyTestChangelog(String name = 'test.changelog', String target = TEST_CHANGELOG) {
		def file = new File(CHANGELOG_DIR, target)
		file.deleteOnExit()
		file.withWriter {
			it.write getClass().getResourceAsStream(name).text
		}
	}
}
