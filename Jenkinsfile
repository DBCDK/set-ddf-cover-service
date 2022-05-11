#!groovy

def workerNode = "devel10"

pipeline {
    agent { label workerNode }
    tools {
        // refers to the name set in manage jenkins -> global tool configuration
        jdk 'jdk11'
        maven "Maven 3"
    }
    triggers {
        // This project uses the docker.dbc.dk/payara5-micro container
        upstream('/Docker-payara5-bump-trigger')
    }
    environment {
        GITLAB_PRIVATE_TOKEN = credentials("metascrum-gitlab-api-token")
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

                    //def spotbugs = scanForIssues tool: [$class: 'SpotBugs']
                    //publishIssues issues:[spotbugs], unstableTotalAll:1

                    if (status != 0) {
                        currentBuild.result = Result.FAILURE
                    } else {
                        docker.image("docker-io.dbc.dk/set-ddf-cover-service:${env.BRANCH_NAME}-${env.BUILD_NUMBER}").push()
                    }
                }
            }
        }

        stage("Bump deploy version") {
            agent {
                docker {
                    label workerNode
                    image "docker.dbc.dk/build-env:latest"
                    alwaysPull true
                }
            }

            when {
                expression {
                    currentBuild.result == null || currentBuild.result == 'SUCCESS'
                }
            }
            steps {
                script {
                    if (env.BRANCH_NAME == 'main') {
                        sh """
                            set-new-version set-ddf-cover-service.yml ${env.GITLAB_PRIVATE_TOKEN} metascrum/set-ddf-cover-service-secrets ${env.BRANCH_NAME}-${env.BUILD_NUMBER} -b fbstest
                        """
                    }
                }
            }
        }
    }
}
