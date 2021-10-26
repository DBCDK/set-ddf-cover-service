#!groovy

pipeline {
    agent { label "devel10" }
    tools {
        // refers to the name set in manage jenkins -> global tool configuration
        maven "Maven 3"
    }
    triggers {
        // This project uses the docker.dbc.dk/payara5-micro container
        upstream('/Docker-payara5-bump-trigger')
    }
    options {
        timestamps()
    }
    stages {
        stage("clear workspace") {
            steps {
                deleteDir()
                checkout scm
            }
        }
        stage("build") {
            steps {
                script {
                    def status = sh returnStatus: true, script:  """
                        rm -rf \$WORKSPACE/.repo
                        mvn -B -Dmaven.repo.local=\$WORKSPACE/.repo dependency:resolve dependency:resolve-plugins >/dev/null
                        mvn -B -Dmaven.repo.local=\$WORKSPACE/.repo clean
                    """

                    // We want code-coverage and pmd/spotbugs even if unittests fails
                    status += sh returnStatus: true, script:  """
                        mvn -B -Dmaven.repo.local=\$WORKSPACE/.repo verify pmd:pmd pmd:cpd spotbugs:spotbugs javadoc:aggregate
                    """

                    junit testResults: '**/target/*-reports/*.xml'

                    def java = scanForIssues tool: [$class: 'Java']
                    def javadoc = scanForIssues tool: [$class: 'JavaDoc']
                    publishIssues issues:[java, javadoc], unstableTotalAll:1

                    def pmd = scanForIssues tool: [$class: 'Pmd']
                    publishIssues issues:[pmd], unstableTotalAll:1

                    def spotbugs = scanForIssues tool: [$class: 'SpotBugs']
                    publishIssues issues:[spotbugs], unstableTotalAll:1

                    if (status != 0) {
                        currentBuild.result = Result.FAILURE
                    } else {
                        docker.image("docker-io.dbc.dk/set-ddf-cover-service:${env.BRANCH_NAME}-${env.BUILD_NUMBER}").push()
                    }
                }
            }
        }
    }
}
