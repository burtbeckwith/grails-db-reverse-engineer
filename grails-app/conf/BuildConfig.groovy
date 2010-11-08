grails.project.class.dir = 'target/classes'
grails.project.test.class.dir = 'target/test-classes'
grails.project.test.reports.dir = 'target/test-reports'
grails.project.docs.output.dir = 'docs' // docs are checked into gh-pages branch

grails.project.dependency.resolution = {
	inherits 'global'

	log 'warn'

	repositories {
		grailsPlugins()
		grailsHome()
		grailsCentral()

		mavenRepo 'http://repository.jboss.com/maven2/'
	}

	dependencies {
		compile 'org.hibernate:hibernate-tools:3.2.4.GA'
	}
}
